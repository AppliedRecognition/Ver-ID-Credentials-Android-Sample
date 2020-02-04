package com.appliedrec.credentials.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;

import androidx.core.util.Pair;
import androidx.preference.PreferenceManager;

import com.google.gson.stream.JsonReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

class IntellicheckBarcodeVerifier {

    private final String url;
    private final SharedPreferences sharedPreferences;
    private final String password;
    private final String appId;

    public IntellicheckBarcodeVerifier(Context context, String password) {
        this.url = BuildConfig.INTELLICHECK_URL;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.password = password;
        this.appId = context.getApplicationContext().getPackageName();
    }

    private String getDeviceId() {
        String key = "device_id";
        String deviceId = sharedPreferences.getString(key, UUID.randomUUID().toString());
        if (!sharedPreferences.contains(key)) {
            sharedPreferences.edit().putString(key, deviceId).apply();
        }
        return deviceId;
    }

    public Completable testPassword() {
        return Completable.create(emitter -> {
            try {
                Uri uri = Uri.parse(url);
                List<String> pathSegments = uri.getPathSegments();
                Uri.Builder builder = new Uri.Builder()
                        .scheme(uri.getScheme())
                        .authority(uri.getAuthority());
                for (int i=0; i<pathSegments.size()-1; i++) {
                    builder.appendPath(pathSegments.get(i));
                }
                builder.appendPath("check-password");
                uri = builder.build();
                HttpsURLConnection connection = (HttpsURLConnection) new URL(uri.toString()).openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setDoOutput(true);
                connection.setDoInput(true);
                byte[] body = Objects.requireNonNull(new Uri.Builder()
                        .appendQueryParameter("device_id", getDeviceId())
                        .appendQueryParameter("app_id", appId)
                        .appendQueryParameter("password", password).build().getQuery()).getBytes(StandardCharsets.UTF_8);
                OutputStream outputStream = connection.getOutputStream();
                ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
                int read;
                byte[] buffer = new byte[512];
                while ((read = inputStream.read(buffer, 0, buffer.length)) > 0) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.close();
                if (connection.getResponseCode() == 200) {
                    emitter.onComplete();
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    while ((read = connection.getErrorStream().read(buffer, 0, buffer.length)) > 0) {
                        stringBuilder.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
                    }
                    String response = stringBuilder.toString();
                    if (!response.isEmpty()) {
                        throw new Exception(response);
                    } else {
                        throw new Exception("Unknown error");
                    }
                }
            } catch (Exception e) {
                emitter.onError(e);
            }
        }).subscribeOn(Schedulers.io());
    }

    public Observable<Pair<String,String>> parseBarcode(String barcode) {
        Observable<Pair<String,String>> observable = Observable.create(emitter -> {
            try {
                URL url = new URL(this.url);
                byte[] barcodeData = barcode.getBytes(StandardCharsets.UTF_8);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setDoInput(true);
                connection.setDoOutput(true);
                String body = new Uri.Builder()
                        .appendQueryParameter("device_os", "Android")
                        .appendQueryParameter("device_id", getDeviceId())
                        .appendQueryParameter("device_name", Build.MODEL)
                        .appendQueryParameter("password", password)
                        .appendQueryParameter("app_id", appId)
                        .appendQueryParameter("data", Base64.encodeToString(barcodeData, Base64.NO_WRAP))
                        .build()
                        .getQuery();
                ByteArrayInputStream inputStream = new ByteArrayInputStream(Objects.requireNonNull(body).getBytes(StandardCharsets.UTF_8));
                OutputStream outputStream = connection.getOutputStream();
                int read;
                byte[] buffer = new byte[512];
                while ((read = inputStream.read(buffer, 0, buffer.length)) > 0) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.close();
                if (connection.getResponseCode() >= 400) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    while ((read = connection.getErrorStream().read(buffer, 0, buffer.length)) > 0) {
                        byteArrayOutputStream.write(buffer, 0, read);
                    }
                    String response = new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
                    throw new Exception(response);
                }
                // TODO: Finish this
                JsonReader jsonReader = new JsonReader(new InputStreamReader(connection.getInputStream()));
                jsonReader.setLenient(true);
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    if ("document_t".equals(jsonReader.nextName())) {
                        jsonReader.beginObject();
                        while (jsonReader.hasNext()) {
                            String name = jsonReader.nextName();
                            if ("$".equals(name)) {
                                jsonReader.skipValue();
                            } else if ("testCard".equals(name)) {
                                emitter.onNext(new Pair<>(name, jsonReader.nextBoolean() ? "Yes" : "No"));
                            } else {
                                try {
                                    String value = jsonReader.nextString();
                                    if (value != null && !value.trim().isEmpty()) {
                                        emitter.onNext(new Pair<>(name, value));
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        jsonReader.endObject();
                    }
                }
                jsonReader.endObject();
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
        return observable.subscribeOn(Schedulers.io());
    }
}
