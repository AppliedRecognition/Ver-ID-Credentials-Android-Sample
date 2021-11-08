package com.appliedrec.credentials.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TextViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_view);
        TextView textView = findViewById(R.id.textView);
        String text = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        textView.setText(text);
        setTitle(getIntent().getStringExtra(Intent.EXTRA_TITLE));
    }
}