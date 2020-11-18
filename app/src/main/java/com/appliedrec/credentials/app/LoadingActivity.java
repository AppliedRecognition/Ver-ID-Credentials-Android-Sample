package com.appliedrec.credentials.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.appliedrec.verid.core2.VerID;
import com.appliedrec.verid.core2.VerIDFactory;
import com.appliedrec.verid.core2.VerIDFactoryDelegate;

public class LoadingActivity extends AppCompatActivity implements VerIDFactoryDelegate {

    private VerID verID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        VerIDFactory verIDFactory = new VerIDFactory(this);
        verIDFactory.setDelegate(this);
        verIDFactory.createVerID();
    }

    @Override
    public void onVerIDCreated(VerIDFactory verIDFactory, VerID verID) {
        this.verID = verID;
        finish();
        startActivity(new Intent(this, MainActivity.class));
    }

    @Override
    public void onVerIDCreationFailed(VerIDFactory verIDFactory, Exception e) {

    }

    VerID getVerID() {
        return verID;
    }
}