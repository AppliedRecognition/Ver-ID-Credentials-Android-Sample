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

    void setValueForKey(String value) {
        sharedPreferences.edit().putString(CommonKeys.INTELLICHECK_API_KEY, value).apply();
    }

    String getValueForKey() {
        return sharedPreferences.getString(CommonKeys.INTELLICHECK_API_KEY, null);
    }

    void deleteValueForKey() {
        sharedPreferences.edit().remove(CommonKeys.INTELLICHECK_API_KEY).apply();
    }
}
