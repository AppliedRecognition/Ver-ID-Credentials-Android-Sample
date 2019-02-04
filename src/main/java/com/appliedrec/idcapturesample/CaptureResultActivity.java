package com.appliedrec.idcapturesample;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.appliedrec.ver_id.VerID;
import com.appliedrec.ver_id.model.FaceTemplate;
import com.appliedrec.ver_id.model.VerIDFace;
import com.appliedrec.ver_id.session.VerIDSessionResult;
import com.appliedrec.ver_id.util.FaceUtil;
import com.appliedrec.ver_ididcapture.data.IDDocument;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class CaptureResultActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks {

    // Extra name constant
    public static final String EXTRA_LIVENESS_DETECTION_RESULT = "com.appliedrec.ver_ididcapture.EXTRA_LIVENESS_DETECTION_RESULT";

    // Loader IDs
    private static final int LOADER_ID_SCORE = 458;
    private static final int LOADER_ID_CARD_FACE = 86;
    private static final int LOADER_ID_LIVE_FACE = 355;

    // Views
    private ImageView liveFaceView;
    private ImageView cardFaceView;
    private LikenessGaugeView likenessGaugeView;
//    private TextView scoreTextView;
    private TextView resultTextView;
//    private View progressIndicatorView;

    // Extracted card and face images
    private Bitmap cardFaceImage;
    private Bitmap liveFaceImage;

    // Live face and ID capture results
    private VerIDSessionResult livenessDetectionResult;
    private IDDocument idDocument;

    /**
     * Loader that compares the face from the card with the live face(s)
     */
    private static class ScoreLoader extends AsyncTaskLoader<Float> {

        private FaceTemplate cardFaceTemplate;
        private VerIDFace[] liveFaces;

        public ScoreLoader(Context context, FaceTemplate cardFaceTemplate, VerIDFace[] liveFaces) {
            super(context);
            this.cardFaceTemplate = cardFaceTemplate;
            this.liveFaces = liveFaces;
        }

        @Override
        public Float loadInBackground() {
            Float score = null;
            for (VerIDFace face : liveFaces) {
                try {
                    VerIDFace cardFace = new VerIDFace(cardFaceTemplate);
                    float faceScore = FaceUtil.compareFaces(cardFace, face);
                    if (score == null) {
                        score = faceScore;
                    } else {
                        score = Math.max(faceScore, score);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return score;
        }
    }

    /**
     * Loader that loads an image and crops it to the given bounds
     */
    private static class ImageLoader extends AsyncTaskLoader<Bitmap> {

        private Uri imageUri;
        private Rect faceBounds;

        public ImageLoader(Context context, Uri imageUri, Rect faceBounds) {
            super(context);
            this.imageUri = imageUri;
            this.faceBounds = faceBounds;
        }

        @Override
        public Bitmap loadInBackground() {
            try {
                InputStream inputStream = getContext().getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (bitmap != null) {
                    Rect imageRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
                    // Add a bit of space around the face for better detection
                    faceBounds.inset(0-(int)((double)faceBounds.width()*0.1), 0-(int)((double)faceBounds.height()*0.1));
                    // Ensure the face is contained within the bounds of the image
                    //noinspection CheckResult
                    imageRect.intersect(faceBounds);
                    return Bitmap.createBitmap(bitmap, imageRect.left, imageRect.top, imageRect.width(), imageRect.height());
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_result);
        liveFaceView = findViewById(R.id.live_face);
        cardFaceView = findViewById(R.id.card_face);
        resultTextView = findViewById(R.id.text);
        likenessGaugeView = findViewById(R.id.likeness_gauge);
        idDocument = CardCaptureResultPersistence.loadCapturedDocument(this);
        Intent intent = getIntent();
        if (idDocument != null && idDocument.getFaceTemplate() != null && intent != null) {
            livenessDetectionResult = intent.getParcelableExtra(EXTRA_LIVENESS_DETECTION_RESULT);
            if (livenessDetectionResult != null && livenessDetectionResult.getFacesSuitableForRecognition(VerID.Bearing.STRAIGHT).length > 0) {
                // Get the cropped face images
                getSupportLoaderManager().initLoader(LOADER_ID_CARD_FACE, intent.getExtras(), this).forceLoad();
                getSupportLoaderManager().initLoader(LOADER_ID_LIVE_FACE, intent.getExtras(), this).forceLoad();
                getSupportLoaderManager().initLoader(LOADER_ID_SCORE, null, this).forceLoad();
                return;
            }
        }
        updateScore(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getLoaderManager().destroyLoader(LOADER_ID_CARD_FACE);
        getLoaderManager().destroyLoader(LOADER_ID_LIVE_FACE);
        getLoaderManager().destroyLoader(LOADER_ID_SCORE);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_CARD_FACE:
                Uri cardImageUri = idDocument.getImageUri();
                Rect cardImageFaceBounds = new Rect();
                idDocument.getFaceBounds().round(cardImageFaceBounds);
                return new ImageLoader(this, cardImageUri, cardImageFaceBounds);
            case LOADER_ID_LIVE_FACE:
                VerIDFace face = livenessDetectionResult.getFacesSuitableForRecognition(VerID.Bearing.STRAIGHT)[0];
                Uri faceImageUri = livenessDetectionResult.getFaceImages().get(face);
                Rect faceImageFaceBounds = new Rect();
                face.getBounds().round(faceImageFaceBounds);
                return new ImageLoader(this, faceImageUri, faceImageFaceBounds);
            case LOADER_ID_SCORE:
                return new ScoreLoader(this, idDocument.getFaceTemplate(), livenessDetectionResult.getFacesSuitableForRecognition(VerID.Bearing.STRAIGHT));
            default:
                return null;

        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        switch (loader.getId()) {
            case LOADER_ID_CARD_FACE:
                if (data != null) {
                    cardFaceImage = (Bitmap) data;
                    if (Build.VERSION.SDK_INT >= 21) {
                        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), cardFaceImage);
                        drawable.setCornerRadius((float) drawable.getIntrinsicWidth() / 10f);
                        cardFaceView.setImageDrawable(drawable);
                    } else {
                        cardFaceView.setImageBitmap(cardFaceImage);
                    }
                }
                break;
            case LOADER_ID_LIVE_FACE:
                if (data != null) {
                    liveFaceImage = (Bitmap) data;
                    if (Build.VERSION.SDK_INT >= 21) {
                        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), liveFaceImage);
                        drawable.setCornerRadius((float)drawable.getIntrinsicWidth() / 10f);
                        liveFaceView.setImageDrawable(drawable);
                    } else {
                        liveFaceView.setImageBitmap(liveFaceImage);
                    }
                }
                break;
            case LOADER_ID_SCORE:
                Float score = (Float) data;
                updateScore(score);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
        switch (loader.getId()) {
            case LOADER_ID_CARD_FACE:
                cardFaceView.setImageDrawable(null);
                break;
            case LOADER_ID_LIVE_FACE:
                liveFaceView.setImageDrawable(null);
                break;
            case LOADER_ID_SCORE:
                break;
        }
    }

    @UiThread
    private void updateScore(Float score) {
        if (score != null) {
            resultTextView.setText(getResources().getString(R.string.similarity_score, score.floatValue() * 10f));
        } else {
            score = 0f;
            resultTextView.setText(R.string.face_score_error);
        }
        likenessGaugeView.setScore(score.floatValue());
    }
}
