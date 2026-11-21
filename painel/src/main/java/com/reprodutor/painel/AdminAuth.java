package com.reprodutor.painel;

import android.content.Context;
import android.util.Base64;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class AdminAuth {
    private static final String PREF="painel_auth";
    private static final int ITERATIONS=210000;

    static void save(Context c, String password) {
        byte[] salt=new byte[16];
        new SecureRandom().nextBytes(salt);
        PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),salt,ITERATIONS,256);
        try {
            // PBKDF2-HMAC-SHA1 é usado aqui por compatibilidade com Android API 23.
            byte[] hash=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                    .generateSecret(spec).getEncoded();
            c.getSharedPreferences(PREF,0).edit()
                    .putString("algorithm","PBKDF2WithHmacSHA1")
                    .putString("salt",Base64.encodeToString(salt,Base64.NO_WRAP))
                    .putString("hash",Base64.encodeToString(hash,Base64.NO_WRAP))
                    .apply();
        } catch(Exception e) {
            throw new IllegalStateException("Não foi possível proteger a senha administrativa",e);
        } finally {
            spec.clearPassword();
        }
    }

    static boolean configured(Context c) {
        return c.getSharedPreferences(PREF,0).contains("hash");
    }

    static boolean verify(Context c,String password) {
        if(password==null || password.isEmpty()) return false;
        PBEKeySpec spec=null;
        try {
            android.content.SharedPreferences p=c.getSharedPreferences(PREF,0);
            if(!"PBKDF2WithHmacSHA1".equals(p.getString("algorithm",""))) return false;
            byte[] salt=Base64.decode(p.getString("salt",""),Base64.NO_WRAP);
            byte[] expected=Base64.decode(p.getString("hash",""),Base64.NO_WRAP);
            spec=new PBEKeySpec(password.toCharArray(),salt,ITERATIONS,256);
            byte[] actual=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                    .generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(expected,actual);
        } catch(Exception e) {
            return false;
        } finally {
            if(spec!=null) spec.clearPassword();
        }
    }

    private AdminAuth() {}
}
