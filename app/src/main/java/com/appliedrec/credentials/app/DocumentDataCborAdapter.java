package com.appliedrec.credentials.app;

import com.appliedrec.verid.core2.serialization.CborCoder;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import com.fasterxml.jackson.dataformat.cbor.CBORParser;

public class DocumentDataCborAdapter implements CborCoder<DocumentData> {

    private final static String FIRST_NAME = "firstName";
    private final static String LAST_NAME = "lastName";
    private final static String ADDRESS = "address";
    private final static String DATE_OF_BIRTH = "dateOfBirth";
    private final static String DATE_OF_EXPIRY = "dateOfExpiry";
    private final static String DATE_OF_ISSUE = "dateOfIssue";
    private final static String DOCUMENT_NUMBER = "documentNumber";
    private final static String SEX = "sex";
    private final static String RAW_BARCODE = "rawBarcode";

    @Override
    public DocumentData decodeFromCbor(CBORParser parser) throws Exception {
        String firstName = null, lastName = null, address = null, dateOfBirth = null, dateOfExpiry = null, dateOfIssue = null, documentNumber = null, sex = null, rawBarcode = null;
        if ((parser.hasCurrentToken() && parser.getCurrentToken() == JsonToken.START_OBJECT) || parser.nextToken() == JsonToken.START_OBJECT) {
            while (parser.nextToken() == JsonToken.FIELD_NAME) {
                String fieldName = parser.getCurrentName();
                if (FIRST_NAME.equals(fieldName)) {
                    firstName = parser.nextTextValue();
                    continue;
                }
                if (LAST_NAME.equals(fieldName)) {
                    lastName = parser.nextTextValue();
                    continue;
                }
                if (ADDRESS.equals(fieldName)) {
                    address = parser.nextTextValue();
                    continue;
                }
                if (DATE_OF_BIRTH.equals(fieldName)) {
                    dateOfBirth = parser.nextTextValue();
                    continue;
                }
                if (DATE_OF_EXPIRY.equals(fieldName)) {
                    dateOfExpiry = parser.nextTextValue();
                    continue;
                }
                if (DATE_OF_ISSUE.equals(fieldName)) {
                    dateOfIssue = parser.nextTextValue();
                    continue;
                }
                if (DOCUMENT_NUMBER.equals(fieldName)) {
                    documentNumber = parser.nextTextValue();
                    continue;
                }
                if (SEX.equals(fieldName)) {
                    sex = parser.nextTextValue();
                    continue;
                }
                if (RAW_BARCODE.equals(fieldName)) {
                    rawBarcode = parser.nextTextValue();
                }
            }
        }
        return new DocumentData(firstName, lastName, address, dateOfBirth, dateOfExpiry, dateOfIssue, documentNumber, sex, rawBarcode);
    }

    @Override
    public void encodeToCbor(DocumentData documentData, CBORGenerator generator) throws Exception {
        generator.writeStartObject();
        if (documentData.getFirstName() != null) {
            generator.writeStringField(FIRST_NAME, documentData.getFirstName());
        }
        if (documentData.getLastName() != null) {
            generator.writeStringField(LAST_NAME, documentData.getLastName());
        }
        if (documentData.getAddress() != null) {
            generator.writeStringField(ADDRESS, documentData.getAddress());
        }
        if (documentData.getDateOfBirth() != null) {
            generator.writeStringField(DATE_OF_BIRTH, documentData.getDateOfBirth());
        }
        if (documentData.getDateOfExpiry() != null) {
            generator.writeStringField(DATE_OF_EXPIRY, documentData.getDateOfExpiry());
        }
        if (documentData.getDateOfIssue() != null) {
            generator.writeStringField(DATE_OF_ISSUE, documentData.getDateOfIssue());
        }
        if (documentData.getDocumentNumber() != null) {
            generator.writeStringField(DOCUMENT_NUMBER, documentData.getDocumentNumber());
        }
        if (documentData.getSex() != null) {
            generator.writeStringField(SEX, documentData.getSex());
        }
        if (documentData.getRawBarcode() != null) {
            generator.writeStringField(RAW_BARCODE, documentData.getRawBarcode());
        }
        generator.writeEndObject();
    }
}
