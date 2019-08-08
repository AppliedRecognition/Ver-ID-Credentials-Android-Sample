![Maven metadata URL](https://img.shields.io/maven-metadata/v/https/dev.ver-id.com/artifactory/gradle-release/com/appliedrec/id-capture/maven-metadata.xml.svg)

# Ver-ID Credentials SDK
The Ver-ID Credentials SDK allows your app to capture an image of the user's ID card and read the PDF 417 barcode on the back of the card. You can use Ver-ID to compare the face on the front of the ID card to a live selfie.

## Adding Ver-ID Credentials SDK in Your Android Studio Project

1. [Request an API secret](https://dev.ver-id.com/admin/register) for your app.
1. Open your app module's **gradle.build** file.
1. Add:

    ```groovy
    repositories {
	    jcenter()
	    maven { url 'http://maven.microblink.com' }
	    maven { url 'https://dev.ver-id.com/artifactory/gradle-release' }
    }
    ```
1. If your app is targeting Android API level 21+ add:

   ```groovy
	dependencies {
		implementation 'com.appliedrec:id-capture:6.0.3'
	}
   ```
1. If your app is targeting Android API level 18–21 add:

   ```groovy
	dependencies {
		implementation 'com.appliedrec:id-capture-api18:6.0.3'
	}
   ```
1. If you need to support both Android API level 18–20 and 21+ you may want to create product flavours. For example:

	```groovy
	android {
		flavorDimensions "apiLevel"
		productFlavors {
			api18 {
				dimension "apiLevel"
				minSdkVersion 18
				maxSdkVersion 20
			}
			api21 {
				dimension "apiLevel"
				minSdkVersion 21
			}
		}
	}
	dependencies {
		api21Implementation 'com.appliedrec:id-capture:6.0.3'
		api18Implementation 'com.appliedrec:id-capture-api18:6.0.3'
	}
	```
1. Open your app's **AndroidManifest.xml** file and add the following tag in `<application>` replacing `[your API secret]` with the API secret your received in step 1:

    ~~~xml
    <meta-data
        android:name="com.appliedrec.verid.apiSecret"
        android:value="[your API secret]" />
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
	private VerID verID;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.my_activity);
		new VerIDFactory(this, new VerIDFactoryDelegate() {
            @Override
            public void veridFactoryDidCreateEnvironment(VerIDFactory verIDFactory, VerID verID) {
            	MyActivity.this.verID = verID;
				// Ver-ID is now loaded. Here you may do things 
				// like enable buttons that control the ID capture.
            }

            @Override
            public void veridFactoryDidFailWithException(VerIDFactory verIDFactory, Exception e) {
                loadingIndicatorView.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, R.string.verid_failed_to_load, Toast.LENGTH_SHORT).show();
            }
        }).createVerID();
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
		// Set the region where the requested card was issued.
		// If you want the API to guess use the RegionUtil utility class.
		// The utility class will base the region on the device's SIM card provider 
		// or on the device's locale. If you want the user to choose the region
		// set the variable to Region.GENERAL.
		IDDocument document = RegionUtil.getDefaultDocumentForUserRegion(this);
		// Construct ID capture settings
		VerIDIDCaptureSettings settings = new VerIDIDCaptureSettings(document, showGuide, showResult);
		// Construct an intent
		VerIDIDCaptureIntent intent = new VerIDIDCaptureIntent(this, verID, settings);
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
			if (document == null || document.getFaceBounds() == null) {	
				return;
			}
			Uri imageUri = document.getImageUri();
			if (imageUri == null || face == null) {
				return;
			}
			/**
			 * Display the card
			 */
			((ImageView)findViewById(R.id.cardImageView)).setImageURI(imageUri);
		
			// Get the bounds of the face detected on the ID card
			Rect faceBounds = new Rect();
			document.getFaceBounds().round(faceBounds);
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
	private VerID verID;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.my_activity);
		new VerIDFactory(this, new VerIDFactoryDelegate() {
            @Override
            public void veridFactoryDidCreateEnvironment(VerIDFactory verIDFactory, VerID verID) {
            	MyActivity.this.verID = verID;
				// Ver-ID is now loaded. Here you may do things 
				// like enable buttons that control the ID capture.
            }

            @Override
            public void veridFactoryDidFailWithException(VerIDFactory verIDFactory, Exception e) {
                loadingIndicatorView.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, R.string.verid_failed_to_load, Toast.LENGTH_SHORT).show();
            }
        }).createVerID();	
    }
	
	/**
	 * Call this method to start the liveness detection session
	 * (for example in response to a button click).
	 */
	void runLivenessDetection() {
		// Create liveness detection settings
		LivenessDetectionSessionSettings settings = new LivenessDetectionSessionSettings();
		// Construct the liveness detection intent
		VerIDLivenessDetectionIntent intent = new VerIDLivenessDetectionIntent(this, verID, settings);
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
			VerIDSessionResult verIDSessionResult = data.getParcelableExtra(VerIDSessionActivity.EXTRA_RESULT);
			if (verIDSessionResult == null || verIDSessionResult.getError() != null) {
				return;
			}
			// Get the face and image URI of the first captured selfie
			HashMap<Face,Uri> faceImages = verIDSessionResult.getFaceImages(Bearing.STRAIGHT);
			if (faceImages.isEmpty()) {
				return;
			}
			Map.Entry<Face,Uri> faceImage = faceImages.entrySet().iterator().next();
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
	        setAuthenticated(null);
        }        
        if (document.getFaceSuitableForRecognition() != null && livenessDetectionResult.getFacesSuitableForRecognition(Bearing.STRAIGHT).length > 0) {
        	new VerIDFactory(this, new VerIDFactoryDelegate() {
	            @Override
	            public void veridFactoryDidCreateEnvironment(VerIDFactory verIDFactory, VerID verID) {
	            	compareFaceTemplates(verID);
	            }
	
	            @Override
	            public void veridFactoryDidFailWithException(VerIDFactory verIDFactory, Exception e) {
	                Toast.makeText(MainActivity.this, R.string.verid_failed_to_load, Toast.LENGTH_SHORT).show();
	            }
	        }).createVerID();
            return;
        }
        setAuthenticated(null);
    }

    private void compareFaceTemplates(VerID verId) {
    	AsyncTask.execute(new Runnable() {
    		@Override
    		public void run() {
    			Float score = null;
		        RecognizableFace cardFace = document.getFaceSuitableForRecognition();
		        RecognizableFace[] liveFaces = livenessDetectionResult.getFacesSuitableForRecognition(Bearing.STRAIGHT);
		        try {
		        	float score = verID.getFaceRecognition().compareSubjectFacesToFaces(new RecognizableFace[]{cardFace}, liveFaces);
		        	float threshold = verID.getFaceRecognition().getAuthenticationThreshold();
		        	// If score >= threshold the user can be considered authenticated against the ID card
		        	final boolean authenticated = 
			        runOnUiThread(new Runnable() {
			        	@Override
			        	public void run() {
				        	updateScore(score);
				        }
			        });
		        } catch (Exception e) {
		        	runOnUiThread(new Runnable() {
			        	@Override
			        	public void run() {
				        	setAuthenticated(null);
				        }
			        });
		        }
    		}
		});
    }

    @UiThread
    private void setAuthenticated(Boolean authenticated) {
        if (authenticated == null) {
            // Display error
        }
    }
}
~~~

[Reference documentation](https://appliedrecognition.github.io/Ver-ID-Credentials-Android/)
