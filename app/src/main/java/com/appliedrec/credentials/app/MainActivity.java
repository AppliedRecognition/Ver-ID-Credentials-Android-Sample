package com.appliedrec.credentials.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.Group;

import com.appliedrec.uielements.RxVerIDActivity;
import com.appliedrec.verid.core.Bearing;
import com.appliedrec.verid.core.DetectedFace;
import com.microblink.entities.recognizers.Recognizer;
import com.microblink.entities.recognizers.RecognizerBundle;
import com.microblink.entities.recognizers.blinkid.DataMatchResult;
import com.microblink.entities.recognizers.blinkid.generic.BlinkIdCombinedRecognizer;
import com.microblink.uisettings.ActivityRunner;
import com.microblink.uisettings.BlinkIdUISettings;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends RxVerIDActivity {

    private static final int REQUEST_CODE_SCAN_ID_CARD = 1;
    private static final int REQUEST_CODE_SHOW_SUPPORTED_DOCUMENTS = 2;
    private RecognizerBundle recognizerBundle;
    private ProgressBar progressBar;
    private Group mainUIGroup;
    private DocumentData documentData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.button).setOnClickListener(v -> scanIDCard());
        progressBar = findViewById(R.id.progressBar);
        mainUIGroup = findViewById(R.id.mainUI);
        progressBar.setVisibility(View.VISIBLE);
        mainUIGroup.setVisibility(View.INVISIBLE);
        BlinkLicenceKeyUpdater licenceKeyUpdater = new BlinkLicenceKeyUpdater(this);
        addDisposable(licenceKeyUpdater
                .getSavedLicenceKey()
                .flatMapCompletable(licenceKeyUpdater::setLicenceKey)
                .onErrorResumeNext(error -> licenceKeyUpdater.deleteSavedLicenceKey()
                        .andThen(licenceKeyUpdater.getSavedLicenceKey())
                        .flatMapCompletable(licenceKeyUpdater::setLicenceKey))
                .onErrorResumeNext(error -> licenceKeyUpdater
                        .getLicenceKeyFromRemote()
                        .flatMapCompletable(licenceKeyUpdater::setLicenceKey))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            progressBar.setVisibility(View.GONE);
                            mainUIGroup.setVisibility(View.VISIBLE);
                        },
                        error -> {
                            progressBar.setVisibility(View.GONE);
                            new AlertDialog.Builder(this)
                                    .setTitle(R.string.invalid_mb_key)
                                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> finish())
                                    .setCancelable(false)
                                    .create()
                                    .show();
                        }
                ));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                showAbout();
                return true;
            case R.id.settings:
                showSettings();
                return true;
            case R.id.supported_documents:
                showSupportedDocuments();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCardFromResult(BlinkIdCombinedRecognizer.Result result) {
        byte[] frontImage = result.getEncodedFrontFullDocumentImage();
        documentData = new DocumentData(result);
        addDisposable(Single.create(single -> {
            try {
                File imageFile = new File(getFilesDir(), "cardFront.jpg");
                FileOutputStream outputStream = new FileOutputStream(imageFile);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(frontImage);
                int read;
                byte[] buffer = new byte[512];
                while ((read = inputStream.read(buffer, 0, buffer.length)) > 0) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.close();
                inputStream.close();
                single.onSuccess(Uri.fromFile(imageFile));
            } catch (Exception e) {
                single.onError(e);
            }
        }).cast(Uri.class)
                .flatMapObservable(imageUri -> getRxVerID().detectRecognizableFacesInImage(imageUri, 1).map(face -> new DetectedFace(face, Bearing.STRAIGHT, imageUri)))
                .singleOrError()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::showCard,
                        this::showError
                ));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN_ID_CARD && resultCode == RESULT_OK && data != null) {
            progressBar.setVisibility(View.VISIBLE);
            mainUIGroup.setVisibility(View.INVISIBLE);
            recognizerBundle.loadFromIntent(data);
            Recognizer firstRecognizer = recognizerBundle.getRecognizers()[0];
            documentData = null;
            if (firstRecognizer instanceof BlinkIdCombinedRecognizer) {
                BlinkIdCombinedRecognizer.Result result = ((BlinkIdCombinedRecognizer)firstRecognizer).getResult();
                if (result.getDocumentDataMatch() == DataMatchResult.Failed) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.invalid_licence)
                            .setMessage(R.string.front_and_back_dont_match)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(R.string.proceed_anyway, (dialogInterface, i) -> showCardFromResult(result))
                            .create()
                            .show();
                } else {
                    showCardFromResult(result);
                }
            }
        } else if (requestCode == REQUEST_CODE_SHOW_SUPPORTED_DOCUMENTS) {
            scanIDCard();
        }
    }

    private void showSupportedDocuments() {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(Intent.EXTRA_TEXT, BuildConfig.BLINK_SUPPORTED_DOCUMENTS_URL);
        startActivityForResult(intent, REQUEST_CODE_SHOW_SUPPORTED_DOCUMENTS);
    }

    private void scanIDCard() {
        BlinkIdCombinedRecognizer recognizer = new BlinkIdCombinedRecognizer();
        recognizer.setReturnFullDocumentImage(true);
        recognizer.setEncodeFullDocumentImage(true);
        recognizerBundle = new RecognizerBundle(recognizer);
        BlinkIdUISettings uiSettings = new BlinkIdUISettings(recognizerBundle);
        uiSettings.enableHighResSuccessFrameCapture(true);
        ActivityRunner.startActivityForResult(this, REQUEST_CODE_SCAN_ID_CARD, uiSettings);
    }

    private void showError(Throwable error) {
        progressBar.setVisibility(View.GONE);
        mainUIGroup.setVisibility(View.VISIBLE);
        new AlertDialog.Builder(this)
                .setTitle(R.string.error)
                .setMessage(error.getLocalizedMessage())
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }

    private void showCard(DetectedFace face) {
        progressBar.setVisibility(View.GONE);
        mainUIGroup.setVisibility(View.VISIBLE);
        Intent intent = new Intent(this, IDCardActivity.class);
        intent.putExtra(IDCardActivity.EXTRA_DETECTED_FACE, face);
        if (documentData != null) {
            intent.putExtra(IDCardActivity.EXTRA_DOCUMENT_DATA, documentData);
        }
        startActivity(intent);
    }

    private void showAbout() {
        startActivity(new Intent(this, AboutActivity.class));
    }

    private void showSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }
}
