package com.appliedrec.credentials.app

import java.util.*
import kotlin.math.abs

class OntarioHealthCardFrontBackMatch(val barcode: String, val name: String?, val documentNumber: String?, val dateOfBirth: Date?, val dateOfExpiry: Date?) {

    var isHealthCardBarcode: Boolean = barcode.startsWith("@ON HC01.")

    val nameMatchesBarcode: Boolean
        get() {
            if (name == null || barcodeName == null) {
                return false
            }
            return name.equals(barcodeName, true)
        }

    val documentNumberMatchesBarcode: Boolean
        get() {
            if (documentNumber == null || barcodeDocumentNumber == null) {
                return false
            }
            return Regex("\\W").replace(documentNumber, "").startsWith(barcodeDocumentNumber!!, true)
        }

    val dateOfBirthMatchesBarcode: Boolean
        get() {
            if (dateOfBirth == null || barcodeDateOfBirth == null) {
                return false
            }
            return abs(dateOfBirth.time - barcodeDateOfBirth!!.time) < (1000 * 60 * 60 * 24)
        }

    val dateOfExpiryMatchesBarcode: Boolean
        get() {
            if (dateOfExpiry == null || barcodeDateOfExpiry == null) {
                return false
            }
            return abs(dateOfExpiry.time - barcodeDateOfExpiry!!.time) < (1000 * 60 * 60 * 24)
        }

    val barcodeName: String?
        get() {
            if (!isHealthCardBarcode) {
                return null
            }
            val re = Regex("[^A-Za-z0-9 ]")
            return re.replace(this.barcode.substring(30..57), "")
        }

    val barcodeDocumentNumber: String?
        get() {
            if (!isHealthCardBarcode) {
                return null
            }
            return this.barcode.substring(20..29).trim()
        }

    val barcodeDateOfBirth: Date?
        get() {
            if (!isHealthCardBarcode) {
                return null
            }
            return dateFromString(this.barcode.substring(58..65))
        }

    val barcodeDateOfExpiry: Date?
        get() {
            if (!isHealthCardBarcode) {
                return null
            }
            return dateFromString(this.barcode.substring(75..82))
        }

    private fun dateFromString(string: String): Date? {
        if (string.length != 8) {
            return null
        }
        val year = string.substring(0..3).toInt()
        val month = string.substring(4..5).toInt()
        val day = string.substring(6..7).toInt()
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day)
        return calendar.time
    }
}