# Ver-ID Credentials Sample

![](app/src/main/res/drawable-mdpi/woman_with_id_card.png)

The project contains a sample application that uses Microblink's [BlinkID SDK](https://github.com/BlinkID/blinkid-android) to scan an ID card. The app uses [Ver-ID SDK](https://github.com/AppliedRecognition/Ver-ID-UI-Android) to detect a face on the captured ID card and compare it to a live selfie taken with the Android device's camera.

## Adding Ver-ID to your Android Studio project
    
1. [Register your app](https://dev.ver-id.com/licensing/). You will need your app's package name.
2. Registering your app will generate an evaluation licence for your app. The licence is valid for 30 days. If you need a production licence please [contact Applied Recognition](mailto:sales@appliedrec.com).
2. When you finish the registration you'll receive a file called **Ver-ID identity.p12** and a password. Copy the password to a secure location.
3. Copy the **Ver-ID identity.p12** into your app's assets folder. A common location is **your\_app_module/src/main/assets**.
8. Ver-ID will need the password you received at registration.    
    - You can either specify the password when you create an instance of `VerIDFactory`:

        ~~~java
        VerIDFactory veridFactory = new VerIDFactory(this, "your password goes here");
        ~~~
    - Or you can add the password in your app's **AndroidManifest.xml**:

        ~~~xml
        <manifest>
            <application>
                <meta-data
                    android:name="com.appliedrec.verid.password"
                    android:value="your password goes here" />
            </application>
        </manifest>
        ~~~

1. Add the Applied Recognition repository and Ver-ID dependencies to your app module's **gradle.build** file:
    
    ~~~groovy    
    dependencies {
        implementation 'com.appliedrec.verid:ui2:[2.9,3.0['
    }
    ~~~

## Adding Microblink to your Android Studio project

1. Apply for an API key on the [Microblink website](https://microblink.com/products/blinkid).
1. Open your app module's **gradle.build** file and add:


    ```groovy
    repositories {
        maven { url 'http://maven.microblink.com' }
    }
    
    dependencies {
        implementation('com.microblink:blinkid:6.1.2@aar') {
            transitive = true
        }
    }
    ```

1. Detailed instructions are available on the [BlinkID Github page](https://github.com/BlinkID/blinkid-android#-sdk-integration). 


## Example 1 – Capture ID card

~~~kotlin
class MyActivity : AppCompatActivity {

    private var blinkIdRecognizer: BlinkIdMultiSideRecognizer? = null
    private var blinkIdRecognizerBundle: RecognizerBundle? = null

    private val idCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            if (result.resultCode == RESULT_OK && result.data != null) {
                blinkIdRecognizerBundle!!.loadFromIntent(result.data!!)
                val blinkIdResult = blinkIdRecognizer!!.result
                if (blinkIdResult.resultState == Recognizer.Result.State.Valid) {
                    // Get the image of the front of the ID card
                    val imageOfCard = blinkIdResult.fullDocumentFrontImage?.convertToBitmap() ?: throw Exception("Failed to extract document image from result")
                    // You can pass the above image to Ver-ID for face detection
                }
            }
        } catch (error: Exception) {
            TODO("Handle error")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Load Microblink SDK
        try {
            val key: String = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData?.getString("mb-licence-key") ?: throw Exception("Failed to load Blink ID licence key")
            MicroblinkSDK.setLicenseKey(key, this)
            MicroblinkSDK.setShowTrialLicenseWarning(false)
            MicroblinkSDK.setIntentDataTransferMode(IntentDataTransferMode.OPTIMISED)
        } catch (error: Exception) {
            TODO("Show error")
        }
    }
    
    fun captureIdCard() {
        blinkIdRecognizer = BlinkIdMultiSideRecognizer()
        blinkIdRecognizer!!.setReturnFullDocumentImage(true)
        blinkIdRecognizerBundle = RecognizerBundle(blinkIdRecognizer)
        val settings = BlinkIdUISettings(blinkIdRecognizerBundle)
        val intent = Intent(this, settings.targetActivity)
        settings.saveToIntent(intent)
        idCaptureLauncher.launch(intent)
    }
}
~~~

## Example 2 – Capture live face

~~~kotlin
class MyActivity : AppCompatActivity(), VerIDFactoryDelegate, VerIDSessionDelegate {

    private val authenticityClassifierFileName: String = "license01-20210820ay-xiypjic%2200-q08.nv"
    private var verID: VerID? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadVerID()
    }
    
    private fun loadVerID() {
        if (verID != null) {
            // Return if Ver-ID is already loaded
            return
        }
        
        val veridFactory = VerIDFactory()
        
        // To enable authenticity check on supported Canadian ID documents include
        // add the app/src/main/assets/license01-20210820ay-xiypjic%2200-q08.nv file
        // in your app's assets
        val detRecFactory = setupFaceDetectionRecognitionWithAuthenticityCheck()
        veridFactory.faceDetectionFactory = detRecFactory
        veridFactory.faceRecognitionFactory = detRecFactory
        // Skip the above 3 lines to disable authenticity check
        
        veridFactory.delegate = this
        veridFactory.createVerID()
    }
    
    private fun startFaceCapture() {
        val verID = this.verID ?: throw Exception("Ver-ID not loaded")
        val settings = LivenessDetectionSessionSettings()
        val session = VerIDSession(verID, settings)
        session.delegate = this
        session.start()
    }
    
    private fun setupFaceDetectionRecognitionWithAuthenticityCheck(): FaceDetectionRecognitionFactory {
        // Declare file to hold the cached classifier
        val classifierFile = File(cacheDir, authenticityClassifierFileName)
        // Copy classifier to your app's cache directory
        assets.open(authenticityClassifierFileName).use { input ->
            classifierFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        val classifier = Classifier("licence", classifierFile.path)
        val detRecFactory = FaceDetectionRecognitionFactory(this)
        detRecFactory.addClassifier(classifier)
        return detRecFactory
    }
    
    //region VerIDFactoryDelegate
    
    override fun onVerIDCreated(factory: VerIDFactory, verID: VerID) {
        this.verID = verID
        // TODO: Enable a button that starts a face capture session
    }

    override fun onVerIDCreationFailed(factory: VerIDFactory, error: Exception) {
        TODO("Handle error")
    }
    
    //endregion
    
    //region VerIDSessionDelegate
    
    override fun onSessionFinished(session: IVerIDSession<*>, result: VerIDSessionResult) {
        if (result.error.isPresent) {
            Log.e("Ver-ID", "Ver-ID session failed", result.error.get())
            TODO("Handle error")
            return
        }
        // Get the face looking straight at the camera
        val face = result.getFaceCaptures(Bearing.STRAIGHT).first().face
        // You can use the above face for comparison
    }
    
    //endregion
}
~~~

## Example 3 - Compare face on ID card with live face

Building on example 1 and 2, you can use the results of the ID capture and liveness detection sessions and compare their faces.

~~~kotlin
class FaceUtilities(verID: VerID) {

    // This must be the same name you used in the Classifier constructor 
    // (see setupFaceDetectionRecognitionWithAuthenticityCheck in example 2 above)
    private val authenticityClassifierName = "licence"

    // Compare live face with a face detected in an ID card image
    fun compareFaceToIDCard(face: RecognizableFace, idCardImage: Bitmap): Float {
        // Detect a face on the ID card
        val idCardFace: RecognizableFace = verID.utilities.orElseThrow {
            Exception("Ver-ID utilities unavailable")
        }.faceDetectionUtilities.detectRecognizableFacesInImage(Image(idCardImage), 1)
            .firstOrNull() ?: throw Exception("Failed to detect face on ID card")
        // Compare faces
        return verID.faceRecognition.compareSubjectFacesToFaces(arrayOf(face), arrayOf(idCardFace))
    }
    
    // Check document authenticity (works with select Canadian documents)
    fun checkDocumentAuthenticity(idCardImage: Bitmap): Float {
        val faceDetection = verID.faceDetection as? FaceDetection 
            ?: throw Exception("Face detection implementation does not support authenticity check")
        val image = Image(idCardImage)
        val face = faceDetection.detectFacesInImage(image, 1, 0).firstOrNull() ?: throw Exception("Failed to detect face on ID card")
        return faceDetection.extractAttributeFromFace(face, image, authenticityClassifierName)
    }
}
~~~

## Links

### Ver-ID
- [Github](https://github.com/AppliedRecognition/Ver-ID-UI-Android)
- [Reference documentation](https://appliedrecognition.github.io/Ver-ID-UI-Android/)

### BlinkID
- [Github](https://github.com/BlinkID/blinkid-android)
- [Reference documentation](https://blinkid.github.io/blinkid-android/)
