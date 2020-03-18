package com.appliedrec.credentials.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import com.appliedrec.credentials.app.databinding.ActivitySettingsBinding;
import com.appliedrec.rxverid.RxVerIDActivity;

import io.reactivex.android.schedulers.AndroidSchedulers;

public class SettingsActivity extends RxVerIDActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySettingsBinding viewBinding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        try {
            viewBinding.progressBar.setVisibility(View.INVISIBLE);
            SecureStorage secureStorage = new SecureStorage(this);
            String password = secureStorage.getValueForKey();
            viewBinding.password.setText(password);
            viewBinding.button.setEnabled(password != null && !password.isEmpty());
            viewBinding.password.addTextChangedListener(new TextWatcher() {
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
                        secureStorage.deleteValueForKey();
                        viewBinding.button.setEnabled(false);
                    } else {
                        secureStorage.setValueForKey(password);
                        viewBinding.button.setEnabled(true);
                    }
                }
            });
            viewBinding.button.setOnClickListener(button -> {
                String intellicheckPassword = viewBinding.password.getText().toString().trim();
                if (intellicheckPassword.isEmpty()) {
                    return;
                }
                viewBinding.progressBar.setVisibility(View.VISIBLE);
                button.setVisibility(View.INVISIBLE);
                IntellicheckBarcodeVerifier barcodeVerifier = new IntellicheckBarcodeVerifier(this, intellicheckPassword);
                addDisposable(barcodeVerifier.testPassword().observeOn(AndroidSchedulers.mainThread()).subscribe(
                        () -> {
                            button.setVisibility(View.VISIBLE);
                            viewBinding.progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(this, "Password OK", Toast.LENGTH_SHORT).show();
                        },
                        error -> {
                            button.setVisibility(View.VISIBLE);
                            viewBinding.progressBar.setVisibility(View.INVISIBLE);
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
