package com.appliedrec.credentials.app

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.appliedrec.credentials.app.databinding.ActivityMainBinding
import com.appliedrec.verid.core2.*
import com.appliedrec.verid.core2.session.FaceCapture
import com.microblink.blinkid.entities.recognizers.Recognizer
import com.microblink.blinkid.entities.recognizers.RecognizerBundle
import com.microblink.blinkid.entities.recognizers.blinkid.generic.BlinkIdMultiSideRecognizer
import com.microblink.blinkid.entities.recognizers.blinkid.generic.classinfo.Region
import com.microblink.blinkid.entities.recognizers.blinkid.generic.classinfo.Type
import com.microblink.blinkid.uisettings.BlinkIdUISettings
import com.microblink.documentverification.client.DocumentVerificationClient
import com.microblink.documentverification.client.Response
import com.microblink.documentverification.client.data.model.request.DocumentVerificationRequest
import com.microblink.documentverification.client.data.model.request.ImageSource
import com.microblink.documentverification.client.data.model.result.DocumentVerificationResult
import com.microblink.documentverification.client.settings.DocumentVerificationServiceSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MainActivity : AppCompatActivity() {

    private var blinkIdRecognizer: BlinkIdMultiSideRecognizer? = null
    private var blinkIdRecognizerBundle: RecognizerBundle? = null
    private var docVerClient: DocumentVerificationClient? = null
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var preferences: SharedPreferences
    private var capturedDocument: CapturedDocument? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        viewBinding.progressBar.visibility = View.VISIBLE
        viewBinding.content.visibility = View.GONE
        viewBinding.scanIDCardButton.setOnClickListener {
            captureDocument()
        }
        assert(application is IDCaptureApplication)
        (application as IDCaptureApplication).verIDLiveData.observe(this) { verIDResult ->
            if (verIDResult == null) {
                return@observe
            }
            viewBinding.content.visibility = View.VISIBLE
            viewBinding.progressBar.visibility = View.GONE
            if (verIDResult.isSuccess) {
                observeDocumentCapture()
            } else {
                val error = verIDResult.exceptionOrNull()?.localizedMessage ?: "Not sure why"
                showError("Failed to load Ver-ID", error)
            }
        }
        viewBinding.settings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun observeDocumentCapture() {
        (application as IDCaptureApplication).documentLiveData.observe(this) { result ->
            if (result == null) {
                return@observe
            }
            viewBinding.content.visibility = View.VISIBLE
            viewBinding.progressBar.visibility = View.GONE
            if (result.isSuccess) {
                startActivity(Intent(this, IDDocumentActivity::class.java))
            } else {
                val error = result.exceptionOrNull()?.localizedMessage ?: "Something went wrong"
                showError("Scan failed", error)
            }
        }
    }

    private fun createDocumentVerificationClient(): DocumentVerificationClient {
        val metaData = getAppMetadata()
        val docVerURL = metaData.getString("docver.url") ?: throw Exception()
        val docVerClientId = metaData.getString("docver.clientid") ?: throw Exception()
        val docVerClientSecret = metaData.getString("docver.clientsecret") ?: throw Exception()
        val docVerClientSettings = DocumentVerificationServiceSettings(docVerURL, docVerClientId, docVerClientSecret)
        return DocumentVerificationClient(docVerClientSettings)
    }

    private fun getAppMetadata(): Bundle {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.packageManager.getApplicationInfo(this.packageName, PackageManager.ApplicationInfoFlags.of(
                PackageManager.GET_META_DATA.toLong()
            )).metaData
        } else {
            this.packageManager.getApplicationInfo(this.packageName, PackageManager.GET_META_DATA).metaData
        }
    }

    private suspend fun processDocumentCaptureResult(blinkIdResult: BlinkIdMultiSideRecognizer.Result): Pair<Bitmap,DocumentVerificationResult?> {
        val bitmap: Bitmap
        var docVerResult: DocumentVerificationResult? = null
        if (docVerClient != null) {
            val frontBitmap = blinkIdResult.frontCameraFrame.convertToBitmap() ?: throw Exception()
            val backBitmap = blinkIdResult.backCameraFrame.convertToBitmap() ?: throw Exception()
            val frontImage = ImageSource(frontBitmap)
            val backImage = ImageSource(backBitmap)
            val docVerificationRequest = DocumentVerificationRequest(frontImage, backImage, true)
            when (val response = docVerClient!!.verify(docVerificationRequest)) {
                is Response.Error -> {
                    throw response.exception ?: Exception()
                }
                is Response.Success -> {
                    bitmap = response.endpointResponse.data?.extractionResult?.fullDocumentFrontImage
                            ?: throw Exception()
                    docVerResult = response.endpointResponse.data
                }
            }
        } else {
            bitmap = blinkIdResult.fullDocumentFrontImage?.convertToBitmap() ?: throw Exception()
        }
        return Pair(bitmap, docVerResult)
    }

    private suspend fun getDocumentBackImage(blinkIdResult: BlinkIdMultiSideRecognizer.Result): Bitmap? {
        if (blinkIdRecognizer?.shouldSaveCameraFrames() == true) {
            return blinkIdResult.backCameraFrame.convertToBitmap()
        } else if (blinkIdRecognizer?.shouldReturnFullDocumentImage() == true) {
            return blinkIdResult.fullDocumentBackImage?.convertToBitmap()
        } else {
            return null
        }
    }

    private val barcodeScanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data?.hasExtra(Intent.EXTRA_TEXT) == true) {
            val barcode = result.data?.getByteArrayExtra(Intent.EXTRA_TEXT)
            if (barcode != null && capturedDocument != null) {
                capturedDocument!!.rawBarcode = barcode.decodeToString()
                (application as IDCaptureApplication).documentLiveData.postValue(
                    Result.success(capturedDocument!!)
                )
                capturedDocument = null
            } else {
                showError("Scan failed", "No barcode found")
            }
        } else if (result.resultCode == RESULT_CANCELED) {
            viewBinding.content.visibility = View.VISIBLE
            viewBinding.progressBar.visibility = View.GONE
        }
    }

    @SuppressLint("RestrictedApi")
    private val documentCaptureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            blinkIdRecognizerBundle!!.loadFromIntent(result.data!!)
            val blinkIdResult = blinkIdRecognizer!!.result
            if (blinkIdResult.resultState == Recognizer.Result.State.Valid) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val bitmapAndDocVerResult = processDocumentCaptureResult(blinkIdResult)
                        var rawBarcode: String? = null
                        if (blinkIdResult.classInfo.type == Type.HEALTH_INSURANCE_CARD && blinkIdResult.classInfo.region == Region.ONTARIO) {
                            getDocumentBackImage(blinkIdResult)?.let { backImage ->
                                try {
                                    rawBarcode = BarcodeDetector.detectBarcodeInImage(backImage)
                                } catch (error: Exception) {
                                    Log.e("BarcodeDetector", "Failed to detect barcode", error)
                                }
                            }
                        }
                        val image = Image(bitmapAndDocVerResult.first)
                        val detectAuthenticity: Boolean = AuthenticityScoreSupport.isDocumentSupported(blinkIdResult)
                        val faceAuthenticityPair = detectFaceInImage(image, detectAuthenticity)
                        val faceCapture = FaceCapture(
                            faceAuthenticityPair.first,
                            Bearing.STRAIGHT,
                            bitmapAndDocVerResult.first,
                            image
                        )
                        val document = CapturedDocument(
                            documentCaptureResult = blinkIdResult,
                            faceCapture = faceCapture,
                            documentVerificationResult = bitmapAndDocVerResult.second,
                            authenticityScore = faceAuthenticityPair.second,
                            rawBarcode = rawBarcode
                        )
                        if (document.type == Type.HEALTH_INSURANCE_CARD && document.region == Region.ONTARIO && rawBarcode == null) {
                            withContext(Dispatchers.Main) {
                                capturedDocument = document
                                captureOntarioHealthCardBarcode()
                            }
                        } else {
                            (application as IDCaptureApplication).documentLiveData.postValue(
                                Result.success(document)
                            )
                        }
                    } catch (error: Exception) {
                        (application as IDCaptureApplication).documentLiveData.postValue(Result.failure(error))
                    }
                }
            }
        } else if (result.resultCode == RESULT_CANCELED) {
            viewBinding.content.visibility = View.VISIBLE
            viewBinding.progressBar.visibility = View.GONE
        }
    }

    private suspend fun detectFaceInImage(image: Image, detectAuthenticity: Boolean): Pair<RecognizableFace,Float?> = suspendCoroutine { continuation ->
        val verID = (application as IDCaptureApplication).verIDLiveData.value!!.getOrElse {
            continuation.resumeWithException(it)
            return@suspendCoroutine
        }
        try {
            val faceDetectionUtilities = verID.utilities.map { it.faceDetectionUtilities }
                .orElseThrow { Exception("Face detection utilities not available") }
            faceDetectionUtilities.detectRecognizableFacesInImage(image, 1, EnumSet.of(VerIDFaceTemplateVersion.getLatest())) { faceDetectionResult ->
                try {
                    var authenticityScore: Float? = null
                    val face = faceDetectionResult.get().firstOrNull() ?: throw Exception("No face detected")
                    if (detectAuthenticity && verID.faceDetection is FaceDetection) {
                        val classifier = AuthenticityScoreSupport.getClassifiers(this).firstOrNull()
                        if (classifier != null) {
                            val faceDetection = verID.faceDetection as FaceDetection
                            authenticityScore = faceDetection.extractAttributeFromFace(face, image, classifier.name)
                        }
                    }
                    continuation.resume(Pair(face, authenticityScore))
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    private fun captureOntarioHealthCardBarcode() {
        AlertDialog.Builder(this)
            .setTitle("Barcode scan failed")
            .setMessage("We were unable to read the barcode on the back of the card")
            .setPositiveButton("Retry") { _, _ ->
                barcodeScanLauncher.launch(Intent(this, BarcodeScannerActivity::class.java))
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun captureDocument() {
        viewBinding.content.visibility = View.GONE
        viewBinding.progressBar.visibility = View.VISIBLE
        blinkIdRecognizer = BlinkIdMultiSideRecognizer()
        if (preferences.enableDocumentVerification) {
            blinkIdRecognizer!!.setSaveCameraFrames(true)
            docVerClient = createDocumentVerificationClient()
        } else {
            blinkIdRecognizer!!.setReturnFullDocumentImage(true)
            docVerClient = null
        }
        blinkIdRecognizerBundle = RecognizerBundle(blinkIdRecognizer)
        val settings = BlinkIdUISettings(blinkIdRecognizerBundle)
        val intent = Intent(this, settings.targetActivity)
        settings.saveToIntent(intent)
        documentCaptureLauncher.launch(intent)
    }
}