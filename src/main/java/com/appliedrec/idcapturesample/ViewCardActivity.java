package com.appliedrec.idcapturesample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.appliedrec.verid.core.Bearing;
import com.appliedrec.verid.core.LivenessDetectionSessionSettings;
import com.appliedrec.verid.core.RecognizableFace;
import com.appliedrec.verid.core.VerID;
import com.appliedrec.verid.core.VerIDSessionResult;
import com.appliedrec.verid.credentials.FacePhotoPage;
import com.appliedrec.verid.credentials.ID;
import com.appliedrec.verid.credentials.IDCaptureSessionActivity;
import com.appliedrec.verid.credentials.IDDocument;
import com.appliedrec.verid.credentials.Page;
import com.appliedrec.verid.ui.VerIDSessionActivity;
import com.appliedrec.verid.ui.VerIDSessionIntent;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class ViewCardActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_LIVE_FACE = 0;
    IDDocument idDocument;
    VerID verID;
    ImageView cardImageView;
    TextView nameTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_card);
        int instanceId = getIntent().getIntExtra(VerIDSessionActivity.EXTRA_VERID_INSTANCE_ID, -1);
        try {
            verID = VerID.getInstance(instanceId);
        } catch (Exception e) {
            e.printStackTrace();
            finish();
            return;
        }
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureLiveFace();
            }
        });
        cardImageView = findViewById(R.id.cardImageView);
        nameTextView = findViewById(R.id.name);
        nameTextView.setVisibility(View.GONE);
        idDocument = getIntent().getParcelableExtra(IDCaptureSessionActivity.EXTRA_DOCUMENT);
        if (idDocument == null) {
            finish();
            return;
        }
        for (ID id : idDocument.getIDs()) {
            String name = id.getDocumentHolder().getName().toString();
            if (!name.isEmpty()) {
                nameTextView.setText(name);
                nameTextView.setVisibility(View.VISIBLE);
                break;
            }
        }
        for (final Page page : idDocument.getPages()) {
            if (page.getImageUri() == null) {
                continue;
            }
            if (page instanceof FacePhotoPage && ((FacePhotoPage) page).getFace() != null) {
                final Uri cardImageUri = page.getImageUri();
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(cardImageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            Rect cropRect = new Rect();
                            ((FacePhotoPage) page).getFace().getBounds().round(cropRect);
                            bitmap = Bitmap.createBitmap(bitmap, cropRect.left, cropRect.top, cropRect.width(), cropRect.height());
                            final RoundedBitmapDrawable bitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
                            bitmapDrawable.setCornerRadius((float) bitmapDrawable.getIntrinsicHeight() / 10f);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    cardImageView.setImageDrawable(bitmapDrawable);
                                }
                            });
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                });
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_LIVE_FACE && resultCode == RESULT_OK && data != null) {
            VerIDSessionResult result = data.getParcelableExtra(VerIDSessionActivity.EXTRA_RESULT);
            if (result == null) {
                return;
            }
            if (result.getError() != null) {
                return;
            }
            RecognizableFace[] faces = result.getFacesSuitableForRecognition(Bearing.STRAIGHT);
            if (faces.length == 0) {
                return;
            }
            final RecognizableFace cardFace = idDocument.getFaceTemplate();
            try {
                float score = verID.getFaceRecognition().compareSubjectFacesToFaces(new RecognizableFace[]{cardFace}, faces);
                Intent intent = new Intent(this, CaptureResultActivity.class);
                intent.putExtras(data);
                intent.putExtras(getIntent());
                intent.putExtra(CaptureResultActivity.EXTRA_SCORE, score);
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void captureLiveFace() {
        LivenessDetectionSessionSettings sessionSettings = new LivenessDetectionSessionSettings();
        VerIDSessionIntent<LivenessDetectionSessionSettings> intent = new VerIDSessionIntent<>(this, verID, sessionSettings);
        startActivityForResult(intent, REQUEST_CODE_LIVE_FACE);
    }
}
