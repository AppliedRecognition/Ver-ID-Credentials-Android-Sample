package com.appliedrec.credentials.app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

class SecureStorage {

    private SharedPreferences sharedPreferences;

    static class CommonKeys {

        static final String INTELLICHECK_API_KEY = "IntellicheckApiKey";
    }

    SecureStorage(Context context) throws GeneralSecurityException, IOException {
        sharedPreferences = EncryptedSharedPreferences.create("credentialsAppSecureStore", MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC), context, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }

    void setValueForKey(String value, String key) {
        sharedPreferences.edit().putString(key, value).apply();
    }

    String getValueForKey(String key) {
        return sharedPreferences.getString(key, null);
    }

    void deleteValueForKey(String key) {
        sharedPreferences.edit().remove(key).apply();
    }
}
