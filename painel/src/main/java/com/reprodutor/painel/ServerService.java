package com.reprodutor.painel;

import android.app.*;
import android.content.Intent;
import android.os.IBinder;
import android.os.Build;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.*;
import android.os.Environment;
import android.util.Base64;
import fi.iki.elonen.NanoHTTPD;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;

public class ServerService extends Service {
    private LocalServer server;
    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT>=26) {
            NotificationChannel ch=new NotificationChannel("activation_server","Servidor de ativação",NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
            Notification n=new Notification.Builder(this,"activation_server").setContentTitle("Painel de Ativação")
                .setContentText("Servidor local ativo").setSmallIcon(android.R.drawable.stat_sys_upload_done).build();
            startForeground(42,n);
        }
        try { server=new LocalServer(this); server.start(NanoHTTPD.SOCKET_READ_TIMEOUT,false); }
        catch(Exception e) { stopSelf(); }
    }
    @Override public int onStartCommand(Intent i,int flags,int id){ return START_STICKY; }
    @Override public void onDestroy(){ if(server!=null) server.stop(); super.onDestroy(); }
    @Override public IBinder onBind(Intent i){ return null; }

    static class Db extends SQLiteOpenHelper {
        Db(android.content.Context c){super(c,"activation.db",null,1);}
        public void onCreate(SQLiteDatabase d){
            d.execSQL("CREATE TABLE requests(id INTEGER PRIMARY KEY AUTOINCREMENT, code_hash TEXT UNIQUE, expires INTEGER, status TEXT, created INTEGER, device TEXT, file_name TEXT, file_hash TEXT, analysis TEXT, approved_token TEXT)");
        }
        public void onUpgrade(SQLiteDatabase d,int a,int b){}
    }
    static class LocalServer extends NanoHTTPD {
        final ServerService owner; final Db db; final File uploads;
        LocalServer(ServerService s)throws IOException{
            super("0.0.0.0",8080); owner=s; db=new Db(s);
            uploads=new File(s.getFilesDir(),"receipts"); uploads.mkdirs();
        }
        @Override public Response serve(IHTTPSession session) {
            try {
                String path=session.getUri();
                if(path.equals("/health")) return json(new JSONObject().put("ok",true).put("service","activation-panel"));
                if(path.equals("/api/request") && session.getMethod()==Method.POST) return createRequest(session);
                if(path.equals("/api/upload") && session.getMethod()==Method.POST) return upload(session);
                if(path.equals("/api/admin/pending") && session.getMethod()==Method.GET) {
                    if(!admin(session)) return unauthorized();
                    return pending();
                }
                if(path.equals("/api/admin/approve") && session.getMethod()==Method.POST) {
                    if(!admin(session)) return unauthorized();
                    return approve(session);
                }
                if(path.equals("/api/admin/reject") && session.getMethod()==Method.POST) {
                    if(!admin(session)) return unauthorized();
                    return reject(session);
                }
                return newFixedLengthResponse(Response.Status.NOT_FOUND,"application/json","{\"error\":\"not_found\"}");
            } catch(Exception e) { return newFixedLengthResponse(Response.Status.BAD_REQUEST,"application/json","{\"error\":\"invalid_request\"}"); }
        }
        boolean admin(IHTTPSession s) {
            String pass=s.getHeaders().get("x-admin-password");
            return pass!=null && AdminAuth.verify(owner,pass);
        }
        Response createRequest(IHTTPSession s)throws Exception {
            Map<String,String> body=new HashMap<>(); s.parseBody(body);
            JSONObject j=new JSONObject(body.getOrDefault("postData","{}"));
            String device=j.optString("device","").trim();
            if(device.length()<8 || device.length()>160) return jsonError("device_invalid");
            byte[] raw=new byte[18]; new SecureRandom().nextBytes(raw);
            String code=Base64.encodeToString(raw,Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);
            String hash=sha(code); long now=System.currentTimeMillis()/1000L;
            ContentValues v=new ContentValues(); v.put("code_hash",hash); v.put("expires",now+86400);
            v.put("status","PENDING"); v.put("created",now); v.put("device",device);
            db.getWritableDatabase().insertOrThrow("requests",null,v);
            return json(new JSONObject().put("code",code).put("expires_at",now+86400).put("message","Código provisório: não libera o player."));
        }
        Response upload(IHTTPSession s)throws Exception {
            Map<String,String> files=new HashMap<>(); s.parseBody(files);
            String temp=files.get("receipt");
            if(temp==null) return jsonError("receipt_missing");
            File src=new File(temp); if(!src.isFile() || src.length()>12*1024*1024) return jsonError("file_size_invalid");
            String supplied=s.getParms().get("code"); if(supplied==null) return jsonError("code_missing");
            String hash=shaFile(src); String mime=detect(src);
            if(mime==null) return jsonError("file_type_unsupported");
            SQLiteDatabase d=db.getWritableDatabase();
            Cursor dup=d.rawQuery("SELECT id FROM requests WHERE file_hash=? LIMIT 1",new String[]{hash});
            boolean repeated=dup.moveToFirst(); dup.close();
            Cursor c=d.rawQuery("SELECT id,expires,status FROM requests WHERE code_hash=?",new String[]{sha(supplied)});
            if(!c.moveToFirst()){c.close();return jsonError("code_invalid");}
            long id=c.getLong(0), exp=c.getLong(1); String status=c.getString(2); c.close();
            long now=System.currentTimeMillis()/1000L;
            if(now>exp || !"PENDING".equals(status)) return jsonError("code_expired_or_used");
            File dest=new File(uploads,id+"_"+hash.substring(0,12)+(mime.equals("application/pdf")?".pdf":".img"));
            try(InputStream in=new FileInputStream(src);OutputStream out=new FileOutputStream(dest)){byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);}
            String analysis=repeated?"SUSPEITO: arquivo idêntico já recebido":(mime.equals("application/pdf")?"INCOMPLETO: PDF reconhecido; conteúdo e pagamento precisam de conferência":"INCOMPLETO: imagem reconhecida; pagamento precisa de conferência");
            ContentValues v=new ContentValues();v.put("file_name",dest.getName());v.put("file_hash",hash);v.put("analysis",analysis);
            d.update("requests",v,"id=?",new String[]{String.valueOf(id)});
            return json(new JSONObject().put("received",true).put("analysis",analysis).put("automatic_payment_confirmation",false));
        }
        Response pending()throws Exception {
            JSONArray arr=new JSONArray(); Cursor c=db.getReadableDatabase().rawQuery("SELECT id,created,expires,status,device,file_name,analysis FROM requests ORDER BY id DESC LIMIT 100",null);
            while(c.moveToNext()) arr.put(new JSONObject().put("id",c.getLong(0)).put("created",c.getLong(1)).put("expires",c.getLong(2)).put("status",c.getString(3)).put("device",c.getString(4)).put("file",c.getString(5)).put("analysis",c.getString(6)));
            c.close(); return json(new JSONObject().put("requests",arr));
        }
        Response approve(IHTTPSession s)throws Exception {
            Map<String,String>b=new HashMap<>();s.parseBody(b);JSONObject j=new JSONObject(b.getOrDefault("postData","{}"));
            long id=j.getLong("id"); String token=UUID.randomUUID().toString().replace("-","").toUpperCase(Locale.US);
            ContentValues v=new ContentValues();v.put("status","APPROVED");v.put("approved_token",token);
            int n=db.getWritableDatabase().update("requests",v,"id=? AND status='PENDING' AND file_hash IS NOT NULL AND expires>=?",new String[]{String.valueOf(id),String.valueOf(System.currentTimeMillis()/1000L)});
            if(n!=1)return jsonError("not_approvable");
            return json(new JSONObject().put("approved",true).put("license_token",token).put("duration","indeterminate"));
        }
        Response reject(IHTTPSession s)throws Exception {
            Map<String,String>b=new HashMap<>();s.parseBody(b);JSONObject j=new JSONObject(b.getOrDefault("postData","{}"));
            ContentValues v=new ContentValues();v.put("status","REJECTED");
            int n=db.getWritableDatabase().update("requests",v,"id=? AND status='PENDING'",new String[]{String.valueOf(j.getLong("id"))});
            return json(new JSONObject().put("rejected",n==1));
        }
        String detect(File f)throws Exception {
            byte[] h=new byte[8];try(InputStream in=new FileInputStream(f)){if(in.read(h)<4)return null;}
            if(h[0]=='%'&&h[1]=='P'&&h[2]=='D'&&h[3]=='F')return "application/pdf";
            if((h[0]&255)==0xFF&&(h[1]&255)==0xD8)return "image/jpeg";
            if((h[0]&255)==0x89&&h[1]=='P'&&h[2]=='N'&&h[3]=='G')return "image/png";
            if(h[0]=='R'&&h[1]=='I'&&h[2]=='F'&&h[3]=='F'&&h[8-1]=='E')return "image/webp";
            return null;
        }
        Response unauthorized(){return newFixedLengthResponse(Response.Status.UNAUTHORIZED,"application/json","{\"error\":\"unauthorized\"}");}
        Response jsonError(String e){try{return json(new JSONObject().put("error",e));}catch(Exception x){return unauthorized();}}
        Response json(JSONObject j){return newFixedLengthResponse(Response.Status.OK,"application/json; charset=utf-8",j.toString());}
        String sha(String s)throws Exception{return hex(MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8")));}
        String shaFile(File f)throws Exception{MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[8192];int n;while((n=in.read(b))>0)md.update(b,0,n);}return hex(md.digest());}
        String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format(Locale.US,"%02x",x));return s.toString();}
    }
}