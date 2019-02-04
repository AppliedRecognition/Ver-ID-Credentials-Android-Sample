package com.appliedrec.idcapturesample;

import android.content.Intent;
import android.os.Bundle;

import com.appliedrec.ver_ididcapture.GuideActivity;
import com.appliedrec.ver_ididcapture.IDCaptureActivity;
import com.appliedrec.ver_ididcapture.VerIDIDCaptureIntent;
import com.appliedrec.ver_ididcapture.VerIDIDCaptureSettings;
import com.appliedrec.ver_ididcapture.data.IDDocument;

public class IDCaptureIntroActivity extends GuideActivity {

    VerIDIDCaptureSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null) {
            settings = getIntent().getParcelableExtra(IDCaptureActivity.EXTRA_SETTINGS);
        }
    }

    @Override
    protected void onLeftButtonClick() {

    }

    @Override
    protected void onRightButtonClick() {
        if (settings == null) {
            settings = new VerIDIDCaptureSettings((IDDocument)null, false, false, true);
        }
        Intent intent = new VerIDIDCaptureIntent(this, settings);
        intent.putExtras(getIntent());
        startActivityForResult(intent, 0);
    }

    @Override
    protected int getImageResourceId() {
        return com.appliedrec.ver_ididcapture.R.mipmap.id_card;
    }

    @Override
    protected String getText() {
        return getString(com.appliedrec.ver_ididcapture.R.string.capture_intro);
    }
}
