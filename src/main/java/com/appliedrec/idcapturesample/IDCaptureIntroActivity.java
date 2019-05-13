package com.appliedrec.idcapturesample;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import com.appliedrec.verid.credentials.GuideFragment;
import com.appliedrec.verid.credentials.GuideFragmentListener;
import com.appliedrec.verid.credentials.IDCaptureSessionActivity;
import com.appliedrec.verid.credentials.IDCaptureSessionSettings;
import com.appliedrec.verid.credentials.IDDocument;
import com.appliedrec.verid.credentials.IGuideFragment;

public class IDCaptureIntroActivity extends AppCompatActivity implements GuideFragmentListener {

    IDCaptureSessionSettings settings;
    int veridInstanceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null) {
            settings = getIntent().getParcelableExtra(IDCaptureSessionActivity.EXTRA_SETTINGS);
            veridInstanceId = getIntent().getIntExtra(IDCaptureSessionActivity.EXTRA_VERID_INSTANCE_ID, -1);
        }
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.container, GuideFragment.newInstance(settings.document)).commit();
        }
    }

//    @Override
//    protected void onLeftButtonClick() {
//
//    }
//
//    @Override
//    protected void onRightButtonClick() {
//        if (settings == null) {
//            settings = new IDCaptureSessionSettings((IDDocument)null, false, false, true);
//        }
//        try {
//            Intent intent = new VerIDIDCaptureIntent(this, VerID.getInstance(veridInstanceId), settings);
//            intent.putExtras(getIntent());
//            startActivityForResult(intent, 0);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    protected int getImageResourceId() {
//        return R.mipmap.id_card;
//    }
//
//    @Override
//    protected String getText() {
//        return getString(R.string.capture_intro);
//    }

    @Override
    public void onStartCapturingDocument(Fragment guideFragment, IDDocument document) {

    }

    @Override
    public void onGuideFragmentCancel(Fragment guideFragment) {

    }
}
