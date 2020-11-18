package com.appliedrec.credentials.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.appliedrec.credentials.app.databinding.ActivityResultBinding;
import com.appliedrec.uielements.facecomparison.ScoreTableActivity;
import com.appliedrec.verid.core2.session.FaceCapture;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.Locale;

public class ResultActivity extends BaseActivity {

    public static final String EXTRA_SCORE = "EXTRA_SCORE";
    public static final String EXTRA_CARD_FACE_CAPTURE = "EXTRA_CARD_FACE_CAPTURE";
    public static final String EXTRA_LIVE_FACE_CAPTURE = "EXTRA_LIVE_FACE_CAPTURE";

    private ActivityResultBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityResultBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        float score = getIntent().getFloatExtra(EXTRA_SCORE, 0f);
        NormalDistribution normalDistribution = new NormalDistribution();
        double probability = (1.0D - normalDistribution.cumulativeProbability(score)) * 100.0D;
        viewBinding.scoreTextView.setText(String.format(Locale.getDefault(), "%.02f", score));
        viewBinding.farExplanation.setText(this.getString(R.string.far_explanation, probability));
        viewBinding.button.setOnClickListener(button -> startActivity(new Intent(this, ScoreTableActivity.class)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewBinding = null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            try {
                getSharedData().setSharedObject(EXTRA_LIVE_FACE_CAPTURE, null);
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public void onVerIDPropertiesAvailable() {
        try {
            byte[] cardFace = getSharedData().getSharedData(EXTRA_CARD_FACE_CAPTURE);
            Bitmap cardFaceImage = BitmapFactory.decodeByteArray(cardFace, 0, cardFace.length);
            byte[] liveFace = getSharedData().getSharedData(EXTRA_LIVE_FACE_CAPTURE);
            Bitmap liveFaceImage = BitmapFactory.decodeByteArray(liveFace, 0, liveFace.length);
            RoundedBitmapDrawable drawable1 = RoundedBitmapDrawableFactory.create(getResources(), cardFaceImage);
            RoundedBitmapDrawable drawable2 = RoundedBitmapDrawableFactory.create(getResources(), liveFaceImage);
            drawable1.setCornerRadius((float) cardFaceImage.getHeight() / 8f);
            drawable2.setCornerRadius((float) liveFaceImage.getHeight() / 8f);
            viewBinding.face1ImageView.setImageDrawable(drawable1);
            viewBinding.face2ImageView.setImageDrawable(drawable2);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load face images", Toast.LENGTH_SHORT).show();
        }
    }
}