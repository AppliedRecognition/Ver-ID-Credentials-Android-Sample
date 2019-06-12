package com.appliedrec.idcapturesample;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.appliedrec.verid.core.VerID;
import com.appliedrec.verid.core.VerIDFactory;
import com.appliedrec.verid.core.VerIDFactoryDelegate;
import com.appliedrec.verid.credentials.IDCaptureSessionActivity;
import com.appliedrec.verid.credentials.IDDocument;
import com.appliedrec.verid.ui.VerIDSessionActivity;

public class MainActivity extends AppCompatActivity implements VerIDFactoryDelegate {

    // Activity request codes
    final static int REQUEST_CODE_CARD = 0;

    // UI Elements
    private View loadingIndicatorView;
    private Button scanIdButton;
    private VerID verID;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadingIndicatorView = findViewById(R.id.loading);
        scanIdButton = findViewById(R.id.scanIdCard);
        scanIdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the ID card scan
                Intent intent = new Intent(MainActivity.this, CardPropertiesActivity.class);
                intent.putExtra(IDCaptureSessionActivity.EXTRA_VERID_INSTANCE_ID, verID.getInstanceId());
                startActivityForResult(intent, REQUEST_CODE_CARD);
            }
        });
        // Load Ver-ID
        if (savedInstanceState != null) {
            int veridInstanceId = savedInstanceState.getInt(VerIDSessionActivity.EXTRA_VERID_INSTANCE_ID, -1);
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
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (verID != null) {
            outState.putInt(VerIDSessionActivity.EXTRA_VERID_INSTANCE_ID, verID.getInstanceId());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CARD && resultCode == RESULT_OK && data != null) {
            // Received an ID card
            IDDocument idDocument = data.getParcelableExtra(IDCaptureSessionActivity.EXTRA_DOCUMENT);
            if (idDocument != null) {
                showCapturedDocument(idDocument);
            }
        }
    }

    private void onVerIDLoaded() {
        if (!isDestroyed()) {
            loadingIndicatorView.setVisibility(View.GONE);
            scanIdButton.setVisibility(View.VISIBLE);
        }
    }

    private void showCapturedDocument(IDDocument idDocument) {
        Intent intent = new Intent(this, ViewCardActivity.class);
        intent.putExtra(VerIDSessionActivity.EXTRA_VERID_INSTANCE_ID, verID.getInstanceId());
        intent.putExtra(IDCaptureSessionActivity.EXTRA_DOCUMENT, idDocument);
        startActivity(intent);
    }

    @Override
    public void veridFactoryDidCreateEnvironment(VerIDFactory verIDFactory, VerID verID) {
        this.verID = verID;
        onVerIDLoaded();
    }

    @Override
    public void veridFactoryDidFailWithException(VerIDFactory verIDFactory, Exception e) {
        if (!isDestroyed()) {
            loadingIndicatorView.setVisibility(View.GONE);
        }
    }
}
