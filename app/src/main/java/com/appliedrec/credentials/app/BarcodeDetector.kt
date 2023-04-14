package com.appliedrec.credentials.app

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object BarcodeDetector {

    suspend fun detectBarcodeInImage(image: Bitmap): String = suspendCoroutine { continuation ->
        val options: BarcodeScannerOptions = BarcodeScannerOptions.Builder().setBarcodeFormats(
            Barcode.FORMAT_PDF417).build()
        val barcodeScanner = BarcodeScanning.getClient(options)
        val inputImage = InputImage.fromBitmap(image, 0)
        barcodeScanner.process(inputImage)
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull()?.rawBytes?.decodeToString()?.let { barcode ->
                    continuation.resume(barcode)
                } ?: continuation.resumeWithException(Exception("No barcode found"))
            }
    }
}