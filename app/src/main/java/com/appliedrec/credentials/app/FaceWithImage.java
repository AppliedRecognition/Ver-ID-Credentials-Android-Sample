package com.appliedrec.credentials.app;

import android.graphics.Bitmap;

import com.appliedrec.verid.core2.RecognizableFace;
import com.appliedrec.verid.core2.serialization.CborAdapter;

@CborAdapter(FaceWithImageCborAdapter.class)
public class FaceWithImage {

    private RecognizableFace face;
    private Bitmap bitmap;
    private Float authenticityScore;

    public FaceWithImage(RecognizableFace face, Bitmap bitmap) {
        this.face = face;
        this.bitmap = bitmap;
    }

    public FaceWithImage(RecognizableFace face, Bitmap bitmap, Float authenticityScore) {
        this.face = face;
        this.bitmap = bitmap;
        this.authenticityScore = authenticityScore;
    }

    public RecognizableFace getFace() {
        return face;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public Float getAuthenticityScore() {
        return authenticityScore;
    }
}
