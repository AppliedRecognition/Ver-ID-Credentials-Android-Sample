package com.appliedrec.credentials.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Pair;
import androidx.exifinterface.media.ExifInterface;

import com.appliedrec.barcodedatamatcher.DocumentFrontPageData;
import com.appliedrec.credentials.app.databinding.ActivityMainBinding;
import com.appliedrec.verid.core2.Classifier;
import com.appliedrec.verid.core2.Face;
import com.appliedrec.verid.core2.FaceDetection;
import com.appliedrec.verid.core2.FaceDetectionImage;
import com.appliedrec.verid.core2.IFaceDetection;
import com.appliedrec.verid.core2.RecognizableFace;
import com.appliedrec.verid.core2.VerIDImageBitmap;
import com.microblink.entities.recognizers.Recognizer;
import com.microblink.entities.recognizers.RecognizerBundle;
import com.microblink.entities.recognizers.blinkid.DataMatchResult;
import com.microblink.entities.recognizers.blinkid.generic.BlinkIdCombinedRecognizer;
import com.microblink.entities.recognizers.blinkid.generic.viz.VizResult;
import com.microblink.uisettings.ActivityRunner;
import com.microblink.uisettings.BlinkIdUISettings;

import java.util.Date;
import java.util.GregorianCalendar;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivity extends BaseActivity {

    private static final int REQUEST_CODE_SCAN_ID_CARD = 1;
    private RecognizerBundle recognizerBundle;
    private ActivityMainBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        viewBinding.button.setOnClickListener(v -> scanIDCard());
        viewBinding.progressBar.setVisibility(View.VISIBLE);
        viewBinding.mainUI.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewBinding = null;
    }

    @Override
    public void onVerIDPropertiesAvailable() {
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
                            viewBinding.progressBar.setVisibility(View.GONE);
                            viewBinding.mainUI.setVisibility(View.VISIBLE);
                        },
                        error -> {
                            viewBinding.progressBar.setVisibility(View.GONE);
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
        if (item.getItemId() == R.id.about) {
            showAbout();
        } else if (item.getItemId() == R.id.settings) {
            showSettings();
        } else if (item.getItemId() == R.id.supported_documents) {
            showSupportedDocuments();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void showCardFromResult(BlinkIdCombinedRecognizer.Result result) {
        byte[] frontImage = result.getEncodedFrontFullDocumentImage();
        @SuppressLint("CheckResult")
        Single<Pair<FaceWithImage,Float>> single = Single.<Bitmap>create(emitter -> {
            try {
                if (frontImage.length == 0) {
                    throw new Exception(getString(R.string.failed_to_collect_card_image));
                }
                Bitmap frontImageBitmap = BitmapFactory.decodeByteArray(frontImage, 0, frontImage.length);
                emitter.onSuccess(frontImageBitmap);
            } catch (Exception e) {
                emitter.onError(e);
            }
        }).flatMap(bitmap -> emitter -> {
            try {
                VerIDImageBitmap image = new VerIDImageBitmap(bitmap, ExifInterface.ORIENTATION_NORMAL);
                Face[] faces = getVerID().getFaceDetection().detectFacesInImage(image.createFaceDetectionImage(), 1, 0);
                if (faces.length > 0) {
                    RecognizableFace[] recognizableFaces = getVerID().getFaceRecognition().createRecognizableFacesFromFaces(faces, image);
                    Float authenticityScore = null;
                    try {
                        if (AuthenticityScoreSupport.defaultInstance().isDocumentSupported(result)) {
                            IFaceDetection<FaceDetectionImage> faceDetection = getVerID().getFaceDetection();
                            FaceDetection veridFaceDetection = (FaceDetection)faceDetection;
                            Classifier[] authenticityClassifiers = AuthenticityScoreSupport.defaultInstance().getClassifiers(this);
                            for (Classifier classifier : authenticityClassifiers) {
                                float score = veridFaceDetection.extractAttributeFromFace(faces[0], image, classifier.getName());
                                if (authenticityScore == null || score > authenticityScore) {
                                    authenticityScore = score;
                                }
                            }
                        }
                    } catch (Exception ignore) {}
                    emitter.onSuccess(new FaceWithImage(recognizableFaces[0], bitmap, authenticityScore));
                } else {
                    throw new Exception(getString(R.string.failed_to_detect_face_in_card));
                }
            } catch (Exception e) {
                emitter.onError(e);
            }
        }).zipWith(new FrontBackMatcher().getFrontBackMatchScore(result).onErrorReturn(error -> null), (faceWithImage, frontBackMatchScore) -> new Pair<>((FaceWithImage) faceWithImage, frontBackMatchScore));
        addDisposable(single.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                faceCapture -> showCard(faceCapture.first, new DocumentData(result), faceCapture.second),
                this::showError
        ));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN_ID_CARD && resultCode == RESULT_OK && data != null) {
            viewBinding.progressBar.setVisibility(View.VISIBLE);
            viewBinding.mainUI.setVisibility(View.INVISIBLE);
            recognizerBundle.loadFromIntent(data);
            Recognizer<?> firstRecognizer = recognizerBundle.getRecognizers()[0];
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
        }
    }

    private void showSupportedDocuments() {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(Intent.EXTRA_TEXT, BuildConfig.BLINK_SUPPORTED_DOCUMENTS_URL);
        startActivity(intent);
    }

    private void scanIDCard() {
        BlinkIdCombinedRecognizer recognizer = new BlinkIdCombinedRecognizer();
        recognizer.setReturnFullDocumentImage(true);
        recognizer.setEncodeFullDocumentImage(true);
        recognizer.setReturnFaceImage(true);
        recognizer.setEncodeFaceImage(true);
        recognizerBundle = new RecognizerBundle(recognizer);
        BlinkIdUISettings uiSettings = new BlinkIdUISettings(recognizerBundle);
        uiSettings.enableHighResSuccessFrameCapture(true);
        ActivityRunner.startActivityForResult(this, REQUEST_CODE_SCAN_ID_CARD, uiSettings);
    }

    private void showError(Throwable error) {
        viewBinding.progressBar.setVisibility(View.GONE);
        viewBinding.mainUI.setVisibility(View.VISIBLE);
        String message = error.getLocalizedMessage();
        if (error.getCause() != null && error.getCause().getLocalizedMessage() != null && !error.getCause().getLocalizedMessage().isEmpty()) {
            message += ": "+error.getCause().getLocalizedMessage();
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.error)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }

    private void showCard(FaceWithImage faceWithImage, DocumentData documentData, Float frontBackMatchScore) {
        viewBinding.progressBar.setVisibility(View.GONE);
        viewBinding.mainUI.setVisibility(View.VISIBLE);
        try {
            getSharedData().setSharedObject(IDCardActivity.EXTRA_FACE_IMAGE, faceWithImage);
            getSharedData().setSharedObject(IDCardActivity.EXTRA_DOCUMENT_DATA, documentData);
            Intent intent = new Intent(this, IDCardActivity.class);
            if (frontBackMatchScore != null) {
                intent.putExtra(IDCardActivity.EXTRA_FRONT_BACK_MATCH_SCORE, frontBackMatchScore);
            }
            startActivity(intent);
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.error)
                    .setMessage("Failed to save captured ID card")
                    .setPositiveButton(android.R.string.ok, null)
                    .create()
                    .show();
        }
    }

    private void showAbout() {
        startActivity(new Intent(this, AboutActivity.class));
    }

    private void showSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }
}
