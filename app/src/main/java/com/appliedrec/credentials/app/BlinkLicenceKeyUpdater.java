package com.appliedrec.credentials.app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.microblink.MicroblinkSDK;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

class BlinkLicenceKeyUpdater {

    private final Context context;
    private static final String BLINK_LICENCE_PREF_KEY = "com.appliedrec.BLINK_LICENCE_PREF_KEY";

    BlinkLicenceKeyUpdater(Context context) {
        this.context = context;
    }

    Single<String> getSavedLicenceKey() {
        return Single.<String>create(emitter -> {
            try {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                String licenceKey = preferences.getString(BLINK_LICENCE_PREF_KEY, BuildConfig.BLINK_LICENCE_KEY);
                emitter.onSuccess(licenceKey);
            } catch (Exception e) {
                emitter.onError(e);
            }
        }).subscribeOn(Schedulers.io());
    }

    Completable deleteSavedLicenceKey() {
        return Completable.create(emitter -> {
            try {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                preferences.edit().remove(BLINK_LICENCE_PREF_KEY).apply();
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        }).subscribeOn(Schedulers.io());
    }

    Single<String> getLicenceKeyFromRemote() {
        return Single.<String>create(emitter -> {
            try {
                String packageName = context.getApplicationContext().getPackageName();
                URL url = new URL("https://ver-id.s3.us-east-1.amazonaws.com/ios/microblink_keys/com.appliedrec.credentials.app.txt");
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setUseCaches(false);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "text/plain");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);
                int status = connection.getResponseCode();
                if (status == HttpsURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line = reader.readLine();
                    reader.close();
                    if (line != null) {
                        emitter.onSuccess(line);
                    } else {
                        throw new Exception("Unable to read response");
                    }
                } else {
                    throw new Exception("Received http status " + status);
                }
            } catch (Exception e) {
                emitter.onError(e);
            }
        }).subscribeOn(Schedulers.io());
    }

    Completable setLicenceKey(String licenceKey) {
        return Completable.create(emitter -> {
            try {
                MicroblinkSDK.setLicenseKey(licenceKey, context);
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                preferences.edit().putString(BLINK_LICENCE_PREF_KEY, licenceKey).apply();
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        }).subscribeOn(AndroidSchedulers.mainThread());
    }
}
