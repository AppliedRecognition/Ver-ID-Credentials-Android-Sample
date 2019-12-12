package com.appliedrec.credentials.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.appliedrec.verid.core.Bearing;
import com.appliedrec.verid.core.DetectedFace;
import com.appliedrec.verid.core.LivenessDetectionSessionSettings;
import com.appliedrec.verid.core.RecognizableFace;
import com.appliedrec.verid.ui.VerIDSessionIntent;

import java.util.ArrayList;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class IDCardActivity extends RxVerIDActivity {

    public static final String EXTRA_DETECTED_FACE = "com.appliedrec.verid.EXTRA_DETECTED_FACE";
    public static final String EXTRA_DOCUMENT_DATA = "com.appliedrec.verid.EXTRA_DOCUMENT_DATA";
    private static final int REQUEST_CODE_LIVE_FACE = 1;
    private DetectedFace cardFace;
    private DocumentData documentData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idcard);
        Intent intent = getIntent();
        if (intent != null) {
            cardFace = intent.getParcelableExtra(EXTRA_DETECTED_FACE);
            if (cardFace != null) {
                ImageView imageView = findViewById(R.id.cardImageView);
                addDisposable(Single.create(emitter -> {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeFile(cardFace.getImageUri().getPath());
                        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
                        drawable.setCornerRadius((float)bitmap.getHeight()/16f);
                        emitter.onSuccess(drawable);
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                }).cast(RoundedBitmapDrawable.class).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                        imageView::setImageDrawable,
                        error -> {

                        }
                ));
            }
            documentData = intent.getParcelableExtra(EXTRA_DOCUMENT_DATA);
            invalidateOptionsMenu();
        }
        findViewById(R.id.button).setOnClickListener(v -> startLivenessDetection());
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
            Intent intent = new Intent(this, DocumentDetailsActivity.class);
            intent.putExtra(EXTRA_DOCUMENT_DATA, documentData);
//            intent.putExtra(EXTRA_DETECTED_FACE, cardFace);
            startActivity(intent);
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

    private void startLivenessDetection() {
        addDisposable(getRxVerID().getVerID()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        verID -> {
                            LivenessDetectionSessionSettings settings = new LivenessDetectionSessionSettings();
                            Intent intent = new VerIDSessionIntent<>(this, verID, settings);

                            startActivityForResult(intent, REQUEST_CODE_LIVE_FACE);
                        }
                ));
    }

    private void showResult(DetectedFace detectedFace) {
        Intent intent = new Intent(this, ResultActivity.class);
        ArrayList<DetectedFace> detectedFaces = new ArrayList<>();
        detectedFaces.add(cardFace);
        detectedFaces.add(detectedFace);
        intent.putParcelableArrayListExtra(ResultActivity.EXTRA_DETECTED_FACES, detectedFaces);
        startActivity(intent);
    }
}
