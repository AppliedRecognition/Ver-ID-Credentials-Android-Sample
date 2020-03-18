package com.appliedrec.credentials.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.core.util.Pair;

import com.appliedrec.credentials.app.databinding.ActivityIdcardBinding;
import com.appliedrec.rxverid.RxVerIDActivity;
import com.appliedrec.rxverid.SchedulersTransformer;
import com.appliedrec.uielements.facecomparison.ResultActivity;
import com.appliedrec.verid.core.Bearing;
import com.appliedrec.verid.core.DetectedFace;
import com.appliedrec.verid.core.LivenessDetectionSessionSettings;
import com.appliedrec.verid.core.RecognizableFace;
import com.appliedrec.verid.ui.VerIDSessionIntent;

import java.io.File;
import java.io.FileOutputStream;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class IDCardActivity extends RxVerIDActivity {

    public static final String EXTRA_DETECTED_FACE = "com.appliedrec.verid.EXTRA_DETECTED_FACE";
    public static final String EXTRA_DOCUMENT_DATA = "com.appliedrec.verid.EXTRA_DOCUMENT_DATA";
    public static final String EXTRA_CARD_IMAGE_URI = "com.appliedrec.verid.EXTRA_CARD_IMAGE_URI";
    private static final int REQUEST_CODE_LIVE_FACE = 1;
    private DetectedFace cardFace;
    private DocumentData documentData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityIdcardBinding viewBinding = ActivityIdcardBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        Intent intent = getIntent();
        if (intent != null) {
            cardFace = intent.getParcelableExtra(EXTRA_DETECTED_FACE);
            Uri cardImageUri = intent.getParcelableExtra(EXTRA_CARD_IMAGE_URI);
            if (cardImageUri != null) {
                viewBinding.cardImageView.setOnClickListener(view -> showCardDetails());
                addDisposable(Single.create(emitter -> {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeFile(cardImageUri.getPath());
                        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
                        drawable.setCornerRadius((float)bitmap.getHeight()/16f);
                        emitter.onSuccess(drawable);
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                }).cast(RoundedBitmapDrawable.class).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                        viewBinding.cardImageView::setImageDrawable,
                        error -> {

                        }
                ));
            }
            documentData = intent.getParcelableExtra(EXTRA_DOCUMENT_DATA);
            invalidateOptionsMenu();
        }
        viewBinding.button.setOnClickListener(v -> startLivenessDetection());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.card, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_details).setVisible(documentData != null && cardFace != null && cardFace.getImageUri() != null);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_LIVE_FACE && resultCode == RESULT_OK) {
            addDisposable(getRxVerID().getSessionResultFromIntent(data)
                    .flatMapObservable(result -> getRxVerID().getFacesAndImageUrisFromSessionResult(result, Bearing.STRAIGHT))
                    .filter(detectedFace -> detectedFace.getFace() instanceof RecognizableFace)
                    .firstOrError()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            this::showResult,
                            error -> {

                            }
                    ));
        }
    }

    private void showCardDetails() {
        Intent intent = new Intent(this, DocumentDetailsActivity.class);
        intent.putExtra(EXTRA_DOCUMENT_DATA, documentData);
        startActivity(intent);
    }

    private void startLivenessDetection() {
        addDisposable(getRxVerID().getVerID()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        verID -> {
                            LivenessDetectionSessionSettings settings = new LivenessDetectionSessionSettings();
                            Intent intent = new VerIDSessionIntent<>(this, verID, settings);

                            startActivityForResult(intent, REQUEST_CODE_LIVE_FACE);
                        }
                ));
    }

    private void showError(Throwable error) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.face_comparison_failed)
                .setMessage(error.getLocalizedMessage())
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }

    private void showResult(DetectedFace detectedFace) {
        addDisposable(getRxVerID().compareFaceToFaces((RecognizableFace)detectedFace.getFace(), new RecognizableFace[]{(RecognizableFace)cardFace.getFace()}).flatMap(score -> Observable.just(detectedFace, cardFace).flatMap(capture -> getRxVerID().cropImageToFace(capture.getImageUri(), capture.getFace()).map(bitmap -> {
            File tempFile = File.createTempFile("verid_",".jpg");
            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
                return Uri.fromFile(tempFile);
            }
        }).toObservable()).toList(2).map(list -> new Pair<>(new Pair<>(list.get(0), list.get(1)),score))).compose(SchedulersTransformer.defaultInstance()).subscribe(result -> {
            Intent intent = new Intent(this, ResultActivity.class);
            intent.putExtra(ResultActivity.EXTRA_SCORE, result.second);
            //noinspection ConstantConditions
            intent.putExtra(ResultActivity.EXTRA_FACE1_IMAGE_URI, result.first.first);
            intent.putExtra(ResultActivity.EXTRA_FACE2_IMAGE_URI, result.first.second);
            startActivity(intent);
        }, this::showError));
    }
}
