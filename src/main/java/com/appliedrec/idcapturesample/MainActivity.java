package com.appliedrec.idcapturesample;

import android.content.Intent;
import android.graphics.RectF;
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
import com.appliedrec.ver_ididcapture.RegionUtil;
import com.appliedrec.ver_ididcapture.VerIDIDCaptureIntent;
import com.appliedrec.ver_ididcapture.VerIDIDCaptureSettings;
import com.appliedrec.ver_ididcapture.data.IDCaptureResult;
import com.appliedrec.ver_ididcapture.data.Region;

import org.json.JSONException;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    // Collected data
    IDCaptureResult idCaptureResult;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadingIndicatorView = findViewById(R.id.loading);
        scanIdButton = (Button) findViewById(R.id.register);
        scanIdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verIDSessionResult = null;
                VerIDIDCaptureSettings settings = new VerIDIDCaptureSettings(Region.GENERAL, true, true);
                Intent intent = new VerIDIDCaptureIntent(MainActivity.this, settings);
                startActivityForResult(intent, REQUEST_CODE_CARD);
            }
        });
        liveFaceCompareButton = (Button) findViewById(R.id.authenticate);
        liveFaceCompareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VerIDLivenessDetectionSessionSettings sessionSettings = new VerIDLivenessDetectionSessionSettings();
                sessionSettings.expiryTime = 12000;
                sessionSettings.showGuide = true;
                Intent intent = new VerIDLivenessDetectionIntent(MainActivity.this, sessionSettings);
                startActivityForResult(intent, REQUEST_CODE_FACE);
            }
        });
        cardImageView = new ImageView(this);
        cardImageView.setVisibility(View.GONE);
        scrollView = findViewById(R.id.scrollView);
        heroLayout = (FrameLayout) findViewById(com.appliedrec.ver_ididcapture.R.id.hero);
        heroLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                heroLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                RectF cardRect = CardOverlayView.cardRectForSize((float)heroLayout.getWidth(), (float)heroLayout.getHeight());
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams((int)cardRect.width(), (int)cardRect.height(), Gravity.CENTER);
                heroLayout.addView(cardImageView, 0, layoutParams);
            }
        });
        if (savedInstanceState != null) {
            // Restore variables
            idCaptureResult = savedInstanceState.getParcelable("idCaptureResult");
            verIDSessionResult = savedInstanceState.getParcelable("verIDSessionResult");
        } else {
            String idCaptureResultString = getPreferences(MODE_PRIVATE).getString("idCaptureResult", null);
            if (idCaptureResultString != null) {
                try {
                    idCaptureResult = new IDCaptureResult(idCaptureResultString);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        updateContent();
        // Load Ver-ID
        VerID.shared.load(this, new VerID.LoadCallback() {
            @Override
            public void onLoad() {
                if (!isDestroyed()) {
                    setShowProgressBar(false);
                }
            }

            @Override
            public void onError(Exception error) {
                // TODO: Show an error to the user
                loadingIndicatorView.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VerID.shared.unload();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (idCaptureResult != null) {
            outState.putParcelable("idCaptureResult", idCaptureResult);
        }
        if (verIDSessionResult != null) {
            outState.putParcelable("verIDSessionResult", verIDSessionResult);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CARD && resultCode == RESULT_OK && data != null) {
            idCaptureResult = data.getParcelableExtra(IDCaptureActivity.EXTRA_RESULT);
            if (idCaptureResult != null) {
                try {
                    String json = idCaptureResult.toJson();
                    saveCardToSharedPrefs(json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            updateContent();
        } else if (requestCode == REQUEST_CODE_FACE && resultCode == RESULT_OK && data != null) {
            // Received a selfie response
            verIDSessionResult = data.getParcelableExtra(VerIDActivity.EXTRA_SESSION_RESULT);
            if (verIDSessionResult != null) {
                if (verIDSessionResult.isPositive()) {
                    verifyCard();
                    return;
                } else if (verIDSessionResult.outcome == VerIDSessionResult.Outcome.FAIL_NUMBER_OF_RESULTS) {
                    Toast.makeText(this, R.string.failed_to_detect_face, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
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
            idCaptureResult = null;
            saveCardToSharedPrefs(null);
            updateContent();
            return true;
        }
        return false;
    }

    private void saveCardToSharedPrefs(String json) {
        if (json != null) {
            getPreferences(MODE_PRIVATE).edit().putString("idCaptureResult", json).apply();
        } else {
            getPreferences(MODE_PRIVATE).edit().remove("idCaptureResult").apply();
        }
    }

    private void updateContent() {
        // Enable the Verify ID button if we have the image of the front of the card, barcode text and selfie face
        liveFaceCompareButton.setEnabled(idCaptureResult != null);
        cardImageView.setVisibility(idCaptureResult != null ? View.VISIBLE : View.GONE);
        cardImageView.setImageDrawable(null);
        if (idCaptureResult != null) {
            if (Build.VERSION.SDK_INT >= 21) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(idCaptureResult.getFrontImageUri());
                    RoundedBitmapDrawable bitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), inputStream);
                    bitmapDrawable.setCornerRadius((float)bitmapDrawable.getIntrinsicHeight() / 20f);
                    cardImageView.setImageDrawable(bitmapDrawable);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    cardImageView.setImageURI(idCaptureResult.getFrontImageUri());
                }
            } else {
                cardImageView.setImageURI(idCaptureResult.getFrontImageUri());
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

    private void verifyCard() {
        if (verIDSessionResult != null && verIDSessionResult.isPositive() && !verIDSessionResult.getFaceImages(VerID.Bearing.STRAIGHT).isEmpty() && idCaptureResult != null && idCaptureResult.getFaceBounds() != null) {
            Intent intent = new Intent(this, CaptureResultActivity.class);
            intent.putExtra(CaptureResultActivity.EXTRA_CAPTURE_RESULT, idCaptureResult);
            intent.putExtra(CaptureResultActivity.EXTRA_LIVENESS_DETECTION_RESULT, verIDSessionResult);
            startActivity(intent);
        }
    }
}
