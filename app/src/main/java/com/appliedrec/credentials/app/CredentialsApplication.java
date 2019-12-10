package com.appliedrec.credentials.app;

import android.app.Application;

import com.appliedrec.rxverid.RxVerID;
import com.microblink.MicroblinkSDK;
import com.microblink.intent.IntentDataTransferMode;

public class CredentialsApplication extends Application {

    private RxVerID rxVerID;

    @Override
    public void onCreate() {
        super.onCreate();
        setupMicroblink();
        setupVerID();
    }

    private void setupMicroblink() {
        MicroblinkSDK.setIntentDataTransferMode(IntentDataTransferMode.PERSISTED_OPTIMISED);
        MicroblinkSDK.setShowTimeLimitedLicenseWarning(false);
    }

    private void setupVerID() {
        rxVerID = new RxVerID.Builder(this).build();
    }

    public RxVerID getRxVerID() {
        return rxVerID;
    }
}
