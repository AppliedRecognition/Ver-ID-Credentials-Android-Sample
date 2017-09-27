package com.appliedrec.idcapturesample;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.appliedrec.ver_id.VerID;
import com.appliedrec.ver_id.session.VerIDSessionResult;
import com.appliedrec.ver_id.util.FaceUtil;
import com.appliedrec.ver_ididcapture.data.IDCaptureResult;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class CaptureResultActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks {

    // Extra name constants
    public static final String EXTRA_CAPTURE_RESULT = "com.appliedrec.ver_ididcapture.EXTRA_CAPTURE_RESULT";
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

    // Likeness view animator
    private ObjectAnimator likenessGaugeAnimator;

    // Extracted card and face images
    private Bitmap cardFaceImage;
    private Bitmap liveFaceImage;

    /**
     * Loader that compares the face from the card with the live face
     */
    private static class ScoreLoader extends AsyncTaskLoader<Float> {

        private final Bitmap image1;
        private final Bitmap image2;

        public ScoreLoader(Context context, Bitmap image1, Bitmap image2) {
            super(context);
            this.image1 = image1;
            this.image2 = image2;
        }

        @Override
        public Float loadInBackground() {
            try {
                double score = VerID.shared.compareFacesInImages(image1, image2);
                return (float) score;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
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
        Intent intent = getIntent();
        if (intent != null) {
            // Get the cropped face images
            getSupportLoaderManager().initLoader(LOADER_ID_CARD_FACE, intent.getExtras(), this).forceLoad();
            getSupportLoaderManager().initLoader(LOADER_ID_LIVE_FACE, intent.getExtras(), this).forceLoad();
        }
        likenessGaugeAnimator = ObjectAnimator.ofFloat(likenessGaugeView, "score", new float[]{0f,0.4f});
        likenessGaugeAnimator.setDuration(8000);
        likenessGaugeAnimator.setRepeatCount(Integer.MAX_VALUE);
        likenessGaugeAnimator.setRepeatMode(ValueAnimator.REVERSE);
        likenessGaugeAnimator.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getLoaderManager().destroyLoader(LOADER_ID_CARD_FACE);
        getLoaderManager().destroyLoader(LOADER_ID_LIVE_FACE);
        getLoaderManager().destroyLoader(LOADER_ID_SCORE);
    }

    private void initScoreLoader() {
        if (cardFaceImage != null && liveFaceImage != null) {
            getSupportLoaderManager().initLoader(LOADER_ID_SCORE, null, this).forceLoad();
        }
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_CARD_FACE:
                IDCaptureResult idCaptureResult = args.getParcelable(EXTRA_CAPTURE_RESULT);
                Uri cardImageUri = idCaptureResult.getFrontImageUri();
                Rect cardImageFaceBounds = new Rect();
                idCaptureResult.getFaceBounds().round(cardImageFaceBounds);
                return new ImageLoader(this, cardImageUri, cardImageFaceBounds);
            case LOADER_ID_LIVE_FACE:
                VerIDSessionResult livenessDetectionResult = args.getParcelable(EXTRA_LIVENESS_DETECTION_RESULT);
                Uri faceImageUri = livenessDetectionResult.getImageUris(VerID.Bearing.STRAIGHT)[0];
                Rect faceImageFaceBounds = livenessDetectionResult.getFaceBounds(VerID.Bearing.STRAIGHT)[0];
                return new ImageLoader(this, faceImageUri, faceImageFaceBounds);
            case LOADER_ID_SCORE:
                if (liveFaceImage != null && cardFaceImage != null) {
                    return new ScoreLoader(this, liveFaceImage, cardFaceImage);
                } else {
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
                    initScoreLoader();
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
                    initScoreLoader();
                }
                break;
            case LOADER_ID_SCORE:
                Float score = (Float) data;
                likenessGaugeAnimator.cancel();
                progressIndicatorView.setVisibility(View.GONE);
                scoreTextView.setVisibility(View.VISIBLE);
                if (score != null) {
                    scoreTextView.setText(String.format("%.0f", score.floatValue()));
                    resultTextView.setText("");
                } else {
                    score = 0f;
                    scoreTextView.setText("?");
                    resultTextView.setText(R.string.face_score_error);
                }
                ObjectAnimator.ofFloat(likenessGaugeView, "score", new float[]{likenessGaugeView.getScore(), score.floatValue() / 100f}).setDuration(500).start();
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
}
