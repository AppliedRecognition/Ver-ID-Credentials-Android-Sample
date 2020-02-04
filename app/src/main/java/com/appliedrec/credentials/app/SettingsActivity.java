package com.appliedrec.credentials.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.appliedrec.uielements.RxVerIDActivity;

import io.reactivex.android.schedulers.AndroidSchedulers;

public class SettingsActivity extends RxVerIDActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        try {
            ProgressBar progressBar = findViewById(R.id.progressBar);
            progressBar.setVisibility(View.INVISIBLE);
            SecureStorage secureStorage = new SecureStorage(this);
            String password = secureStorage.getValueForKey(SecureStorage.CommonKeys.INTELLICHECK_API_KEY);
            EditText passwordEditText = findViewById(R.id.password);
            Button testButton = findViewById(R.id.button);
            passwordEditText.setText(password);
            testButton.setEnabled(password != null && !password.isEmpty());
            passwordEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    String password = editable.toString().trim();
                    if (password.isEmpty()) {
                        secureStorage.deleteValueForKey(SecureStorage.CommonKeys.INTELLICHECK_API_KEY);
                        testButton.setEnabled(false);
                    } else {
                        secureStorage.setValueForKey(password, SecureStorage.CommonKeys.INTELLICHECK_API_KEY);
                        testButton.setEnabled(true);
                    }
                }
            });
            testButton.setOnClickListener(button -> {
                String intellicheckPassword = passwordEditText.getText().toString().trim();
                if (intellicheckPassword.isEmpty()) {
                    return;
                }
                progressBar.setVisibility(View.VISIBLE);
                button.setVisibility(View.INVISIBLE);
                IntellicheckBarcodeVerifier barcodeVerifier = new IntellicheckBarcodeVerifier(this, intellicheckPassword);
                addDisposable(barcodeVerifier.testPassword().observeOn(AndroidSchedulers.mainThread()).subscribe(
                        () -> {
                            button.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(this, "Password OK", Toast.LENGTH_SHORT).show();
                        },
                        error -> {
                            button.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.INVISIBLE);
                            String message = error.getLocalizedMessage();
                            if (message == null || message.isEmpty()) {
                                message = "Failed to test password";
                            }
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        }
                ));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
