package com.appliedrec.idcapturesample;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.appliedrec.ver_ididcapture.data.IDCaptureResult;

import org.json.JSONException;

/**
 * Helper class that saves the captured ID card result to shared preferences
 */

public class CardCaptureResultPersistence {

    private static final String CARD_CAPTURE_RESULT_KEY = "idCaptureResult";

    public static boolean saveCardCaptureResult(@NonNull Context context, @Nullable IDCaptureResult idCaptureResult) {
        if (idCaptureResult == null) {
            getSharedPreferences(context).edit().remove(CARD_CAPTURE_RESULT_KEY).apply();
            return true;
        }
        try {
            getSharedPreferences(context).edit().putString(CARD_CAPTURE_RESULT_KEY, idCaptureResult.toJson()).apply();
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static IDCaptureResult loadCardCaptureResult(@NonNull Context context) {
        String resultString = getSharedPreferences(context).getString(CARD_CAPTURE_RESULT_KEY, null);
        if (resultString != null) {
            try {
                return new IDCaptureResult(resultString);
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
