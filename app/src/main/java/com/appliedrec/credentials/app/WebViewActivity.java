package com.appliedrec.credentials.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.appliedrec.credentials.app.databinding.ActivityWebViewBinding;

public class WebViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityWebViewBinding viewBinding = ActivityWebViewBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        Intent intent = getIntent();
        String url = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (url != null) {
            viewBinding.webView.loadUrl(url);
        }
    }

}
