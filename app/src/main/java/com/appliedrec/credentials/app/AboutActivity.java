package com.appliedrec.credentials.app;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;

import androidx.appcompat.app.AppCompatActivity;

import com.appliedrec.verid.core.VerID;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try {
            String appVersion = "App version: "+getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            menu.add(appVersion);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        String veridVersion = "Ver-ID SDK version: "+VerID.getVersion();
        menu.add(veridVersion);
        return true;
    }
}
