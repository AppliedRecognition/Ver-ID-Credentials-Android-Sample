package com.appliedrec.credentials.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.appliedrec.verid.core2.Classifier;
import com.appliedrec.verid.core2.FaceDetectionRecognitionFactory;
import com.appliedrec.verid.core2.FaceDetectionRecognitionSettings;
import com.appliedrec.verid.core2.VerID;
import com.appliedrec.verid.core2.VerIDFactory;
import com.appliedrec.verid.core2.VerIDFactoryDelegate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;

public class LoadingActivity extends AppCompatActivity implements VerIDFactoryDelegate {

    private VerID verID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        FaceDetectionRecognitionSettings faceDetectionRecognitionSettings = new FaceDetectionRecognitionSettings(null);
        faceDetectionRecognitionSettings.setFaceExtractQualityThreshold(4.0f);
        FaceDetectionRecognitionFactory faceDetectionRecognitionFactory = new FaceDetectionRecognitionFactory(this, faceDetectionRecognitionSettings);
        try {
            Classifier licenceAuthenticityClassifiers[] = AuthenticityScoreSupport.defaultInstance().getClassifiers(this);
            for (Classifier classifier : licenceAuthenticityClassifiers) {
                faceDetectionRecognitionFactory.addClassifier(classifier);
            }
        } catch (IOException ignore) {
            // Failed to load licence model file. Authenticity detection will be disabled.
        }
        VerIDFactory verIDFactory = new VerIDFactory(this);
        verIDFactory.setFaceDetectionFactory(faceDetectionRecognitionFactory);
        verIDFactory.setFaceRecognitionFactory(faceDetectionRecognitionFactory);
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
        new AlertDialog.Builder(this)
                .setTitle(R.string.verid_failed_to_load)
                .setMessage(e.getLocalizedMessage())
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    finish();
                })
                .create()
                .show();
    }

    VerID getVerID() {
        return verID;
    }
}