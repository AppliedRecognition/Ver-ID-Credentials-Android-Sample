package com.appliedrec.credentials.app;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;
import androidx.multidex.MultiDexApplication;
import androidx.test.espresso.IdlingResource;

import com.appliedrec.verid.core2.VerID;
import com.appliedrec.verid.core2.serialization.Cbor;
import com.microblink.MicroblinkSDK;
import com.microblink.intent.IntentDataTransferMode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Set;

public class CredentialsApplication extends MultiDexApplication implements Application.ActivityLifecycleCallbacks, ISharedData, IdlingResource {

    private VerID verID;
    private final HashMap<String, byte[]> sharedData = new HashMap<>();
    private boolean isIdle = false;
    private Set<ResourceCallback> resourceCallbacks = new ArraySet<>();

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
        setupMicroblink();
    }

    private void setupMicroblink() {
        MicroblinkSDK.setIntentDataTransferMode(IntentDataTransferMode.PERSISTED_OPTIMISED);
        MicroblinkSDK.setShowTimeLimitedLicenseWarning(false);
    }

    //region ActivityLifecycleCallbacks

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
        if (activity instanceof BaseActivity && verID != null) {
            ((BaseActivity) activity).setVerIDProperties(verID, this);
            isIdle = true;
            for (ResourceCallback cb : resourceCallbacks) {
                cb.onTransitionToIdle();
            }
            resourceCallbacks.clear();
        }
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (activity instanceof LoadingActivity && activity.isFinishing()) {
            verID = ((LoadingActivity)activity).getVerID();
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).destroy();
        }
    }

    @Override
    public <T> void setSharedObject(String key, T object) throws Exception {
        byte[] data = null;
        if (object != null) {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                Cbor.encodeToStream(object, outputStream);
                data = outputStream.toByteArray();
            }
        }
        setSharedData(key, data);
    }

    @Override
    public <T> T getSharedObject(String key, Class<T> type) throws Exception {
        byte[] data = getSharedData(key);
        if (data == null) {
            return null;
        }
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
            T result = Cbor.decodeStream(inputStream, type);
            return result;
        }
    }

    @Override
    public void setSharedData(String key, byte[] data) throws Exception {
        if (data == null) {
            sharedData.remove(key);
        } else {
            sharedData.put(key, data);
        }
    }

    @Override
    public byte[] getSharedData(String key) throws Exception {
        return sharedData.get(key);
    }

    @Override
    public String getName() {
        return "Credentials application";
    }

    @Override
    public boolean isIdleNow() {
        return isIdle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback callback) {
        resourceCallbacks.add(callback);
    }

    //endregion
}
