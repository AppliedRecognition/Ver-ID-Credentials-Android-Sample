package com.appliedrec.credentials.app

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import com.appliedrec.credentials.app.databinding.ActivitySettingsBinding
import com.appliedrec.verid.core2.VerID
import com.microblink.blinkid.MicroblinkSDK

class SettingsActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    lateinit var viewBinding: ActivitySettingsBinding
    lateinit var preferences: SharedPreferences

    val barcodeScannerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data?.hasExtra(Intent.EXTRA_TEXT) == true) {
            result.data?.let { data ->
                val barcode = data.getByteArrayExtra(Intent.EXTRA_TEXT) ?: return@registerForActivityResult
                AlertDialog.Builder(this)
                    .setTitle("Barcode scan result")
                    .setMessage(barcode.decodeToString())
                    .setNeutralButton("Cancel", null)
                    .create()
                    .show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        viewBinding.appVersion.text = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA).versionName
        viewBinding.veridVersion.text = VerID.getVersion()
        viewBinding.blinkidVersion.text = MicroblinkSDK.getNativeLibraryVersionString()
        viewBinding.useBackCamera.also {
            it.isChecked = preferences.useBackCamera
            it.setOnCheckedChangeListener { _, isChecked ->
                preferences.useBackCamera = isChecked
            }
        }
        viewBinding.enableActiveLiveness.also {
            it.isChecked = preferences.enableActiveLiveness
            it.setOnCheckedChangeListener { _, isChecked ->
                preferences.enableActiveLiveness = isChecked
            }
        }
        viewBinding.enableDocVer.also {
            it.isChecked = preferences.enableDocumentVerification
            it.setOnCheckedChangeListener { _, isChecked ->
                preferences.enableDocumentVerification = isChecked
            }
        }
        viewBinding.supportedDocuments.setOnClickListener {
            val url = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData.getString("supportedDocumentsUrl") ?: return@setOnClickListener
            val uri = Uri.parse(url)
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
        viewBinding.testBarcodeScanner.setOnClickListener {
            barcodeScannerLauncher.launch(Intent(this, BarcodeScannerActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        super.onStop()
        preferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PreferenceKeys.USE_BACK_CAMERA -> viewBinding.useBackCamera.isChecked = preferences.useBackCamera
            PreferenceKeys.ENABLE_ACTIVE_LIVENESS -> viewBinding.enableActiveLiveness.isChecked = preferences.enableActiveLiveness
            PreferenceKeys.ENABLE_DOCUMENT_VERIFICATION -> viewBinding.enableDocVer.isChecked = preferences.enableDocumentVerification
        }
    }
}