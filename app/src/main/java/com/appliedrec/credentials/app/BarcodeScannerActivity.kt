package com.appliedrec.credentials.app

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.appliedrec.credentials.app.databinding.ActivityBarcodeScannerBinding
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BarcodeScannerActivity : AppCompatActivity(),
    OnSuccessListener<MutableList<Barcode>>, OnFailureListener {

    private lateinit var viewBinding: ActivityBarcodeScannerBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        window.setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
        viewBinding = ActivityBarcodeScannerBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        val options: BarcodeScannerOptions = BarcodeScannerOptions.Builder().setBarcodeFormats(
            Barcode.FORMAT_PDF417).build()
        barcodeScanner = BarcodeScanning.getClient(options)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermissionRequestLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private val cameraPermissionRequestLauncher: ActivityResultLauncher<String> = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startCamera()
        } else {
            finish()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(getApplicationContext());
        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val preview: Preview = Preview.Builder().build()
                preview.setSurfaceProvider(viewBinding.cameraPreview.surfaceProvider)

                val imageAnalyser: ImageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(3000, 3000))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                imageAnalyser.setAnalyzer(cameraExecutor, this::detectBarcodeInImage)

                cameraProvider.unbindAll()
                var cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                }
                val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyser);
                if (camera.cameraInfo.hasFlashUnit()) {
                    viewBinding.torchButton.visibility = android.view.View.VISIBLE
                    viewBinding.torchButton.setOnClickListener {
                        val enableTorchFuture = if (camera.cameraInfo.torchState.value == TorchState.OFF) {
                            camera.cameraControl.enableTorch(true)
                        } else {
                            camera.cameraControl.enableTorch(false)
                        }
                        enableTorchFuture.addListener({
                            try {
                                enableTorchFuture.get()
                                val torchState = camera.cameraInfo.torchState.value
                                if (torchState == TorchState.ON) {
                                    viewBinding.torchButton.setImageResource(R.drawable.baseline_flashlight_off_24)
                                } else {
                                    viewBinding.torchButton.setImageResource(R.drawable.baseline_flashlight_on_24)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace();
                            }
                        }, ContextCompat.getMainExecutor(this))
                    }
                } else {
                    viewBinding.torchButton.visibility = android.view.View.GONE
                }
                enableTapToZoom(camera.cameraControl)
            } catch (e: Exception) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun enableTapToZoom(cameraControl: CameraControl) {
        runOnUiThread {
            viewBinding.cameraPreview.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> return@setOnTouchListener true
                    MotionEvent.ACTION_UP -> {
                        val factory = viewBinding.cameraPreview.meteringPointFactory
                        val point = factory.createPoint(event.x, event.y)
                        val action = FocusMeteringAction.Builder(point).build()
                        cameraControl.startFocusAndMetering(action)
                        return@setOnTouchListener true
                    }
                    else -> return@setOnTouchListener false
                }
            }
        }
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun detectBarcodeInImage(imageProxy: ImageProxy) {
        val image: Image? = imageProxy.image
        if (image == null) {
            imageProxy.close()
            return
        }
        val inputImage: InputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(inputImage)
            .addOnSuccessListener(this)
            .addOnFailureListener(this)
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    override fun onSuccess(barcodes: MutableList<Barcode>?) {
        barcodes?.let { list ->
            if (list.size == 1 && list[0].rawBytes != null) {
                runOnUiThread {
                    val intent = Intent()
                    intent.putExtra(Intent.EXTRA_TEXT, list[0].rawBytes)
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }
        }
    }

    override fun onFailure(e: Exception) {
        Log.w("Ver-ID", "Barcode scan failed: "+e.localizedMessage)
    }
}