package com.appliedrec.credentials.app;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

public class WebViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        WebView webView = findViewById(R.id.webView);
        Intent intent = getIntent();
        String url = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (url != null) {
            webView.loadUrl(url);
        }
    }

}
