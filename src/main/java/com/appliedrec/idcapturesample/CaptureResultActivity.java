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
import com.appliedrec.ver_id.FaceTemplateExtraction;
import com.appliedrec.ver_id.VerID;
import com.appliedrec.ver_id.model.VerIDUser;
import com.appliedrec.ver_id.session.VerIDSessionResult;
import com.appliedrec.ver_id.util.FaceUtil;
import com.appliedrec.ver_ididcapture.data.IDCaptureResult;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class CaptureResultActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks, FaceTemplateExtraction.FaceTemplateExtractionListener {

    // Extra name constants
    public static final String EXTRA_CAPTURE_RESULT = "com.appliedrec.ver_ididcapture.EXTRA_CAPTURE_RESULT";
    public static final String EXTRA_LIVENESS_DETECTION_RESULT = "com.appliedrec.ver_ididcapture.EXTRA_LIVENESS_DETECTION_RESULT";

    // Loader IDs
    private static final int LOADER_ID_SCORE = 458;
    private static final int LOADER_ID_CARD_FACE = 86;
    private static final int LOADER_ID_LIVE_FACE = 355;
    private static final int LOADER_ID_REGISTRATION = 123;

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

        private VerIDUser cardUser;
        private FBFace[] faces;

        public ScoreLoader(Context context, VerIDUser cardUser, FBFace[] faces) {
            super(context);
            this.cardUser = cardUser;
            this.faces = faces;
        }

        @Override
        public Float loadInBackground() {
            // Record the current security level settings
            VerID.SecurityLevel securityLevel = VerID.shared.getSecurityLevel();
            // ID card face comparison works best with the lowest security level
            VerID.shared.setSecurityLevel(VerID.SecurityLevel.LOWEST);
            float score = 0;
            try {
                score = VerID.shared.compareUserToFaces(cardUser, faces);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Restore the previous security level
            VerID.shared.setSecurityLevel(securityLevel);
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

    /**
     * Loader that registers the face on the ID card so that it can be compared to the live selfie faces
     */
    private static class RegistrationLoader extends AsyncTaskLoader<VerIDUser> {

        private FBFace face;

        public RegistrationLoader(Context context, FBFace face) {
            super(context);
            this.face = face;
        }

        @Override
        public VerIDUser loadInBackground() {
            VerIDUser user;
            try {
                user = VerID.shared.registerUserWithFace("cardUser", face, false);
            } catch (Exception e) {
                e.printStackTrace();
                user = null;
            }
            return user;
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
            if (livenessDetectionResult != null) {
                // Get the cropped face images
                getSupportLoaderManager().initLoader(LOADER_ID_CARD_FACE, intent.getExtras(), this).forceLoad();
                getSupportLoaderManager().initLoader(LOADER_ID_LIVE_FACE, intent.getExtras(), this).forceLoad();
                // Check whether the face on the captured ID card is being processed
                if (idCaptureResult.getRegisteredUser() == null && idCaptureResult.getFace().isBackgroundProcessing()) {
                    // The ID card face is being processed. Listen for face template extraction events
                    VerID.shared.getFaceTemplateExtraction().addListener(idCaptureResult.getFace().getId(), this);
                } else {
                    initScoreLoaderIfReady();
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
        getLoaderManager().destroyLoader(LOADER_ID_REGISTRATION);
        if (idCaptureResult != null && idCaptureResult.getFace() != null && idCaptureResult.getFace().isBackgroundProcessing()) {
            VerID.shared.getFaceTemplateExtraction().removeListener(idCaptureResult.getFace().getId(), this);
        }
    }

    private void initScoreLoaderIfReady() {
        if (idCaptureResult.getRegisteredUser() != null) {
            // The face on the ID card is registered, calculate the similarity score
            getSupportLoaderManager().initLoader(LOADER_ID_SCORE, null, this).forceLoad();
        } else if (idCaptureResult.getFace().isSuitableForRecognition()) {
            // Register user on the ID card
            getSupportLoaderManager().initLoader(LOADER_ID_REGISTRATION, null, this).forceLoad();
        }
    }

    @Override
    public void onFaceTemplateExtracted(long faceId, FBFace face) {
        VerID.shared.getFaceTemplateExtraction().removeListener(faceId, this);
        if (idCaptureResult.getFace() != null && idCaptureResult.getFace().isBackgroundProcessing() && idCaptureResult.getFace().getId() == faceId) {
            // Face template has been extracted from the face in the ID card. The face is now ready to be registered
            idCaptureResult.setFace(face);
            CardCaptureResultPersistence.saveCardCaptureResult(this, idCaptureResult);
            initScoreLoaderIfReady();
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
                FBFace[] faces = livenessDetectionResult.getFaces(VerID.Bearing.STRAIGHT);
                FBFace face = null;
                for (FBFace f : faces) {
                    if (f.isSuitableForRecognition()) {
                        face = f;
                        break;
                    }
                }
                if (face == null) {
                    for (FBFace f : faces) {
                        if (f.isBackgroundProcessing()) {
                            face = f;
                            break;
                        }
                    }
                }
                if (face != null) {
                    Uri faceImageUri = livenessDetectionResult.getFaceImages().get(face);
                    Rect faceImageFaceBounds = new Rect();
                    FaceUtil.getFaceBounds(face).round(faceImageFaceBounds);
                    return new ImageLoader(this, faceImageUri, faceImageFaceBounds);
                }
                return null;
            case LOADER_ID_REGISTRATION:
                if (idCaptureResult.getFace() != null && idCaptureResult.getFace().isSuitableForRecognition()) {
                    return new RegistrationLoader(this, idCaptureResult.getFace());
                } else {
                    return null;
                }
            case LOADER_ID_SCORE:
                if (idCaptureResult.getRegisteredUser() != null) {
                    return new ScoreLoader(this, idCaptureResult.getRegisteredUser(), livenessDetectionResult.getFacesSuitableForRecognition());
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
            case LOADER_ID_REGISTRATION:
                if (data != null && (data instanceof VerIDUser)) {
                    VerIDUser user = (VerIDUser) data;
                    idCaptureResult.setRegisteredUser(user);
                    CardCaptureResultPersistence.saveCardCaptureResult(this, idCaptureResult);
                    initScoreLoaderIfReady();
                } else {
                    updateScore(null);
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
