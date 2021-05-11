package com.appliedrec.credentials.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.appliedrec.credentials.app.databinding.ActivityResultBinding;

import org.apache.commons.math3.distribution.NormalDistribution;

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
        double probability = normalDistribution.cumulativeProbability(score) * 100.0D;
        float threshold = 3f;
        if (score >= threshold) {
            viewBinding.resultTextView.setText(R.string.pass);
            viewBinding.resultTextView.setTextColor(Color.argb(0xFF, 0x36, 0xAF, 0x00));
            viewBinding.farExplanation.setText(this.getString(R.string.far_explanation, score, probability, threshold));
        } else {
            viewBinding.resultTextView.setText(R.string.warning);
            viewBinding.resultTextView.setTextColor(Color.RED);
            viewBinding.farExplanation.setText(this.getString(R.string.warning_explanation, score, threshold));
        }
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