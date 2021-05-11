package com.appliedrec.credentials.app;

import android.graphics.Bitmap;

import com.appliedrec.verid.core2.RecognizableFace;
import com.appliedrec.verid.core2.serialization.CborAdapter;

@CborAdapter(FaceWithImageCborAdapter.class)
public class FaceWithImage {

    private RecognizableFace face;
    private Bitmap bitmap;

    public FaceWithImage(RecognizableFace face, Bitmap bitmap) {
        this.face = face;
        this.bitmap = bitmap;
    }

    public RecognizableFace getFace() {
        return face;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }
}
