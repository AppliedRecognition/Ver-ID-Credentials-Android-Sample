package com.appliedrec.credentials.app;

import androidx.appcompat.app.AppCompatActivity;

import com.appliedrec.rxverid.RxVerID;

import java.util.ArrayList;
import java.util.Iterator;

import io.reactivex.disposables.Disposable;

public abstract class RxVerIDActivity extends AppCompatActivity {

    private final ArrayList<Disposable> disposables = new ArrayList<>();

    RxVerID getRxVerID() {
        return ((CredentialsApplication) getApplication()).getRxVerID();
    }

    void addDisposable(Disposable disposable) {
        disposables.add(disposable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Iterator<Disposable> iterator = disposables.iterator();
        while (iterator.hasNext()) {
            iterator.next().dispose();
            iterator.remove();
        }
    }
}
