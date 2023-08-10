package com.appliedrec.credentials.app

import android.graphics.Bitmap
import com.appliedrec.verid.core2.session.FaceCapture
import com.microblink.blinkid.entities.recognizers.blinkid.DataMatchState
import com.microblink.blinkid.entities.recognizers.blinkid.generic.BlinkIdMultiSideRecognizer
import com.microblink.blinkid.entities.recognizers.blinkid.generic.classinfo.Country
import com.microblink.blinkid.entities.recognizers.blinkid.generic.classinfo.Region
import com.microblink.blinkid.entities.recognizers.blinkid.generic.classinfo.Type
import com.microblink.blinkid.entities.recognizers.blinkid.generic.datamatch.DataMatchField
import com.microblink.blinkid.entities.recognizers.blinkid.generic.datamatch.DataMatchResult
import com.microblink.blinkid.results.date.SimpleDate
import com.microblink.documentverification.client.data.model.result.DocumentVerificationResult
import com.microblink.documentverification.client.data.model.result.verification.CheckResult
import java.text.SimpleDateFormat
import java.util.*

class CapturedDocument(val documentCaptureResult: BlinkIdMultiSideRecognizer.Result, val documentVerificationResult: DocumentVerificationResult? = null, val faceCapture: FaceCapture, authenticityScore: Float? = null, rawBarcode: String? = null) {

    val documentNumber: String? = documentCaptureResult.documentNumber?.value()
    val firstName: String? = documentCaptureResult.firstName?.value()
    val lastName: String? = documentCaptureResult.lastName?.value()
    val fullName: String? = documentCaptureResult.fullName?.value()
    val address: String? = documentCaptureResult.address?.value()
    val street: String? = if (documentCaptureResult.barcodeResult.street.isBlank()) null else documentCaptureResult.barcodeResult.street
    val city: String? = if (documentCaptureResult.barcodeResult.city.isBlank()) null else documentCaptureResult.barcodeResult.city
    val jurisdiction: String? = if (documentCaptureResult.barcodeResult.jurisdiction.isBlank()) null else documentCaptureResult.barcodeResult.jurisdiction
    val postCode: String? = if (documentCaptureResult.barcodeResult.postalCode.isBlank()) null else documentCaptureResult.barcodeResult.postalCode
    val country: String? = if (documentCaptureResult.classInfo.country == Country.NONE) null else documentCaptureResult.classInfo.countryName
    val dateOfBirth: Date?
        get() = documentCaptureResult.dateOfBirth?.date?.toDate()

    val dateOfIssue: Date?
        get() = documentCaptureResult.dateOfIssue?.date?.toDate()

    val dateOfExpiry: Date?
        get() = documentCaptureResult.dateOfExpiry?.date?.toDate()

    val image: Bitmap? = documentVerificationResult?.extractionResult?.fullDocumentFrontImage ?: documentCaptureResult.fullDocumentFrontImage?.convertToBitmap()
    val type: Type = documentCaptureResult.classInfo.type
    val region: Region = documentCaptureResult.classInfo.region
    var rawBarcode: String? = rawBarcode
        set(value) {
            field = value
            ontarioHealthCardFrontBackMatch = if (value != null) {
                OntarioHealthCardFrontBackMatch(
                    value,
                    fullName,
                    documentNumber,
                    dateOfBirth,
                    dateOfExpiry
                )
            } else {
                null
            }
        }
    var ontarioHealthCardFrontBackMatch: OntarioHealthCardFrontBackMatch?
    val frontBackMatchCheck: DataMatchResult = documentCaptureResult.dataMatch
    private val dateFormatter: SimpleDateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    init {
        if (type == Type.HEALTH_INSURANCE_CARD && region == Region.ONTARIO && rawBarcode != null) {
            ontarioHealthCardFrontBackMatch = OntarioHealthCardFrontBackMatch(rawBarcode, fullName, documentNumber, dateOfBirth, dateOfExpiry)
        } else {
            ontarioHealthCardFrontBackMatch = null
        }
    }

    val textFields: List<DocumentField> by lazy {
        mutableListOf<DocumentField>().also { fields ->
            if (documentNumber?.isBlank() == false) {
                fields.add(DocumentField("Document number", documentNumber))
            }
            documentVerificationResult?.let { docVerResult ->
                docVerResult.dataCheck?.overall?.result?.let {
                    fields.add(DocumentField("Data check", it))
                }
                docVerResult.documentLivenessCheck?.overall?.let {
                    fields.add(DocumentField("Doc liveness check", it))
                }
                docVerResult.documentValidityCheck?.expiredCheck?.let {
                    fields.add(DocumentField("Doc expiration check", it))
                }
                docVerResult.imageQualityCheck?.blurCheck?.let {
                    fields.add(DocumentField("Image quality check", it))
                }
                docVerResult.visualCheck?.overall?.let {
                    fields.add(DocumentField("Visual check", it))
                }
            }
            ontarioHealthCardFrontBackMatch?.let { ontarioHealthCardFrontBackMatch ->
                fields.add(DocumentField("Document number check", if (ontarioHealthCardFrontBackMatch.documentNumberMatchesBarcode) "Passed" else "Failed"))
                fields.add(DocumentField("Name check", if (ontarioHealthCardFrontBackMatch.nameMatchesBarcode) "Passed" else "Failed"))
                fields.add(DocumentField("Date of birth check", if (ontarioHealthCardFrontBackMatch.dateOfBirthMatchesBarcode) "Passed" else "Failed"))
                fields.add(DocumentField("Date of expiry check", if (ontarioHealthCardFrontBackMatch.dateOfExpiryMatchesBarcode) "Passed" else "Failed"))
            }
            if (authenticityScore != null) {
                fields.add(DocumentField("Authenticity score", String.format(Locale.ROOT, "%.02f", authenticityScore)))
            }
            if (firstName?.isBlank() == false) {
                fields.add(DocumentField("First name", firstName))
            }
            if (lastName?.isBlank() == false) {
                fields.add(DocumentField("Last name", lastName))
            } else if (fullName?.isBlank() == false) {
                fields.add(DocumentField("Name", fullName))
            }
            if (street?.isBlank() == false) {
                fields.add(DocumentField("Street", street))
                if (city?.isBlank() == false) {
                    fields.add(DocumentField("City", city))
                }
                if (jurisdiction?.isBlank() == false) {
                    fields.add(DocumentField("Jurisdiction", jurisdiction))
                }
                if (postCode?.isBlank() == false) {
                    fields.add(DocumentField("Postal code", postCode))
                }
            } else if (address?.isBlank() == false) {
                fields.add(DocumentField("Address", address))
            }
            if (country?.isBlank() == false) {
                fields.add(DocumentField("Country", country))
            }
            dateOfBirth?.let {
                fields.add(DocumentField("Date of birth", dateFormatter.format(it)))
            }
            dateOfIssue?.let {
                fields.add(DocumentField("Date of issue", dateFormatter.format(it)))
            }
            dateOfExpiry?.let {
                fields.add(DocumentField("Date of expiry", dateFormatter.format(it)))
            }
            if (frontBackMatchCheck.stateForWholeDocument != DataMatchState.NotPerformed) {
                fields.add(DocumentField("Data match check", if (frontBackMatchCheck.stateForWholeDocument == DataMatchState.Success) "Passed" else "Failed"))
                for (state in frontBackMatchCheck.states) {
                    if (state.state != DataMatchState.NotPerformed && state.fieldType != null) {
                        when (state.fieldType!!) {
                            DataMatchField.DocumentNumber -> fields.add(DocumentField("Document number check", if (state.state == DataMatchState.Success) "Passed" else "Failed"))
                            DataMatchField.DateOfBirth -> fields.add(DocumentField("Date of birth check", if (state.state == DataMatchState.Success) "Passed" else "Failed"))
                            DataMatchField.DateOfExpiry -> fields.add(DocumentField("Date of expiry check", if (state.state == DataMatchState.Success) "Passed" else "Failed"))
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun dateFromSimpleDate(simpleDate: SimpleDate): Date {
        val calendar = Calendar.getInstance()
        calendar.set(simpleDate.year, simpleDate.month-1, simpleDate.day)
        return calendar.time
    }
}

data class DocumentField(val name: String, val value: String) {

    constructor(name: String, checkResult: CheckResult): this(name, if (checkResult == CheckResult.PASS) { "Passed" } else if (checkResult == CheckResult.FAIL) { "Failed" } else { "N/A"})
}

fun SimpleDate.toDate(): Date {
    val calendar = Calendar.getInstance()
    calendar.set(year, month - 1, day)
    return calendar.time
}