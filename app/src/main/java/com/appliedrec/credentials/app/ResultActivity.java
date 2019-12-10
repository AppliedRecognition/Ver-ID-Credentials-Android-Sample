package com.appliedrec.credentials.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.Group;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.appliedrec.verid.core.DetectedFace;
import com.appliedrec.verid.core.RecognizableFace;

import org.javatuples.Pair;
import org.javatuples.Quartet;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ResultActivity extends RxVerIDActivity {

    public static final String EXTRA_DETECTED_FACES = "com.appliedrec.verid.EXTRA_DETECTED_FACES";

    Single<Pair<Float,Float>> compareIDCardToLiveFace(Uri imageFileUri, RecognizableFace face) {
        return getRxVerID().detectRecognizableFacesInImage(imageFileUri, 1)
                .singleOrError()
                .flatMap(cardFace -> getRxVerID().compareFaceToFaces(cardFace, new RecognizableFace[]{face}))
                .flatMap(score -> getRxVerID().getVerID().map(verID -> new Pair<>(score, verID.getFaceRecognition().getAuthenticationThreshold())))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }
        ProgressBar progressBar = findViewById(R.id.progressBar);
        Group resultGroup = findViewById(R.id.result);
        ImageView cardFaceImageView = findViewById(R.id.cardFaceImageView);
        ImageView liveFaceImageView = findViewById(R.id.liveFaceImageView);
        ArrayList<DetectedFace> detectedFaces = intent.getParcelableArrayListExtra(EXTRA_DETECTED_FACES);
        if (detectedFaces == null) {
            return;
        }
        DetectedFace[] detectedFacesArray = new DetectedFace[detectedFaces.size()];
        detectedFaces.toArray(detectedFacesArray);
        TextView scoreTextView = findViewById(R.id.scoreTextView);
        DialView dialView = findViewById(R.id.dialView);
        findViewById(R.id.scoreExplanationLink).setOnClickListener(view -> showScoreExplanation());

        resultGroup.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        addDisposable(Observable.fromArray(detectedFacesArray)
                .flatMap(detectedFace -> getRxVerID().cropImageToFace(detectedFace.getImageUri(), detectedFace.getFace()).toObservable()
                        .map(bitmap -> {
                            RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
                            drawable.setCornerRadius((float)bitmap.getWidth()/8f);
                            return new Pair<>(detectedFace, drawable);
                        }))
                .toList()
                .flatMap(list -> getRxVerID().compareFaceToFaces((RecognizableFace)list.get(0).getValue0().getFace(), new RecognizableFace[]{(RecognizableFace)list.get(1).getValue0().getFace()})
                        .map(score -> new Pair<>(score, list)))
                .flatMap(result -> getRxVerID().getVerID().map(verID -> new Quartet<>(result.getValue0(), verID.getFaceRecognition().getAuthenticationThreshold(), verID.getFaceRecognition().getMaxAuthenticationScore(), result.getValue1())))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            progressBar.setVisibility(View.GONE);
                            resultGroup.setVisibility(View.VISIBLE);
                            cardFaceImageView.setImageDrawable(result.getValue3().get(0).getValue1());
                            liveFaceImageView.setImageDrawable(result.getValue3().get(1).getValue1());
                            scoreTextView.setText(getString(R.string.result_score, result.getValue0(), result.getValue1()));
                            dialView.setScore(result.getValue0(), result.getValue1(), result.getValue2());
                        },
                        error -> {
                            progressBar.setVisibility(View.GONE);
                            new AlertDialog.Builder(this)
                                    .setTitle(R.string.face_comparison_failed)
                                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> finish())
                                    .setCancelable(false)
                                    .create()
                                    .show();
                        }
                ));
    }

    private void showScoreExplanation() {
        Intent intent = new Intent(this, ScoreTableActivity.class);
        startActivity(intent);
    }
}
