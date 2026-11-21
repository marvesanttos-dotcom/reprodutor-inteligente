package com.reprodutor.painel;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.*;

public class MainActivity extends Activity {
    private TextView status, requests;
    private EditText password;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root=new LinearLayout(this); root.setOrientation(1); root.setPadding(24,30,24,24); root.setBackgroundColor(0xff101010);
        TextView title=new TextView(this); title.setText("PAINEL DE ATIVAÇÃO"); title.setTextSize(22); title.setTextColor(0xffffd700); title.setGravity(Gravity.CENTER); root.addView(title);
        TextView note=new TextView(this); note.setText("Servidor no seu celular • Pix conferido manualmente • Licença por tempo indeterminado após aprovação"); note.setTextColor(-1); note.setPadding(0,18,0,12); root.addView(note);
        Button set=new Button(this); set.setText("CRIAR / ALTERAR SENHA ADMINISTRATIVA"); root.addView(set); set.setOnClickListener(v->passwordDialog());
        Button start=new Button(this); start.setText("INICIAR SERVIDOR"); root.addView(start); start.setOnClickListener(v->{Intent i=new Intent(this,ServerService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);status.setText("Servidor iniciado na porta 8080 (se o Android permitir).");});
        password=new EditText(this); password.setHint("Senha administrativa"); password.setSingleLine(true); password.setTextColor(-1); password.setHintTextColor(0xffaaaaaa); root.addView(password);
        Button refresh=new Button(this); refresh.setText("CARREGAR SOLICITAÇÕES"); root.addView(refresh); refresh.setOnClickListener(v->loadPending());
        requests=new TextView(this); requests.setTextColor(-1); requests.setPadding(0,16,0,16); root.addView(requests);
        status=new TextView(this); status.setTextColor(0xffffd700); status.setText("Servidor desligado"); root.addView(status);
        TextView warning=new TextView(this); warning.setTextColor(0xffffcc66); warning.setPadding(0,20,0,0); warning.setText("A pré-análise detecta tipo e repetição do arquivo, mas não confirma o Pix. Confira o crédito no RecargaPay antes de aprovar. Para clientes fora da sua rede, será necessário configurar um túnel HTTPS seguro."); root.addView(warning);
        setContentView(root);
    }
    private void passwordDialog(){
        EditText a=new EditText(this);a.setHint("Nova senha (mínimo 10 caracteres)");
        EditText b=new EditText(this);b.setHint("Repita a senha");
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(24,8,24,8);box.addView(a);box.addView(b);
        new AlertDialog.Builder(this).setTitle("Configurar administrador").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",(d,w)->{
            String x=a.getText().toString(),y=b.getText().toString();
            if(x.length()<10||!x.equals(y)){Toast.makeText(this,"Senhas diferentes ou muito curtas.",Toast.LENGTH_LONG).show();return;}
            AdminAuth.save(this,x);Toast.makeText(this,"Senha salva neste aparelho.",Toast.LENGTH_LONG).show();
        }).show();
    }
    private void loadPending(){
        String p=password.getText().toString();
        io.execute(()->{try{
            HttpURLConnection c=(HttpURLConnection)new URL("http://127.0.0.1:8080/api/admin/pending").openConnection();
            c.setRequestProperty("X-Admin-Password",p);c.setConnectTimeout(4000);c.setReadTimeout(4000);
            String body=read(c.getInputStream());JSONArray arr=new JSONObject(body).getJSONArray("requests");
            runOnUiThread(()->showRequests(arr,p));
        }catch(Exception e){runOnUiThread(()->status.setText("Não foi possível carregar. Inicie o servidor e confira a senha."));}});
    }
    private void showRequests(JSONArray arr,String pass){
        requests.removeAllViews();
        if(arr.length()==0){requests.setText("Nenhuma solicitação.");return;}
        StringBuilder out=new StringBuilder();
        for(int i=0;i<arr.length();i++){JSONObject o=arr.optJSONObject(i);out.append("#").append(o.optLong("id")).append(" • ").append(o.optString("status")).append("\nDispositivo: ").append(o.optString("device")).append("\nArquivo: ").append(o.optString("file","não enviado")).append("\nPré-análise: ").append(o.optString("analysis","aguardando")).append("\n\n");}
        requests.setText(out.toString());
        for(int i=0;i<arr.length();i++){
            JSONObject o=arr.optJSONObject(i);if(!"PENDING".equals(o.optString("status"))||o.optString("file","").isEmpty())continue;
            final long id=o.optLong("id");
            Button approve=new Button(this);approve.setText("APROVAR solicitação #"+id+" (Pix já conferido)");((LinearLayout)requests.getParent()).addView(approve);
            approve.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Confirma que você verificou a entrada de R$ 9,99 no RecargaPay?").setNegativeButton("Cancelar",null).setPositiveButton("Sim, aprovar",(d,w)->decide(id,pass,true)).show());
            Button reject=new Button(this);reject.setText("REJEITAR #"+id);((LinearLayout)requests.getParent()).addView(reject);reject.setOnClickListener(v->decide(id,pass,false));
        }
    }
    private void decide(long id,String pass,boolean approve){
        io.execute(()->{try{HttpURLConnection c=(HttpURLConnection)new URL("http://127.0.0.1:8080/api/admin/"+(approve?"approve":"reject")).openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("X-Admin-Password",pass);try(OutputStream o=c.getOutputStream()){o.write(new JSONObject().put("id",id).toString().getBytes("UTF-8"));}String body=read(c.getInputStream());runOnUiThread(()->{status.setText(body);loadPending();});}catch(Exception e){runOnUiThread(()->status.setText("Falha ao registrar decisão."));}});
    }
    private String read(InputStream in)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] x=new byte[4096];int n;while((n=in.read(x))>0)b.write(x,0,n);return b.toString("UTF-8");}
}