package com.appliedrec.idcapturesample;

import android.content.Intent;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
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

import com.appliedrec.ver_id.VerID;
import com.appliedrec.ver_id.VerIDLivenessDetectionIntent;
import com.appliedrec.ver_id.session.VerIDLivenessDetectionSessionSettings;
import com.appliedrec.ver_id.session.VerIDSessionResult;
import com.appliedrec.ver_id.ui.VerIDActivity;
import com.appliedrec.ver_ididcapture.CardOverlayView;
import com.appliedrec.ver_ididcapture.IDCaptureActivity;
import com.appliedrec.ver_ididcapture.VerIDIDCapture;
import com.appliedrec.ver_ididcapture.VerIDIDCaptureIntent;
import com.appliedrec.ver_ididcapture.VerIDIDCaptureSettings;
import com.appliedrec.ver_ididcapture.data.IDDocument;
import com.appliedrec.ver_ididcapture.data.Page;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadingIndicatorView = findViewById(R.id.loading);
        scanIdButton = findViewById(R.id.register);
        scanIdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the ID card scan
                verIDSessionResult = null;
                VerIDIDCaptureSettings settings = new VerIDIDCaptureSettings((IDDocument)null, true, true, true);
                Intent intent = new VerIDIDCaptureIntent(MainActivity.this, settings);
                startActivityForResult(intent, REQUEST_CODE_CARD);
            }
        });
        liveFaceCompareButton = findViewById(R.id.authenticate);
        liveFaceCompareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Collect a live selfie and compare it to the face on the ID card
                VerIDLivenessDetectionSessionSettings sessionSettings = new VerIDLivenessDetectionSessionSettings();
                sessionSettings.expiryTime = 120000;
                // Ask Ver-ID to extract face templates needed for face recognition
                sessionSettings.includeFaceTemplatesInResult = true;
                Intent intent = new VerIDLivenessDetectionIntent(MainActivity.this, sessionSettings);
                startActivityForResult(intent, REQUEST_CODE_FACE);
            }
        });
        cardImageView = new ImageView(this);
        cardImageView.setVisibility(View.GONE);
        scrollView = findViewById(R.id.scrollView);
        heroLayout = findViewById(com.appliedrec.ver_ididcapture.R.id.hero);
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
        VerID.shared.load(this, new VerID.LoadCallback() {
            @Override
            public void onLoad() {
                if (!isDestroyed()) {
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            if (savedInstanceState != null) {
                                verIDSessionResult = savedInstanceState.getParcelable("verIDSessionResult");
                            }
                            // Load captured ID card result from shared preferences
                            idDocument = CardCaptureResultPersistence.loadCapturedDocument(MainActivity.this);
                            // Check that the card has a face that's suitable for recognition
                            if (idDocument != null && idDocument.getFaceTemplate() == null) {
                                idDocument = null;
                                // Delete the card, it cannot be used for face recognition
                                CardCaptureResultPersistence.saveCapturedDocument(MainActivity.this, null);
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (!isDestroyed()) {
                                        setShowProgressBar(false);
                                        updateContent();
                                    }
                                }
                            });
                        }
                    });
                }
            }

            @Override
            public void onError(Exception error) {
                loadingIndicatorView.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, R.string.verid_failed_to_load, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup
        VerID.shared.unload();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (verIDSessionResult != null) {
            outState.putParcelable("verIDSessionResult", verIDSessionResult);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CARD && resultCode == RESULT_OK && data != null) {
            // Received an ID card
            idDocument = data.getParcelableExtra(IDCaptureActivity.EXTRA_ID_DOCUMENT);
            if (idDocument != null && idDocument.getFaceTemplate() != null) {
                File cardImageFile = new File(getFilesDir(), "idcard.jpg");
                for (Page page : idDocument.getPages()) {
                    if (page.getImageUri() != null) {
                        try {
                            InputStream inputStream = new FileInputStream(page.getImageUri().getPath());
                            try {
                                OutputStream outputStream = new FileOutputStream(cardImageFile);
                                try {
                                    int read;
                                    byte[] buffer = new byte[512];
                                    while ((read = inputStream.read(buffer, 0, buffer.length)) > 0) {
                                        outputStream.write(buffer, 0, read);
                                    }
                                    page.setImageUri(Uri.fromFile(cardImageFile));
                                } catch (IOException e) {
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
                // Save the card to shared preferences
                CardCaptureResultPersistence.saveCapturedDocument(this, idDocument);
            } else {
                idDocument = null;
            }
            updateContent();
        } else if (requestCode == REQUEST_CODE_FACE && resultCode == RESULT_OK && data != null) {
            // Received a selfie response
            verIDSessionResult = data.getParcelableExtra(VerIDActivity.EXTRA_SESSION_RESULT);
            if (verIDSessionResult != null) {
                if (verIDSessionResult.isPositive()) {
                    // Compare the selfie with the face on the ID card
                    compareLiveFace();
                    return;
                } else if (verIDSessionResult.outcome == VerIDSessionResult.Outcome.FAIL_NUMBER_OF_RESULTS) {
                    // No faces detected
                    Toast.makeText(this, R.string.failed_to_detect_face, Toast.LENGTH_SHORT).show();
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
            CardCaptureResultPersistence.saveCapturedDocument(this, null);
        }
    }

    private void updateContent() {
        // Enable the Compare Live Face button if we have the image of the front of the card, barcode text and selfie face
        liveFaceCompareButton.setEnabled(idDocument != null);
        cardImageView.setVisibility(idDocument != null ? View.VISIBLE : View.GONE);
        cardImageView.setImageDrawable(null);
        if (idDocument != null) {
            if (idDocument.getImageUri() != null) {
                if (Build.VERSION.SDK_INT >= 21) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(idDocument.getImageUri());
                        RoundedBitmapDrawable bitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), inputStream);
                        bitmapDrawable.setCornerRadius((float) bitmapDrawable.getIntrinsicHeight() / 20f);
                        cardImageView.setImageDrawable(bitmapDrawable);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        cardImageView.setImageURI(idDocument.getImageUri());
                    }
                } else {
                    cardImageView.setImageURI(idDocument.getImageUri());
                }
            }
            scrollView.setVisibility(View.GONE);
        } else {
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
        if (verIDSessionResult != null && verIDSessionResult.isPositive() && !verIDSessionResult.getFaceImages(VerID.Bearing.STRAIGHT).isEmpty() && idDocument != null && idDocument.getFaceTemplate() != null) {
            Intent intent = new Intent(this, CaptureResultActivity.class);
            intent.putExtra(CaptureResultActivity.EXTRA_LIVENESS_DETECTION_RESULT, verIDSessionResult);
            startActivity(intent);
        }
    }
}
