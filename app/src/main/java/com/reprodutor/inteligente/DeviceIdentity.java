package com.reprodutor.inteligente;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.util.UUID;

public final class DeviceIdentity {
    private static final String PREFS = "identidade_licenca";
    private static final String INSTALLATION_ID = "installation_id";
    private static final String KEY_ALIAS = "reprodutor_device_signing_key";

    private DeviceIdentity() { }

    public static String getInstallationId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String id = prefs.getString(INSTALLATION_ID, null);
        if (id == null || id.trim().isEmpty()) {
            id = UUID.randomUUID().toString().toUpperCase(java.util.Locale.US);
            prefs.edit().putString(INSTALLATION_ID, id).apply();
        }
        return id;
    }

    public static synchronized void ensureSigningKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) return;

        KeyPairGenerator generator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                .setAlgorithmParameterSpec(new java.security.spec.ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build();
        generator.initialize(spec);
        generator.generateKeyPair();
    }
}
