package com.appliedrec.credentials.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.appliedrec.credentials.app.databinding.ActivityIdcardBinding;
import com.appliedrec.verid.core2.Bearing;
import com.appliedrec.verid.core2.IRecognizable;
import com.appliedrec.verid.core2.session.FaceCapture;
import com.appliedrec.verid.core2.session.LivenessDetectionSessionSettings;
import com.appliedrec.verid.core2.session.VerIDSessionResult;
import com.appliedrec.verid.ui2.IVerIDSession;
import com.appliedrec.verid.ui2.VerIDSession;
import com.appliedrec.verid.ui2.VerIDSessionDelegate;

import java.io.ByteArrayOutputStream;

public class IDCardActivity extends BaseActivity implements VerIDSessionDelegate {

    public static final String EXTRA_FACE_IMAGE = "com.appliedrec.verid.EXTRA_FACE_IMAGE";
    public static final String EXTRA_DOCUMENT_DATA = "com.appliedrec.verid.EXTRA_DOCUMENT_DATA";
    private FaceWithImage faceWithImage;
    private DocumentData documentData;
    private ActivityIdcardBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityIdcardBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
    }

    @Override
    public void onVerIDPropertiesAvailable() {
        try {
            faceWithImage = getSharedData().getSharedObject(EXTRA_FACE_IMAGE, FaceWithImage.class);
            if (faceWithImage != null) {
                viewBinding.cardImageView.setOnClickListener(view -> showCardDetails());
                RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), faceWithImage.getBitmap());
                drawable.setCornerRadius((float) faceWithImage.getBitmap().getHeight() / 16f);
                viewBinding.cardImageView.setImageDrawable(drawable);
            }
            documentData = getSharedData().getSharedObject(EXTRA_DOCUMENT_DATA, DocumentData.class);
            invalidateOptionsMenu();
            viewBinding.button.setOnClickListener(v -> startLivenessDetection());
        } catch (Exception e) {

        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            try {
                getSharedData().setSharedObject(EXTRA_FACE_IMAGE, null);
                getSharedData().setSharedObject(EXTRA_DOCUMENT_DATA, null);
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.card, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_details).setVisible(documentData != null && faceWithImage != null);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_details) {
            showCardDetails();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCardDetails() {
        Intent intent = new Intent(this, DocumentDetailsActivity.class);
        intent.putExtra(EXTRA_DOCUMENT_DATA, documentData);
        startActivity(intent);
    }

    private void startLivenessDetection() {
        try {
            LivenessDetectionSessionSettings sessionSettings = new LivenessDetectionSessionSettings();
            VerIDSession session = new VerIDSession(getVerID(), sessionSettings);
            session.setDelegate(this);
            session.start();
        } catch (Exception e) {
            showError(e);
        }
    }

    private void showError(Throwable error) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.face_comparison_failed)
                .setMessage(error.getLocalizedMessage())
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }

    @SuppressLint("CheckResult")
    private void showResult(FaceCapture detectedFace) {
        try {
            float score = getVerID().getFaceRecognition().compareSubjectFacesToFaces(new IRecognizable[]{detectedFace.getFace()}, new IRecognizable[]{faceWithImage.getFace()});
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                Rect faceBounds = new Rect();
                faceWithImage.getFace().getBounds().round(faceBounds);
                faceBounds.intersect(0, 0, faceWithImage.getBitmap().getWidth(), faceWithImage.getBitmap().getHeight());
                Bitmap faceImage = Bitmap.createBitmap(faceWithImage.getBitmap(), faceBounds.left, faceBounds.top, faceBounds.width(), faceBounds.height());
                faceImage.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                getSharedData().setSharedData(ResultActivity.EXTRA_CARD_FACE_CAPTURE, outputStream.toByteArray());
            }
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                detectedFace.getFaceImage().compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                getSharedData().setSharedData(ResultActivity.EXTRA_LIVE_FACE_CAPTURE, outputStream.toByteArray());
            }
            Intent intent = new Intent(this, ResultActivity.class);
            intent.putExtra(ResultActivity.EXTRA_SCORE, score);
            startActivity(intent);
        } catch (Exception e) {
            showError(e);
        }
    }

    @Override
    public void onSessionFinished(IVerIDSession<?> abstractVerIDSession, VerIDSessionResult verIDSessionResult) {
        if (!verIDSessionResult.getError().isPresent()) {
            verIDSessionResult.getFirstFaceCapture(Bearing.STRAIGHT).ifPresent(this::showResult);
        }
    }
}
