package com.appliedrec.credentials.app;

import android.os.Parcel;
import android.os.Parcelable;

import com.appliedrec.verid.core2.serialization.CborAdapter;
import com.microblink.entities.recognizers.blinkid.generic.BlinkIdCombinedRecognizer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@CborAdapter(DocumentDataCborAdapter.class)
public class DocumentData implements Parcelable {

    private final String firstName;
    private final String lastName;
    private final String address;
    private final String dateOfBirth;
    private final String dateOfExpiry;
    private final String dateOfIssue;
    private final String documentNumber;
    private final String sex;
    private String rawBarcode;

    public DocumentData(BlinkIdCombinedRecognizer.Result result) {
        firstName = result.getFirstName();
        lastName = result.getLastName();
        address = result.getAddress();
        dateOfBirth = result.getDateOfBirth().getOriginalDateString();
        dateOfExpiry = result.getDateOfExpiry().getOriginalDateString();
        dateOfIssue = result.getDateOfIssue().getOriginalDateString();
        documentNumber = result.getDocumentNumber();
        sex = result.getSex();
        rawBarcode = new String(result.getBarcodeResult().getRawData(), StandardCharsets.UTF_8);
    }

    DocumentData(String firstName, String lastName, String address, String dateOfBirth, String dateOfExpiry, String dateOfIssue, String documentNumber, String sex, String rawBarcode) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
        this.dateOfExpiry = dateOfExpiry;
        this.dateOfIssue = dateOfIssue;
        this.documentNumber = documentNumber;
        this.sex = sex;
        this.rawBarcode = rawBarcode;
    }

    private DocumentData(Parcel in) {
        firstName = in.readString();
        lastName = in.readString();
        address = in.readString();
        dateOfBirth = in.readString();
        dateOfExpiry = in.readString();
        dateOfIssue = in.readString();
        documentNumber = in.readString();
        sex = in.readString();
        rawBarcode = in.readString();
    }

    public static final Creator<DocumentData> CREATOR = new Creator<DocumentData>() {
        @Override
        public DocumentData createFromParcel(Parcel in) {
            return new DocumentData(in);
        }

        @Override
        public DocumentData[] newArray(int size) {
            return new DocumentData[size];
        }
    };

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getAddress() {
        return address;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getDateOfExpiry() {
        return dateOfExpiry;
    }

    public String getDateOfIssue() {
        return dateOfIssue;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public String getSex() {
        return sex;
    }

    public String getRawBarcode() {
        return rawBarcode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(firstName);
        parcel.writeString(lastName);
        parcel.writeString(address);
        parcel.writeString(dateOfBirth);
        parcel.writeString(dateOfExpiry);
        parcel.writeString(dateOfIssue);
        parcel.writeString(documentNumber);
        parcel.writeString(sex);
        parcel.writeString(rawBarcode);
    }
}
