package com.reprodutor.painel;

import android.content.Context;
import android.util.Base64;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class AdminAuth {
    private static final String PREF="painel_auth";
    static void save(Context c, String password) {
        try {
            byte[] salt=new byte[16]; new SecureRandom().nextBytes(salt);
            PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),salt,210000,256);
            byte[] hash=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            c.getSharedPreferences(PREF,0).edit().putString("salt",Base64.encodeToString(salt,2))
                .putString("hash",Base64.encodeToString(hash,2)).apply();
        } catch(Exception e) { throw new IllegalStateException(e); }
    }
    static boolean configured(Context c) { return c.getSharedPreferences(PREF,0).contains("hash"); }
    static boolean verify(Context c,String password) {
        try {
            var p=c.getSharedPreferences(PREF,0);
            byte[] salt=Base64.decode(p.getString("salt",""),2);
            byte[] expected=Base64.decode(p.getString("hash",""),2);
            PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),salt,210000,256);
            byte[] actual=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(expected,actual);
        } catch(Exception e) { return false; }
    }
    private AdminAuth() {}
}