package com.appliedrec.credentials.app;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Parcel;

import androidx.exifinterface.media.ExifInterface;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.platform.app.InstrumentationRegistry;

import com.appliedrec.uielements.facecomparison.ResultActivity;
import com.appliedrec.verid.core.Bearing;
import com.appliedrec.verid.core.DetectedFace;
import com.appliedrec.verid.core.Face;
import com.appliedrec.verid.core.FaceDetectionException;
import com.appliedrec.verid.core.RecognizableFace;
import com.appliedrec.verid.core.VerID;
import com.appliedrec.verid.core.VerIDFactory;
import com.appliedrec.verid.core.VerIDImage;
import com.appliedrec.verid.core.VerIDSessionResult;
import com.appliedrec.verid.ui.VerIDSessionActivity;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.getIntents;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class AppTests {

    private File idCardTempFile;
    private File selfieTempFile;
    private VerID verID;

    @After
    public void cleanup() {
        if (idCardTempFile != null) {
            idCardTempFile.delete();
            idCardTempFile = null;
        }
        if (selfieTempFile != null) {
            selfieTempFile.delete();
            selfieTempFile = null;
        }
    }

    @Rule
    public IntentsTestRule<IDCardActivity> idCardActivityIntentsTestRule = new IntentsTestRule<IDCardActivity>(IDCardActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), IDCardActivity.class);
            try {
                Uri cardImageUri = getCardImageUri();
                intent.putExtra(IDCardActivity.EXTRA_CARD_IMAGE_URI, cardImageUri);
                VerIDImage verIDImage = getImageFromUri(cardImageUri);
                Face[] faces = getVerID().getFaceDetection().detectFacesInImage(verIDImage, 1, 0);
                if (faces.length == 0) {
                    throw new FaceDetectionException(FaceDetectionException.Type.FACE_NOT_FOUND, null);
                }
                RecognizableFace[] recognizableFaces = getVerID().getFaceRecognition().createRecognizableFacesFromFaces(faces, verIDImage);
                intent.putExtra(IDCardActivity.EXTRA_DETECTED_FACE, new DetectedFace(recognizableFaces[0], Bearing.STRAIGHT, cardImageUri));
            } catch (Exception e) {
                fail();
            }
            return intent;
        }
    };

    @Test
    public void testFaceComparison_returnsExpectedScore() throws Exception {
        Intent data = new Intent();
        VerIDSessionResult sessionResult = getSessionResult(null);
        data.putExtra(VerIDSessionActivity.EXTRA_RESULT, sessionResult);
        Instrumentation.ActivityResult activityResult = new Instrumentation.ActivityResult(Activity.RESULT_OK, data);
        intending(hasComponent(VerIDSessionActivity.class.getName())).respondWith(activityResult);

        onView(withId(R.id.button)).perform(click());

        Thread.sleep(5000);

        intended(allOf(hasComponent(ResultActivity.class.getName()), hasExtraWithKey(ResultActivity.EXTRA_SCORE)));

        float score = -1f;
        for (Intent intent : getIntents()) {
            if (intent.hasExtra(ResultActivity.EXTRA_SCORE)) {
                score = intent.getFloatExtra(ResultActivity.EXTRA_SCORE, -1f);
                break;
            }
        }
        assertTrue(score > 3.8f);
    }

    @Test
    public void testFaceDetection_returnsExpectedFaceCoordinates() throws Exception {
        Face[] faces = getVerID().getFaceDetection().detectFacesInImage(getSelfieImage("selfieImage.jpg"), 1, 0);
        assertEquals(1, faces.length);
        float delta = 0.1f;
        assertEquals(81f, faces[0].getLeftEye().x, delta);
        assertEquals(71.5f, faces[0].getLeftEye().y, delta);
        assertEquals(111.0f, faces[0].getRightEye().x, delta);
        assertEquals(68.5f, faces[0].getRightEye().y, delta);
    }

    private VerIDImage getImageFromUri(Uri uri) throws IOException {
        Bitmap bitmap;
        int orientation;
        try (InputStream inputStream = new FileInputStream(uri.getPath())) {
            bitmap = BitmapFactory.decodeStream(inputStream);
        }
        if (bitmap == null) {
            throw new IOException();
        }
        try (InputStream inputStream = new FileInputStream(uri.getPath())) {
            orientation = new ExifInterface(inputStream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        }
        return new VerIDImage(bitmap, orientation);
    }

    private Uri getCardImageUri() throws Exception {
        try (InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("cardImage.png")) {
            idCardTempFile = File.createTempFile("cardImage_", ".png");
            try (FileOutputStream outputStream = new FileOutputStream(idCardTempFile)) {
                int read;
                byte[] buffer = new byte[512];
                while ((read = inputStream.read(buffer, 0, buffer.length)) > 0) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
                return Uri.fromFile(idCardTempFile);
            }
        }
    }

    private VerIDSessionResult getSessionResult(Exception exception) throws Exception {
        if (exception != null) {
            return new VerIDSessionResult(exception);
        } else {
            String fileName = "selfieImage.jpg";
            VerIDImage image = getSelfieImage(fileName);
            Face[] faces = getVerID().getFaceDetection().detectFacesInImage(image, 1, 0);
            assertEquals(1, faces.length);
            RecognizableFace[] recognizableFaces = getVerID().getFaceRecognition().createRecognizableFacesFromFaces(faces, image);

            DetectedFace detectedFace = new DetectedFace(recognizableFaces[0], Bearing.STRAIGHT, getSelfieImageUri(fileName));
            ArrayList<DetectedFace> attachments = new ArrayList<>();
            attachments.add(detectedFace);

            Parcel parcel = Parcel.obtain();
            parcel.writeTypedList(attachments);
            parcel.setDataPosition(0);
            return VerIDSessionResult.CREATOR.createFromParcel(parcel);
        }
    }

    private VerIDImage getSelfieImage(String name) throws Exception {
        Bitmap bitmap;
        int orientation;
        try (InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getAssets().open(name)) {
            bitmap = BitmapFactory.decodeStream(inputStream);
        }
        if (bitmap == null) {
            throw new Exception();
        }
        try (InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getAssets().open(name)) {
            ExifInterface exifInterface = new ExifInterface(inputStream);
            orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        }
        return new VerIDImage(bitmap, orientation);
    }

    private Uri getSelfieImageUri(String name) throws Exception {
        if (selfieTempFile == null) {
            try (InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getAssets().open(name)) {
                File tempFile = File.createTempFile("test_", ".jpg");
                try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
                    int read;
                    byte[] buffer = new byte[512];
                    while ((read = inputStream.read(buffer, 0, buffer.length)) > 0) {
                        fileOutputStream.write(buffer, 0, read);
                    }
                    fileOutputStream.flush();
                    selfieTempFile = tempFile;
                }
            }
        }
        return Uri.fromFile(selfieTempFile);
    }

    private VerID getVerID() throws Exception {
        if (verID == null) {
            verID = new VerIDFactory(InstrumentationRegistry.getInstrumentation().getTargetContext()).createVerIDSync();
        }
        return verID;
    }
}
