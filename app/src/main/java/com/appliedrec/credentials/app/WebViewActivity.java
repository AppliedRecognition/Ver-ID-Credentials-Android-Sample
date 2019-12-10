package com.appliedrec.credentials.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

public class WebViewActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        webView = findViewById(R.id.webView);
        Intent intent = getIntent();
        String url = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (url != null) {
            webView.loadUrl(url);
        }
    }

    void loadUrl(String url) {
        webView.loadUrl(url);
    }
}
