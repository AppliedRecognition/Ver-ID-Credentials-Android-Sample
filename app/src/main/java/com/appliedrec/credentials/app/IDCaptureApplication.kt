package com.appliedrec.credentials.app

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.appliedrec.verid.core2.*
import com.appliedrec.verid.core2.session.FaceCapture
import com.microblink.blinkid.MicroblinkSDK
import com.microblink.blinkid.intent.IntentDataTransferMode
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*

class IDCaptureApplication: Application(), VerIDFactoryDelegate {

    private val _verIDLiveData = MutableLiveData<Result<VerID>?>(null)
    val verIDLiveData: LiveData<Result<VerID>?> = _verIDLiveData
    val documentLiveData = MutableLiveData<Result<CapturedDocument>?>(null)
    val faceCaptureLiveData = MutableLiveData<Result<FaceCapture>?>(null)

    override fun onCreate() {
        super.onCreate()
        MainScope().launch {
            withContext(Dispatchers.IO) {
                try {
                    val key = downloadMicroblinkKey().getOrThrow()
                    withContext(Dispatchers.Main) {
                        MicroblinkSDK.setLicenseKey(key, this@IDCaptureApplication)
                        MicroblinkSDK.setShowTrialLicenseWarning(false)
                        MicroblinkSDK.setIntentDataTransferMode(IntentDataTransferMode.OPTIMISED)
                        loadVerID()
                    }
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            }
        }
    }

    private fun loadVerID() {
        if (_verIDLiveData.value == null) {
            val detRecFactory = FaceDetectionRecognitionFactory(this)
            detRecFactory.faceTemplateVersions = EnumSet.of(VerIDFaceTemplateVersion.getLatest())
            detRecFactory.defaultFaceTemplateVersion = VerIDFaceTemplateVersion.getLatest()
            AuthenticityScoreSupport.getClassifiers(this).forEach { classifier ->
                detRecFactory.addClassifier(classifier)
            }
            val veridFactory = VerIDFactory(this)
            veridFactory.faceDetectionFactory = detRecFactory
            veridFactory.faceRecognitionFactory = detRecFactory
            veridFactory.delegate = this
            veridFactory.createVerID()
        }
    }

    override fun onVerIDCreated(factory: VerIDFactory, verID: VerID) {
        _verIDLiveData.postValue(Result.success(verID))
    }

    override fun onVerIDCreationFailed(factory: VerIDFactory, error: Exception) {
        _verIDLiveData.postValue(Result.failure(error))
    }

    suspend fun downloadMicroblinkKey(): Result<String> = coroutineScope {
        try {
            OkHttpClient.Builder().build().newCall(
                Request.Builder()
                    .url("https://ver-id.s3.amazonaws.com/blinkid-keys/android/${packageName}.txt")
                    .build()
            ).execute().use { response ->
                if (response.isSuccessful) {
                    val key = response.body?.string()
                    if (key != null) {
                        return@coroutineScope Result.success(key)
                    }
                }
                return@coroutineScope Result.failure(Exception("Failed to download Microblink key"))
            }
        } catch (e: Exception) {
            return@coroutineScope Result.failure(e)
        }
    }
}