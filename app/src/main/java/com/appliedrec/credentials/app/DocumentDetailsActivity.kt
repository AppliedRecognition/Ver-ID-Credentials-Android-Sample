package com.appliedrec.credentials.app

import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.appliedrec.credentials.app.databinding.ActivityDocumentDetailsBinding
import com.appliedrec.credentials.app.databinding.ViewDocumentDetailBinding

class DocumentDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewBinding = ActivityDocumentDetailsBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        assert(application is IDCaptureApplication)
        (application as IDCaptureApplication).documentLiveData.observe(this) {
            val doc = it?.getOrNull() ?: return@observe
            when (val image = doc.image) {
                is Bitmap -> {
                    val drawable = RoundedBitmapDrawableFactory.create(resources, image)
                    drawable.cornerRadius = image.height / 10f
                    viewBinding.documentImage.setImageDrawable(drawable)
                }
            }
            for (field in doc.textFields) {
                ViewDocumentDetailBinding.inflate(layoutInflater, viewBinding.content, true).also { view ->
                    view.key.text = field.name
                    view.value.text = field.value
                }
            }
        }
    }
}