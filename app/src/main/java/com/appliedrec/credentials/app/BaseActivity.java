package com.appliedrec.credentials.app;

import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.appliedrec.verid.core2.VerID;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.disposables.Disposable;

public abstract class BaseActivity extends AppCompatActivity {

    ArrayList<Disposable> disposables = new ArrayList<>();
    AtomicReference<VerID> verIDRef = new AtomicReference<>();
    AtomicReference<ISharedData> sharedDataRef = new AtomicReference<>();

    final void setVerIDProperties(VerID verID, ISharedData sharedData) {
        verIDRef.set(verID);
        sharedDataRef.set(sharedData);
        new Handler(Looper.getMainLooper()).post(this::onVerIDPropertiesAvailable);
    }

    protected void onVerIDPropertiesAvailable() {
    }

    final void addDisposable(Disposable disposable) {
        disposables.add(disposable);
    }

    final void destroy() {
        Iterator<Disposable> disposableIterator = disposables.iterator();
        while (disposableIterator.hasNext()) {
            disposableIterator.next().dispose();
            disposableIterator.remove();
        }
        sharedDataRef.set(null);
        verIDRef.set(null);
    }

    final VerID getVerID() throws Exception {
        VerID verID = verIDRef.get();
        if (verID == null) {
            throw new Exception("Ver-ID not available");
        }
        return verID;
    }

    final ISharedData getSharedData() throws Exception {
        ISharedData sharedData = sharedDataRef.get();
        if (sharedData == null) {
            throw new Exception("Shared data not available");
        }
        return sharedData;
    }
}
