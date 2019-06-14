package com.appliedrec.idcapturesample;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Switch;

import com.appliedrec.verid.core.VerID;
import com.appliedrec.verid.credentials.BarcodePage;
import com.appliedrec.verid.credentials.CardFormat;
import com.appliedrec.verid.credentials.FacePhotoPage;
import com.appliedrec.verid.credentials.IDCaptureSessionActivity;
import com.appliedrec.verid.credentials.IDCaptureSessionSettings;
import com.appliedrec.verid.credentials.IDDocument;
import com.appliedrec.verid.credentials.IDFeature;
import com.appliedrec.verid.credentials.Page;
import com.appliedrec.verid.credentials.VerIDIDCaptureIntent;

public class CardPropertiesActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_ID_CAPTURE = 0;
    Switch barcodePageSwitch;
    VerID verID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_properties);
        barcodePageSwitch = findViewById(R.id.barcodePageSwitch);
        Intent intent = getIntent();
        if (intent != null) {
            try {
                verID = VerID.getInstance(intent.getIntExtra(IDCaptureSessionActivity.EXTRA_VERID_INSTANCE_ID, -1));
                invalidateOptionsMenu();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.card_properties, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_continue).setEnabled(verID != null);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_continue) {
            startIDCaptureSession();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ID_CAPTURE) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK, data);
                finish();
            }
        }
    }

    private void startIDCaptureSession() {
        Page[] pages = new Page[barcodePageSwitch.isChecked() ? 2 : 1];
        pages[0] = new FacePhotoPage(CardFormat.ID1);
        if (barcodePageSwitch.isChecked()) {
            pages[1] = new BarcodePage(CardFormat.ID1, BarcodePage.BarcodeFormat.PDF417);
        }
        IDCaptureSessionSettings settings = new IDCaptureSessionSettings(new IDDocument(pages), false, true);
        Intent intent = new VerIDIDCaptureIntent(this, verID, settings);
        startActivityForResult(intent, REQUEST_CODE_ID_CAPTURE);
    }
}
