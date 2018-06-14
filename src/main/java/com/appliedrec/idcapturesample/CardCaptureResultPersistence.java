package com.appliedrec.idcapturesample;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.appliedrec.ver_ididcapture.data.IDDocument;

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
            getSharedPreferences(context).edit().putString(ID_DOCUMENT_KEY, idDocument.getSerialized()).apply();
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static IDDocument loadCapturedDocument(@NonNull Context context) {
        String docString = getSharedPreferences(context).getString(ID_DOCUMENT_KEY, null);
        if (docString != null) {
            try {
                return new IDDocument(docString);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static SharedPreferences getSharedPreferences(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences("idCapture", Context.MODE_PRIVATE);
    }
}
