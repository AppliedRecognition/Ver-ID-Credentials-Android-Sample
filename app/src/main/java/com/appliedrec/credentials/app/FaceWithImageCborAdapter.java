package com.appliedrec.credentials.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.appliedrec.verid.core2.RecognizableFace;
import com.appliedrec.verid.core2.serialization.Cbor;
import com.appliedrec.verid.core2.serialization.CborCoder;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import com.fasterxml.jackson.dataformat.cbor.CBORParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class FaceWithImageCborAdapter implements CborCoder<FaceWithImage> {

    public static final String FIELD_FACE = "face";
    public static final String FIELD_IMAGE = "image";
    public static final String FIELD_AUTHENTICITY_SCORE = "authenticityScore";

    @Override
    public FaceWithImage decodeFromCbor(CBORParser parser) throws Exception {
        RecognizableFace face = null;
        Bitmap bitmap = null;
        Float authenticityScore = null;
        if ((parser.hasCurrentToken() && parser.getCurrentToken() == JsonToken.START_OBJECT) || parser.nextToken() == JsonToken.START_OBJECT) {
            while (parser.nextToken() == JsonToken.FIELD_NAME) {
                String fieldName = parser.getCurrentName();
                if (FIELD_FACE.equals(fieldName) && parser.nextToken() != JsonToken.VALUE_NULL) {
                    byte[] encodedFace = parser.getBinaryValue();
                    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(encodedFace)) {
                        face = Cbor.decodeStream(inputStream, RecognizableFace.class);
                    }
                } else if (FIELD_IMAGE.equals(fieldName) && parser.nextToken() != JsonToken.VALUE_NULL) {
                    byte[] encodedBitmap = parser.getBinaryValue();
                    bitmap = BitmapFactory.decodeByteArray(encodedBitmap, 0, encodedBitmap.length);
                } else if (FIELD_AUTHENTICITY_SCORE.equals(fieldName) && parser.nextToken() == JsonToken.VALUE_NUMBER_FLOAT) {
                    authenticityScore = parser.getFloatValue();
                }
            }
        }
        if (face != null && bitmap != null) {
            return new FaceWithImage(face, bitmap, authenticityScore);
        }
        throw new Exception("Failed to decode face and image from CBOR");
    }

    @Override
    public void encodeToCbor(FaceWithImage faceWithImage, CBORGenerator cborGenerator) throws Exception {
        cborGenerator.writeStartObject();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Cbor.encodeToStream(faceWithImage.getFace(), outputStream);
            cborGenerator.writeBinaryField(FIELD_FACE, outputStream.toByteArray());
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            faceWithImage.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            cborGenerator.writeBinaryField(FIELD_IMAGE, outputStream.toByteArray());
        }
        Float authenticitScore = faceWithImage.getAuthenticityScore();
        if (authenticitScore != null) {
            cborGenerator.writeNumberField(FIELD_AUTHENTICITY_SCORE, authenticitScore);
        }
        cborGenerator.writeEndObject();
    }
}
