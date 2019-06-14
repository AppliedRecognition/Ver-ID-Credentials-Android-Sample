package com.appliedrec.idcapturesample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.appliedrec.verid.core.Bearing;
import com.appliedrec.verid.core.Face;
import com.appliedrec.verid.core.VerID;
import com.appliedrec.verid.core.VerIDSessionResult;
import com.appliedrec.verid.credentials.FacePhotoPage;
import com.appliedrec.verid.credentials.IDCaptureSessionActivity;
import com.appliedrec.verid.credentials.IDDocument;
import com.appliedrec.verid.credentials.IDFeature;
import com.appliedrec.verid.credentials.Page;
import com.appliedrec.verid.ui.VerIDSessionActivity;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

public class CaptureResultActivity extends AppCompatActivity {

    // Extra name constant
    public static final String EXTRA_SCORE = "com.appliedrec.verid.score";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_result);
        ImageView liveFaceView = findViewById(R.id.live_face);
        ImageView cardFaceView = findViewById(R.id.card_face);
        TextView resultTextView = findViewById(R.id.text);
        LikenessGaugeView likenessGaugeView = findViewById(R.id.likeness_gauge);
        float score = getIntent().getFloatExtra(EXTRA_SCORE, 0f);
        try {
            VerID verID = VerID.getInstance(getIntent().getIntExtra(VerIDSessionActivity.EXTRA_VERID_INSTANCE_ID, -1));
            likenessGaugeView.setMax(verID.getFaceRecognition().getMaxAuthenticationScore());
            likenessGaugeView.setThreshold(verID.getFaceRecognition().getAuthenticationThreshold());
            likenessGaugeView.setScore(score);
            resultTextView.setText(getResources().getString(R.string.similarity_score, score));
            VerIDSessionResult livenessDetectionResult = getIntent().getParcelableExtra(VerIDSessionActivity.EXTRA_RESULT);
            IDDocument idDocument = getIntent().getParcelableExtra(IDCaptureSessionActivity.EXTRA_DOCUMENT);
            Iterator<Map.Entry<Face,Uri>> liveFacesIterator = livenessDetectionResult.getFaceImages(Bearing.STRAIGHT).entrySet().iterator();
            if (!liveFacesIterator.hasNext()) {
                return;
            }
            Map.Entry<Face,Uri> liveFaceEntry = liveFacesIterator.next();
            setCroppedImageInImageView(liveFaceEntry.getValue(), liveFaceEntry.getKey().getBounds(), liveFaceView);
            Page[] docPages = idDocument.getPages();
            for (Page page : docPages) {
                if (page.getImageUri() == null) {
                    continue;
                }
                if (page instanceof FacePhotoPage && ((FacePhotoPage) page).getFace() != null) {
                    setCroppedImageInImageView(page.getImageUri(), ((FacePhotoPage) page).getFace().getBounds(), cardFaceView);
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setCroppedImageInImageView(final Uri imageUri, final RectF crop, final ImageView imageView) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    if (bitmap == null) {
                        return;
                    }
                    Rect cropRect = new Rect();
                    crop.round(cropRect);
                    Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, cropRect.left, cropRect.top, cropRect.width(), cropRect.height());
                    final RoundedBitmapDrawable bitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), croppedBitmap);
                    bitmapDrawable.setCornerRadius((float) bitmapDrawable.getIntrinsicHeight() / 10f);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isDestroyed()) {
                                return;
                            }
                            imageView.setImageDrawable(bitmapDrawable);
                        }
                    });
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
