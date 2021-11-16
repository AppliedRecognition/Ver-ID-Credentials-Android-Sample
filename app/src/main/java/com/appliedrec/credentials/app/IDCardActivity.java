package com.appliedrec.credentials.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
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
    public static final String EXTRA_AUTHENTICITY_SCORE = "com.appliedrec.verid.EXTRA_AUTHENTICITY_SCORE";
    public static final String EXTRA_FRONT_BACK_MATCH_SCORE = "com.appliedrec.verid.EXTRA_FRONT_BACK_MATCH_SCORE";
    private FaceWithImage faceWithImage;
    private DocumentData documentData;
    private Float frontBackMatchScore;
    private ActivityIdcardBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityIdcardBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        frontBackMatchScore = getIntent().getFloatExtra(EXTRA_FRONT_BACK_MATCH_SCORE, -1f);
        if (frontBackMatchScore == -1f) {
            frontBackMatchScore = null;
        }
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
        } catch (Exception ignore) {
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
        if (faceWithImage != null && faceWithImage.getAuthenticityScore() != null) {
            intent.putExtra(EXTRA_AUTHENTICITY_SCORE, faceWithImage.getAuthenticityScore());
        }
        if (frontBackMatchScore != null) {
            intent.putExtra(EXTRA_FRONT_BACK_MATCH_SCORE, frontBackMatchScore);
        }
        startActivity(intent);
    }

    private void startLivenessDetection() {
        try {
            LivenessDetectionSessionSettings sessionSettings = new LivenessDetectionSessionSettings();
            VerIDSession session = new VerIDSession(getVerID(), sessionSettings);
            session.setDelegate(this);
            session.start();
        } catch (Exception e) {
            showError(R.string.failed_to_start_session);
        }
    }

    private void showError(@StringRes int error) {
        new AlertDialog.Builder(this)
                .setTitle(error)
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
            showError(R.string.failed_to_prepare_face_images);
        }
    }

    @Override
    public void onSessionFinished(IVerIDSession<?> abstractVerIDSession, VerIDSessionResult verIDSessionResult) {
        if (!verIDSessionResult.getError().isPresent()) {
            verIDSessionResult.getFirstFaceCapture(Bearing.STRAIGHT).ifPresent(this::showResult);
        } else {
            showError(R.string.face_capture_failed);
        }
    }
}
