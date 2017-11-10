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
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.appliedrec.detreclib.detection.FBFace;
import com.appliedrec.detreclib.detection.IFace;
import com.appliedrec.ver_id.FaceTemplateExtraction;
import com.appliedrec.ver_id.VerID;
import com.appliedrec.ver_id.model.RecognitionFace;
import com.appliedrec.ver_id.session.VerIDSessionResult;
import com.appliedrec.ver_id.util.FaceUtil;
import com.appliedrec.ver_ididcapture.data.IDCaptureResult;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class CaptureResultActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks, FaceTemplateExtraction.FaceTemplateExtractionListener {

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
    private TextView scoreTextView;
    private TextView resultTextView;
    private View progressIndicatorView;

    // Extracted card and face images
    private Bitmap cardFaceImage;
    private Bitmap liveFaceImage;

    // Live face and ID capture results
    private VerIDSessionResult livenessDetectionResult;
    private IDCaptureResult idCaptureResult;

    /**
     * Loader that compares the face from the card with the live face(s)
     */
    private static class ScoreLoader extends AsyncTaskLoader<Float> {

        private IFace cardFace;
        private IFace[] liveFaces;

        public ScoreLoader(Context context, IFace cardFace, IFace[] liveFaces) {
            super(context);
            this.cardFace = cardFace;
            this.liveFaces = liveFaces;
        }

        @Override
        public Float loadInBackground() {
            Float score = null;
            for (IFace face : liveFaces) {
                try {
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
        liveFaceView = (ImageView) findViewById(R.id.live_face);
        cardFaceView = (ImageView) findViewById(R.id.card_face);
        scoreTextView = (TextView) findViewById(R.id.likeness_score);
        resultTextView = (TextView) findViewById(R.id.text);
        likenessGaugeView = (LikenessGaugeView) findViewById(R.id.likeness_gauge);
        progressIndicatorView = findViewById(R.id.score_progress);
        findViewById(R.id.done_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK);
                finish();
            }
        });
        idCaptureResult = CardCaptureResultPersistence.loadCardCaptureResult(this);
        Intent intent = getIntent();
        if (idCaptureResult != null && idCaptureResult.getFace() != null && intent != null) {
            livenessDetectionResult = intent.getParcelableExtra(EXTRA_LIVENESS_DETECTION_RESULT);
            if (livenessDetectionResult != null && livenessDetectionResult.getRecognitionFaces(VerID.Bearing.STRAIGHT).length > 0) {
                // Get the cropped face images
                getSupportLoaderManager().initLoader(LOADER_ID_CARD_FACE, intent.getExtras(), this).forceLoad();
                getSupportLoaderManager().initLoader(LOADER_ID_LIVE_FACE, intent.getExtras(), this).forceLoad();
                if (idCaptureResult.getFace().isSuitableForRecognition()) {
                    // The card capture result has a suitable face
                    compareFaceTemplates();
                } else if (idCaptureResult.getFace().isBackgroundProcessing()) {
                    // The ID card face is being processed. Listen for face template extraction events
                    VerID.shared.getFaceTemplateExtraction().addListener(idCaptureResult.getFace().getId(), this);
                }
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
        if (idCaptureResult != null && idCaptureResult.getFace() != null && idCaptureResult.getFace().isBackgroundProcessing()) {
            VerID.shared.getFaceTemplateExtraction().removeListener(idCaptureResult.getFace().getId(), this);
        }
    }

    private void compareFaceTemplates() {
        if (idCaptureResult.getFace().isSuitableForRecognition()) {
            // The face on the ID card is registered, calculate the similarity score
            Loader loader = getSupportLoaderManager().initLoader(LOADER_ID_SCORE, null, this);
            if (loader != null) {
                loader.forceLoad();
            }
        }
    }

    @Override
    public void onFaceTemplateExtracted(long faceId, FBFace face) {
        VerID.shared.getFaceTemplateExtraction().removeListener(faceId, this);
        if (idCaptureResult.getFace() != null && idCaptureResult.getFace().isBackgroundProcessing() && idCaptureResult.getFace().getId() == faceId) {
            // Face template has been extracted from the face in the ID card. The face is now ready to be registered
            idCaptureResult.setFace(face);
            CardCaptureResultPersistence.saveCardCaptureResult(this, idCaptureResult);
            if (idCaptureResult.getFace().isSuitableForRecognition()) {
                compareFaceTemplates();
            } else {
                updateScore(null);
            }
        }
    }

    @Override
    public void onFaceTemplateExtractionProgress(long faceId, double progress) {

    }

    @Override
    public void onFaceTemplateExtractionFailed(long faceId, Exception exception) {
        VerID.shared.getFaceTemplateExtraction().removeListener(faceId, this);
        updateScore(null);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_CARD_FACE:
                Uri cardImageUri = idCaptureResult.getFrontImageUri();
                Rect cardImageFaceBounds = new Rect();
                idCaptureResult.getFaceBounds().round(cardImageFaceBounds);
                return new ImageLoader(this, cardImageUri, cardImageFaceBounds);
            case LOADER_ID_LIVE_FACE:
                RecognitionFace[] faces = livenessDetectionResult.getRecognitionFaces(VerID.Bearing.STRAIGHT);
                if (faces.length > 0) {
                    RecognitionFace face = faces[0];
                    Uri faceImageUri = livenessDetectionResult.getFaceImages().get(face);
                    Rect faceImageFaceBounds = new Rect();
                    face.getBounds().round(faceImageFaceBounds);
                    return new ImageLoader(this, faceImageUri, faceImageFaceBounds);
                }
                return null;
            case LOADER_ID_SCORE:
                if (idCaptureResult.getFace() != null) {
                    return new ScoreLoader(this, idCaptureResult.getFace(), livenessDetectionResult.getRecognitionFaces(VerID.Bearing.STRAIGHT));
                } else {
                    updateScore(null);
                    return null;
                }
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
        progressIndicatorView.setVisibility(View.GONE);
        scoreTextView.setVisibility(View.VISIBLE);
        if (score != null) {
            scoreTextView.setText(String.format("%.0f%%", score.floatValue() * 100f));
            resultTextView.setText("");
        } else {
            score = 0f;
            scoreTextView.setText("?");
            resultTextView.setText(R.string.face_score_error);
        }
        likenessGaugeView.setScore(score.floatValue());
    }
}
