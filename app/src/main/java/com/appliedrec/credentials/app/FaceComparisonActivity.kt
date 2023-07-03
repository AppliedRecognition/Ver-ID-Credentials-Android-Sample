package com.appliedrec.credentials.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.appliedrec.credentials.app.databinding.ActivityFaceComparisonBinding
import org.apache.commons.math3.distribution.NormalDistribution

class FaceComparisonActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityFaceComparisonBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityFaceComparisonBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        // Show progress bar
        (application as IDCaptureApplication).verIDLiveData.observe(this) {
            if (it?.isSuccess == true) {
                compareFaces()
            }
        }
        (application as IDCaptureApplication).faceCaptureLiveData.observe(this) {
            val faceCapture = it?.getOrNull() ?: return@observe
            val drawable = RoundedBitmapDrawableFactory.create(resources, faceCapture.faceImage)
            drawable.cornerRadius = faceCapture.faceImage.height / 10f
            viewBinding.liveFaceImage.setImageDrawable(drawable)
            compareFaces()
        }
        (application as IDCaptureApplication).documentLiveData.observe(this) {
            val faceCapture = it?.map { doc -> doc.faceCapture }?.getOrNull() ?: return@observe
            val drawable = RoundedBitmapDrawableFactory.create(resources, faceCapture.faceImage)
            drawable.cornerRadius = faceCapture.faceImage.height / 10f
            viewBinding.documentFaceImage.setImageDrawable(drawable)
            compareFaces()
        }
    }

    private fun compareFaces() {
        try {
            val verID = (application as IDCaptureApplication).verIDLiveData.value?.getOrNull() ?: return
            val docFace =
                (application as IDCaptureApplication).documentLiveData.value?.getOrNull()?.faceCapture?.face ?: return
            val liveFace =
                (application as IDCaptureApplication).faceCaptureLiveData.value?.getOrNull()?.face ?: return
            val score = verID.faceRecognition.compareSubjectFacesToFaces(
                arrayOf(liveFace),
                arrayOf(docFace)
            )
            val threshold = 4f
            if (score >= threshold) {
                val probability = NormalDistribution().cumulativeProbability(score.toDouble()) * 100.0
                viewBinding.scoreText.text = String.format("The face matching score %.02f indicates a likelihood of %.0f%% that the person on the ID card is the same person as the one in the selfie. We recommend a threshold of %.02f for a positive identification when comparing faces from identity cards.", score, probability, threshold)
            } else {
                viewBinding.scoreText.text = String.format("The face matching score %.02f indicates that the person on the ID card is likely NOT the same person as the one in the selfie. We recommend a threshold of %.02f for a positive identification when comparing faces from identity cards.", score, threshold)
            }
            viewBinding.scoreHeading.text = String.format("Score %.02f", score)
            viewBinding.progressBar.visibility = View.GONE
            viewBinding.content.visibility = View.VISIBLE
        } catch (error: Exception) {
            showError("Error", "Face comparison failed")
        }
    }

    override fun finish() {
        (application as IDCaptureApplication).faceCaptureLiveData.postValue(null)
        super.finish()
    }
}