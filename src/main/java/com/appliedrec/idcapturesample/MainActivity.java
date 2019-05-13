package com.appliedrec.idcapturesample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.appliedrec.verid.core.Bearing;
import com.appliedrec.verid.core.LivenessDetectionSessionSettings;
import com.appliedrec.verid.core.VerID;
import com.appliedrec.verid.core.VerIDFactory;
import com.appliedrec.verid.core.VerIDFactoryDelegate;
import com.appliedrec.verid.core.VerIDSessionResult;
import com.appliedrec.verid.credentials.CardOverlayView;
import com.appliedrec.verid.credentials.FacePhotoFeature;
import com.appliedrec.verid.credentials.IDCaptureSessionActivity;
import com.appliedrec.verid.credentials.IDDocument;
import com.appliedrec.verid.credentials.IDFeature;
import com.appliedrec.verid.credentials.Page;
import com.appliedrec.verid.ui.VerIDSessionActivity;
import com.appliedrec.verid.ui.VerIDSessionIntent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity implements VerIDFactoryDelegate {

    // Collected data
    IDDocument idDocument;
    VerIDSessionResult verIDSessionResult;

    // Activity request codes
    final static int REQUEST_CODE_CARD = 0;
    final static int REQUEST_CODE_FACE = 1;

    // UI Elements
    private View loadingIndicatorView;
    private Button scanIdButton;
    private Button liveFaceCompareButton;
    private ImageView cardImageView;
    private View scrollView;
    private FrameLayout heroLayout;
    private VerID verID;
    private Uri cardImageUri;
    private File defaultCardImageFile;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        defaultCardImageFile = new File(getFilesDir(), "idcard.jpg");
        loadingIndicatorView = findViewById(R.id.loading);
        scanIdButton = findViewById(R.id.register);
        scanIdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the ID card scan
                verIDSessionResult = null;
                Intent intent = new Intent(MainActivity.this, CardPropertiesActivity.class);
                intent.putExtra(IDCaptureSessionActivity.EXTRA_VERID_INSTANCE_ID, verID.getInstanceId());
                startActivityForResult(intent, REQUEST_CODE_CARD);
            }
        });
        liveFaceCompareButton = findViewById(R.id.authenticate);
        liveFaceCompareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Collect a live selfie and compare it to the face on the ID card
                LivenessDetectionSessionSettings sessionSettings = new LivenessDetectionSessionSettings();
                sessionSettings.setExpiryTime(120000);
                // Ask Ver-ID to extract face templates needed for face recognition
                sessionSettings.setIncludeFaceTemplatesInResult(true);
                Intent intent = new VerIDSessionIntent<>(MainActivity.this, verID, sessionSettings);
                startActivityForResult(intent, REQUEST_CODE_FACE);
            }
        });
        cardImageView = new ImageView(this);
        cardImageView.setVisibility(View.GONE);
        scrollView = findViewById(R.id.scrollView);
        heroLayout = findViewById(R.id.hero);
        heroLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                heroLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                RectF cardRect = CardOverlayView.cardRectForSize((float)heroLayout.getWidth(), (float)heroLayout.getHeight());
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams((int)cardRect.width(), (int)cardRect.height(), Gravity.CENTER);
                heroLayout.addView(cardImageView, 0, layoutParams);
            }
        });
        // Load Ver-ID
        if (savedInstanceState != null) {
            int veridInstanceId = savedInstanceState.getInt("veridInstanceId", -1);
            if (veridInstanceId > -1) {
                try {
                    verID = VerID.getInstance(veridInstanceId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (verID == null) {
            VerIDFactory verIDFactory = new VerIDFactory(this, this);
            verIDFactory.createVerID();
        } else {
            onVerIDLoaded();
        }
//        VerID.shared.load(this, new VerID.LoadCallback() {
//            @Override
//            public void onLoad() {
//                if (!isDestroyed()) {
//                    AsyncTask.execute(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (savedInstanceState != null) {
//                                verIDSessionResult = savedInstanceState.getParcelable("verIDSessionResult");
//                            }
//                            // Load captured ID card result from shared preferences
//                            idDocument = CardCaptureResultPersistence.loadCapturedDocument(MainActivity.this);
//                            // Check that the card has a face that's suitable for recognition
//                            if (idDocument != null && idDocument.getFaceTemplate() == null) {
//                                idDocument = null;
//                                // Delete the card, it cannot be used for face recognition
//                                CardCaptureResultPersistence.saveCapturedDocument(MainActivity.this, null);
//                            }
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    if (!isDestroyed()) {
//                                        setShowProgressBar(false);
//                                        updateContent();
//                                    }
//                                }
//                            });
//                        }
//                    });
//                }
//            }
//
//            @Override
//            public void onError(Exception error) {
//                loadingIndicatorView.setVisibility(View.GONE);
//                Toast.makeText(MainActivity.this, R.string.verid_failed_to_load, Toast.LENGTH_SHORT).show();
//            }
//        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (verIDSessionResult != null) {
            outState.putParcelable("verIDSessionResult", verIDSessionResult);
        }
        if (verID != null) {
            outState.putLong("veridInstanceId", verID.getInstanceId());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CARD && resultCode == RESULT_OK && data != null) {
            // Received an ID card
            idDocument = data.getParcelableExtra(IDCaptureSessionActivity.EXTRA_DOCUMENT);
            if (idDocument != null) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        final File cardImageFile = defaultCardImageFile;
                        for (Page page : idDocument.getPages()) {
                            if (page.getImageUri() != null) {
                                try {
                                    InputStream inputStream = getContentResolver().openInputStream(page.getImageUri());
                                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                    for (IDFeature feature : page.getFeatures()) {
                                        if (feature instanceof FacePhotoFeature) {
                                            Rect bounds = new Rect();
                                            feature.getBounds().round(bounds);
                                            // Crop image to face
                                            bitmap = Bitmap.createBitmap(bitmap, bounds.left, bounds.top, bounds.width(), bounds.height());
                                            break;
                                        }
                                    }
                                    try {
                                        OutputStream outputStream = new FileOutputStream(cardImageFile);
                                        try {
                                            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    cardImageUri = Uri.fromFile(cardImageFile);
                                                    updateContent();
                                                }
                                            });
                                        } catch (Exception e) {
                                        } finally {
                                            outputStream.close();
                                        }
                                    } catch (IOException e) {
                                    } finally {
                                        inputStream.close();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                });
                // Save the card to shared preferences
                CardCaptureResultPersistence.saveCapturedDocument(this, idDocument);
            } else {
                idDocument = null;
                cardImageUri = null;
                updateContent();
            }
        } else if (requestCode == REQUEST_CODE_FACE && resultCode == RESULT_OK && data != null) {
            // Received a selfie response
            verIDSessionResult = data.getParcelableExtra(VerIDSessionActivity.EXTRA_RESULT);
            if (verIDSessionResult != null) {
                if (verIDSessionResult.getError() == null) {
                    // Compare the selfie with the face on the ID card
                    compareLiveFace();
                    return;
                }
            }
            // Selfie capture failed
            Toast.makeText(this, R.string.live_face_capture_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete_card) {
            deleteIdCaptureResult();
            updateContent();
            return true;
        }
        return false;
    }

    private void deleteIdCaptureResult() {
        if (idDocument != null) {
            idDocument = null;
            cardImageUri = null;
            CardCaptureResultPersistence.saveCapturedDocument(this, null);
        }
    }

    private void updateContent() {
        // Enable the Compare Live Face button if we have the image of the front of the card, barcode text and selfie face
        liveFaceCompareButton.setEnabled(idDocument != null);
        cardImageView.setVisibility(idDocument != null ? View.VISIBLE : View.GONE);
        if (idDocument != null && cardImageUri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(cardImageUri);
                RoundedBitmapDrawable bitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), inputStream);
                bitmapDrawable.setCornerRadius((float) bitmapDrawable.getIntrinsicHeight() / 20f);
                cardImageView.setImageDrawable(bitmapDrawable);
                scrollView.setVisibility(View.GONE);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                idDocument = null;
                cardImageUri = null;
                updateContent();
            }
        } else {
            cardImageView.setImageDrawable(null);
            scrollView.setVisibility(View.VISIBLE);
        }
    }

    private void setShowProgressBar(boolean show) {
        loadingIndicatorView.setVisibility(show ? View.VISIBLE : View.GONE);
        scanIdButton.setVisibility(show ? View.GONE : View.VISIBLE);
        liveFaceCompareButton.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void compareLiveFace() {
        // Ensure we have a valid ID capture result to compare the selfie to
        if (verID != null && verIDSessionResult != null && verIDSessionResult.getError() == null && verIDSessionResult.getFacesSuitableForRecognition(Bearing.STRAIGHT).length > 0 && idDocument != null && idDocument.getFaceTemplate() != null) {
            Intent intent = new Intent(this, CaptureResultActivity.class);
            intent.putExtra(CaptureResultActivity.EXTRA_LIVENESS_DETECTION_RESULT, verIDSessionResult);
            intent.putExtra(VerIDSessionActivity.EXTRA_VERID_INSTANCE_ID, verID.getInstanceId());
            startActivity(intent);
        }
    }

    private void onVerIDLoaded() {
        if (!isDestroyed()) {
            setShowProgressBar(false);
            CardCaptureResultPersistence.loadCapturedDocument(this, new CardCaptureResultPersistence.LoadCallback() {
                @Override
                public void onLoadDocument(IDDocument document) {
                    idDocument = document;
                    if (document != null) {
                        cardImageUri = Uri.fromFile(defaultCardImageFile);
                    }
                    updateContent();
                }
            });
        }
    }

    @Override
    public void veridFactoryDidCreateEnvironment(VerIDFactory verIDFactory, VerID verID) {
        this.verID = verID;
        onVerIDLoaded();
    }

    @Override
    public void veridFactoryDidFailWithException(VerIDFactory verIDFactory, Exception e) {

    }
}
