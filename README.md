# Ver-ID Credentials SDK
The Ver-ID Credentials SDK allows your app to capture an image of the user's ID card and read the PDF 417 barcode on the back of the card. You can use Ver-ID to compare the face on the front of the ID card to a live selfie.

## Adding Ver-ID Credentials SDK in Your Android Studio Project

1. [Request an API secret](https://dev.ver-id.com/admin/register) for your app.
1. Open your app module's **gradle.build** file.
1. Under `repositories` add

    ```
    jcenter()
    maven { url 'http://maven.microblink.com' }
    maven { url 'https://dev.ver-id.com/artifactory/gradle-release' }
    ```
1. Under `dependencies` add

    ```
    implementation 'com.appliedrec:id-capture:4.1.0'
    ```
1. Open your app's **AndroidManifest.xml** file and add the following tag in `<application>` replacing `[your API secret]` with the API secret your received in step 1:

    ~~~xml
    <meta-data
        android:name="com.appliedrec.verid.apiSecret"
        android:value="[your API secret]" />
    ~~~
1. [Download resources archive](https://ver-id.s3.amazonaws.com/resources/models/v1/VerIDModels.zip) and put it in your app's **assets** folder.
1. As an alternative to the previous step, specify a URL from which to download the resources. This will reduce the download size of your app. In the app's manifest file:

    ~~~xml
    <meta-data
        android:name="com.appliedrec.verid.resourcesURL"
        android:value="http://my.domain.com/path/to/resources.zip" />
	~~~
## Getting Started with the Ver-ID Credentials API
To scan an ID card your app will start an activity with a Ver-ID ID Capture intent and receive the result in `onActivityResult`.

1. Load Ver-ID
2. Construct settings
3. Construct an intent
4. Start activity for result
5. Receive result
6. Unload Ver-ID

## Example 1 – Capture ID Card

~~~java
public class MyActivity extends AppCompatActivity {
	
	private static final int REQUEST_CODE_ID_CAPTURE = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.my_activity);
		VerID.shared.load(this, "myApiSecret", new VerID.LoadCallback() {
			@Override
			public void onLoad() {
				// Ver-ID is now loaded. Here you may do things 
				// like enable buttons that control the ID capture.
			}
			@Override
			public void onError(Exception exception) {
				// Ver-ID failed to load. Check your API secret.
			}
		});
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Unload Ver-ID when your activity gets destroyed
		VerID.shared.unload();
	}

	/**
	 * Call this method to start the ID capture 
	 * (for example in response to a button click).
	 */
	void runIdCapture() {
		// If you want to show a guide to the user set this to true
		boolean showGuide = true;
		// If you want to show the result of the scan to the user set this to true
		boolean showResult = true;
		// To extract a face that's suitable for face recognition
		boolean detectFaceForRecognition = true;
		// Set the region where the requested card was issued.
		// If you want the API to guess use the RegionUtil utility class.
		// The utility class will base the region on the device's SIM card provider 
		// or on the device's locale. If you want the user to choose the region
		// set the variable to Region.GENERAL.
		Region region = RegionUtil.getUserRegion(this);
		// Construct ID capture settings
		VerIDIDCaptureSettings settings = new VerIDIDCaptureSettings(region, showGuide, showResult, detectFaceForRecognition);
		// Construct an intent
		VerIDIDCaptureIntent intent = new VerIDIDCaptureIntent(this, settings);
		// Start the ID capture activity
		startActivityForResult(intent, REQUEST_CODE_ID_CAPTURE);
	}
	
	/**
	 * Listen for the result of the ID capture and display 
	 * the card and detected face in your activity.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_CODE_ID_CAPTURE && resultCode == RESULT_OK && data != null) {
			// Extract the ID capture result from the data intent
			IDDocument document = data.getParcelable(IDCaptureActivity.EXTRA_ID_DOCUMENT);
			if (document == null) {	
				return;
			}
			Uri imageUri = document.getImageUri();
			VerIDFace face = document.getFace();
			if (imageUri == null || face == null) {
				return;
			}
			/**
			 * Display the card
			 */
			((ImageView)findViewById(R.id.cardImageView)).setImageURI(imageUri);
		
			// Get the bounds of the face detected on the ID card
			Rect faceBounds = new Rect();
			face.getBoundingBox().round(faceBounds);
			// Load the image of the front of the card
			InputStream inputStream = getContentResolver().openInputStream(imageUri);
			try {
				Bitmap cardBitmap = BitmapFactory.decodeStream(inputStream);
				if (cardBitmap != null) {
					// Crop the card image to the face bounds
					Bitmap faceBitmap = Bitmap.createBitmap(cardBitmap, faceBounds.left, faceBounds.top, faceBounds.width(), faceBounds.height());
					
					/**
					 * Display the face
					 */
					((ImageView)findViewById(R.id.faceImageView)).setImageBitmap(faceBitmap);
				}
			} catch (FileNotFoundException e) {
			}
			
			/**
			 * Display the text from the card
			 */
			((TextView)findViewById(R.id.cardTextView)).setText(document.getDescription());
		}
	}
}
~~~

## Example 2 – Capture Live Face

~~~java
public class MyActivity extends AppCompatActivity {
	
	private static final int REQUEST_CODE_LIVENESS_DETECTION = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.my_activity);
		VerID.shared.load(this, "myApiSecret", new VerID.LoadCallback() {
			@Override
			public void onLoad() {
				// Ver-ID is now loaded. Here you may do things 
				// like enable buttons that control the ID capture.
			}
			@Override
			public void onError(Exception exception) {
				// Ver-ID failed to load. Check your API secret.
			}
		});
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Unload Ver-ID when your activity gets destroyed
		VerID.shared.unload();
	}
	
	/**
	 * Call this method to start the liveness detection session
	 * (for example in response to a button click).
	 */
	void runLivenessDetection() {
		// Create liveness detection settings
		VerIDLivenessDetectionSessionSettings settings = new VerIDLivenessDetectionSessionSettings();
		// Ask Ver-ID to extract face templates needed for face recognition
		settings.includeFaceTemplatesInResult = true;
		// Construct the liveness detection intent
		VerIDLivenessDetectionIntent intent = new VerIDLivenessDetectionIntent(this, settings);
		// Start the liveness detection activity
		startActivityForResult(intent, REQUEST_CODE_LIVENESS_DETECTION);
	}
	
	/**
	 * Listen for the result of the liveness detection
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_CODE_LIVENESS_DETECTION && resultCode == RESULT_OK && data != null) {
			// Extract the liveness detection session result from the data intent
			VerIDSessionResult verIDSessionResult = data.getParcelableExtra(VerIDActivity.EXTRA_SESSION_RESULT);
			if (verIDSessionResult == null || !verIDSessionResult.isPositive()) {
				return;
			}
			// Get the face and image URI of the first captured selfie
			HashMap<VerIDFace,Uri> faceImages = verIDSessionResult.getFaceImages(VerID.Bearing.STRAIGHT);
			if (faceImages.isEmpty()) {
				return;
			}
			Map.Entry<VerIDFace,Uri> faceImage = faceImages.entrySet().iterator().next();
			// Get the bounds of the face detected in the first selfie
			Rect faceBounds = new Rect();
			faceImage.getKey().getBoundingBox().round(faceBounds);
			InputStream inputStream = getContentResolver().openInputStream(imageUri);
			try {
				Bitmap cardBitmap = BitmapFactory.decodeStream(inputStream);
				if (cardBitmap != null) {
					// Crop the selfie to the face bounds
					Bitmap faceBitmap = Bitmap.createBitmap(cardBitmap, faceBounds.left, faceBounds.top, faceBounds.width(), faceBounds.height());
					
					/**
					 * Display the face
					 */
					((ImageView)findViewById(R.id.faceImageView)).setImageBitmap(faceBitmap);
				}
			} catch (FileNotFoundException e) {
			}
		}
	}	
}
~~~

## Example 3 - Compare Face on ID Card with Live Face

Building on example 1 and 2, you can use the results of the ID capture and liveness detection sessions and compare their faces.

**Important:** When requesting the live face you must set `includeFaceTemplatesInResult` of the `VerIDLivenessDetectionSessionSettings` and `detectFaceForRecognition` of the `VerIDIDCaptureSettings` object to `true` to extract the captured face template and make it available for recognition.

~~~java
public class CaptureResultActivity extends AppCompatActivity {

    // Live face and ID capture results
    private VerIDSessionResult livenessDetectionResult;
    private IDDocument document;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set up views
        // Obtain idCaptureResult and livenessDetectionResult, e.g. from intent parcelable
        // ...
        if (document == null || livenessDetectionResult == null) {
	        updateScore(null);
        }
        
        if (document.getFaceSuitableForRecognition() != null && livenessDetectionResult.getFacesSuitableForRecognition(VerID.Bearing.STRAIGHT).length > 0) {
            compareFaceTemplates();
            return;
        }
        updateScore(null);
    }

    private void compareFaceTemplates() {
    	AsyncTask.execute(new Runnable() {
    		@Override
    		public void run() {
    			Float score = null;
		        VerIDFace cardFace = document.getFaceSuitableForRecognition();
		        VerIDFace[] liveFaces = livenessDetectionResult.getFacesSuitableForRecognition(VerID.Bearing.STRAIGHT);
		        for (VerIDFace face : liveFaces) {
		            try {
		                float faceScore = FaceUtil.compareFaces(cardFace, face);
		                if (score == null) {
		                    score = faceScore;
		                } else {
		                    score = Math.max(faceScore, score);
		                }
		            } catch (Exception e) {
		                e.printStackTrace();
		            }
		        }
		        final Float finalScore = score;
		        runOnUiThread(new Runnable() {
		        	@Override
		        	public void run() {
			        	updateScore(finalScore);
			        }
		        });
    		}
		});
    }

    @UiThread
    private void updateScore(Float score) {
        if (score != null) {
            // Display the score
        } else {
            // Display error
        }
    }
}
~~~
