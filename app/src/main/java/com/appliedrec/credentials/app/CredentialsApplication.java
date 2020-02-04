package com.appliedrec.credentials.app;

import android.app.Application;

import com.microblink.MicroblinkSDK;
import com.microblink.intent.IntentDataTransferMode;

public class CredentialsApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        setupMicroblink();
    }

    private void setupMicroblink() {
        MicroblinkSDK.setIntentDataTransferMode(IntentDataTransferMode.PERSISTED_OPTIMISED);
        MicroblinkSDK.setShowTimeLimitedLicenseWarning(false);
    }
}
