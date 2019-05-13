package com.appliedrec.idcapturesample;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.appliedrec.verid.credentials.IDDocument;
import com.appliedrec.verid.credentials.IDDocumentCoder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Helper class that saves the captured ID card result to shared preferences
 */

public class CardCaptureResultPersistence {

    private static final String ID_DOCUMENT_KEY = "idDocument";
    private static Object lock = new Object();

    public interface LoadCallback {
        void onLoadDocument(IDDocument document);
    }

    public static void saveCapturedDocument(@NonNull final Context context, @Nullable final IDDocument idDocument) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    if (idDocument == null) {
                        getSharedPreferences(context).edit().remove(ID_DOCUMENT_KEY);
                        return;
                    }
                    try {
                        Gson gson = new Gson();
                        String json = gson.toJson(idDocument);
                        getSharedPreferences(context).edit().putString(ID_DOCUMENT_KEY, json).apply();
                    } catch (Exception e) {
                    }
                }
            }
        });
    }

    public static void loadCapturedDocument(@NonNull final Context context, final LoadCallback loadCallback) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    String docString = getSharedPreferences(context).getString(ID_DOCUMENT_KEY, null);
                    IDDocument document = null;
                    if (docString != null) {
                        try {
                            Gson gson = new Gson();
                            document = gson.fromJson(docString, IDDocument.class);
                        } catch (Exception e) {
                        }
                    }
                    final IDDocument doc = document;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (loadCallback != null) {
                                loadCallback.onLoadDocument(doc);
                            }
                        }
                    });
                }
            }
        });
    }

    private static SharedPreferences getSharedPreferences(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences("idCapture", Context.MODE_PRIVATE);
    }
}
