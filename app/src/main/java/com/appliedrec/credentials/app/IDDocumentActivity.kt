package com.appliedrec.credentials.app

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.preference.PreferenceManager
import com.appliedrec.credentials.app.databinding.ActivityIddocumentBinding
import com.appliedrec.verid.core2.session.LivenessDetectionSessionSettings
import com.appliedrec.verid.core2.session.VerIDSessionResult
import com.appliedrec.verid.ui2.CameraLocation
import com.appliedrec.verid.ui2.IVerIDSession
import com.appliedrec.verid.ui2.VerIDSession
import com.appliedrec.verid.ui2.VerIDSessionDelegate

class IDDocumentActivity : AppCompatActivity(), VerIDSessionDelegate {

    private lateinit var viewBinding: ActivityIddocumentBinding
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityIddocumentBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        viewBinding.captureFaceButton.background = ResourcesCompat.getDrawable(resources, R.drawable.button_disabled_background, null)
        assert(application is IDCaptureApplication)
        (application as IDCaptureApplication).verIDLiveData.observe(this) { verIDResult ->
            if (verIDResult?.isSuccess == true && (application as IDCaptureApplication).documentLiveData.value?.isSuccess == true) {
                enableCaptureButton()
            }
        }
        (application as IDCaptureApplication).documentLiveData.observe(this) { documentResult ->
            val capturedDocument = documentResult?.getOrNull() ?: return@observe
            val image = capturedDocument.image
            if (image != null) {
                val drawable = RoundedBitmapDrawableFactory.create(resources, image)
                drawable.cornerRadius = image.height / 10f
                viewBinding.documentImage.setImageDrawable(drawable)
                viewBinding.documentImage.setOnClickListener {
                    showDocumentDetails()
                }
                viewBinding.details.setOnClickListener {
                    showDocumentDetails()
                }
            }
            if (documentResult.isSuccess && (application as IDCaptureApplication).verIDLiveData.value?.isSuccess == true) {
                enableCaptureButton()
            }
        }
        (application as IDCaptureApplication).faceCaptureLiveData.observe(this) { faceCaptureResult ->
            if (faceCaptureResult?.isSuccess == true) {
                startActivity(Intent(this, FaceComparisonActivity::class.java))
            } else if (faceCaptureResult?.isFailure == true) {
                showError("Error", "Face capture failed")
            }
        }
    }

    private fun showDocumentDetails() {
        startActivity(Intent(this, DocumentDetailsActivity::class.java))
    }

    private fun enableCaptureButton() {
        viewBinding.captureFaceButton.background = ResourcesCompat.getDrawable(resources, R.drawable.button_background, null)
        viewBinding.captureFaceButton.setOnClickListener {
            captureLiveFace()
        }
    }

    private fun captureLiveFace() {
        val verID = (application as IDCaptureApplication).verIDLiveData.value?.getOrNull() ?: return
        val settings = LivenessDetectionSessionSettings()
        settings.faceCaptureCount = if (preferences.enableActiveLiveness) 2 else 1
        val session = VerIDSession(verID, settings)
        session.setDelegate(this)
        session.start()
    }

    override fun onSessionFinished(session: IVerIDSession<*>, result: VerIDSessionResult) {
        if (result.error.isPresent) {
            (application as IDCaptureApplication).faceCaptureLiveData.postValue(Result.failure(result.error.get()))
        } else {
            (application as IDCaptureApplication).faceCaptureLiveData.postValue(Result.success(result.faceCaptures.first()))
        }
    }

    override fun getSessionCameraLocation(session: IVerIDSession<*>): CameraLocation {
        return if (preferences.useBackCamera) CameraLocation.BACK else CameraLocation.FRONT
    }

    override fun finish() {
        (application as IDCaptureApplication).documentLiveData.postValue(null)
        super.finish()
    }
}