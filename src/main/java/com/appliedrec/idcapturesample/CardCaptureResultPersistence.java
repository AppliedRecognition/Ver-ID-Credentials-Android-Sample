package com.appliedrec.idcapturesample;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.appliedrec.ver_ididcapture.data.IDDocument;
import com.appliedrec.ver_ididcapture.data.IDDocumentCoder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;

/**
 * Helper class that saves the captured ID card result to shared preferences
 */

public class CardCaptureResultPersistence {

    private static final String ID_DOCUMENT_KEY = "idDocument";

    public static boolean saveCapturedDocument(@NonNull Context context, @Nullable IDDocument idDocument) {
        if (idDocument == null) {
            getSharedPreferences(context).edit().remove(ID_DOCUMENT_KEY);
            return true;
        }
        try {
            Gson gson = new GsonBuilder().registerTypeAdapter(IDDocument.class, new IDDocumentCoder()).create();
            String json = gson.toJson(idDocument);
            getSharedPreferences(context).edit().putString(ID_DOCUMENT_KEY, json).apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Nullable
    public static IDDocument loadCapturedDocument(@NonNull Context context) {
        String docString = getSharedPreferences(context).getString(ID_DOCUMENT_KEY, null);
        if (docString != null) {
            try {
                Gson gson = new GsonBuilder().registerTypeAdapter(IDDocument.class, new IDDocumentCoder()).create();
                IDDocument document = gson.fromJson(docString, IDDocument.class);
                return document;
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static SharedPreferences getSharedPreferences(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences("idCapture", Context.MODE_PRIVATE);
    }
}
