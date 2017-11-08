# Ver-ID ID Capture
The Ver-ID ID Capture SDK allows your app to capture an image of the user's ID card and read the PDF 417 barcode on the back of the card. You can use Ver-ID to compare the face on the front of the ID card to a live selfie.

## Adding Ver-ID ID Capture in Your Android Studio Project

1. [Request an API secret](https://dev.ver-id.com/admin/register) for your app.
2. Open your app module's **gradle.build** file.
1. Under `repositories` add

	```
	jcenter()
	maven { url 'http://maven.microblink.com' }
	maven { url 'https://dev.ver-id.com/artifactory/gradle-release' }
	```
1. Under `dependencies` add

	```
	compile 'com.appliedrec:shared:2.0'
	compile 'com.appliedrec:det-rec-lib:2.0'
	compile 'com.appliedrec:verid:2.0'
	compile 'com.appliedrec:id-capture:2.0'
	compile('com.microblink:blinkid:3.9.0@aar') {
		transitive = true
	}
	compile 'com.android.support:appcompat-v7:25.3.1'
	compile 'com.android.support.constraint:constraint-layout:1.0.2'
	compile 'com.android.support:design:25.3.1'
	```
	
## Getting Started with the Ver-ID ID Capture API
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
		// Set the region where the requested card was issued.
		// If you want the API to guess use the RegionUtil utility class.
		// The utility class will base the region on the device's SIM card provider 
		// or on the device's locale. If you want the user to choose the region
		// set the variable to Region.GENERAL.
		Region region = RegionUtil.getUserRegion(this);
		// Construct ID capture settings
		VerIDIDCaptureSettings settings = new VerIDIDCaptureSettings(region, showGuide, showResult);
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
			IDCaptureResult idCaptureResult = data.getParcelable(IDCaptureActivity.EXTRA_RESULT);
			if (idCaptureResult != null) {			
				/**
				 * Display the card
				 */
				((ImageView)findViewById(R.id.cardImageView)).setImageURI(idCaptureResult.getFrontImageUri());
			
				// Get the bounds of the face detected on the ID card
				Rect faceBounds = new Rect();
				idCaptureResult.getFaceBounds().round(faceBounds);
				// Load the image of the front of the card
				InputStream inputStream = getContentResolver().openInputStream(idCaptureResult.getFrontImageUri());
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
				((TextView)findViewById(R.id.cardTextView)).setText(idCaptureResult.getTextDescription());
			}
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
		// Construct the liveness detection intent
		VerIDLivenessDetectionIntent intent = new VerIDLivenessDetectionIntent(this);
		// Start the liveness detection activity
		startActivityForResult(intent, REQUEST_CODE_LIVENESS_DETECTION);
	}
	
	/**
	 * Listen for the result of the liveness detection
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_CODE_ID_CAPTURE && resultCode == RESULT_OK && data != null) {
			// Extract the liveness detection session result from the data intent
			VerIDSessionResult verIDSessionResult = data.getParcelableExtra(VerIDActivity.EXTRA_SESSION_RESULT);
			if (verIDSessionResult != null && verIDSessionResult.isPositive()) {
				// Get the URI of the first captured selfie
				Uri imageUri = verIDSessionResult.getImageUris(VerID.Bearing.STRAIGHT)[0];
				// Get the bounds of the face detected in the first selfie
				Rect faceBounds = verIDSessionResult.getFaceBounds(VerID.Bearing.STRAIGHT)[0];
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
}
~~~

## Example 3 - Compare Face on ID Card with Live Face

Building on example 1 and 2, you can use the results of the ID capture and liveness detection sessions and compare their faces. The activity below contains two `AsyncTaskLoader` classes that extract the bitmaps from the results and run the face comparison.

**Important:** When requesting the live face you must set `includeFaceTemplatesInResult` of the `VerIDLivenessDetectionSessionSettings` object to `true` to extract the captured face template and make it available for recognition.

~~~java
public class CaptureResultActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks, FaceTemplateExtraction.FaceTemplateExtractionListener {

    // Loader IDs
    private static final int LOADER_ID_SCORE = 458;
    private static final int LOADER_ID_REGISTRATION = 123;

    // Live face and ID capture results
    private VerIDSessionResult livenessDetectionResult;
    private IDCaptureResult idCaptureResult;

    /**
     * Loader that compares the face from the card with the live face(s)
     */
    private static class ScoreLoader extends AsyncTaskLoader<Float> {

        private VerIDUser cardUser;
        private FBFace[] faces;

        public ScoreLoader(Context context, VerIDUser cardUser, FBFace[] faces) {
            super(context);
            this.cardUser = cardUser;
            this.faces = faces;
        }

        @Override
        public Float loadInBackground() {
            // Record the current security level settings
            VerID.SecurityLevel securityLevel = VerID.shared.getSecurityLevel();
            // ID card face comparison works best with the lowest security level
            VerID.shared.setSecurityLevel(VerID.SecurityLevel.LOWEST);
            float score = 0;
            try {
                score = VerID.shared.compareUserToFaces(cardUser, faces);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Restore the previous security level
            VerID.shared.setSecurityLevel(securityLevel);
            return score;
        }
    }

    /**
     * Loader that registers the face on the ID card so that it can be compared to the live selfie faces
     */
    private static class RegistrationLoader extends AsyncTaskLoader<VerIDUser> {

        private FBFace face;

        public RegistrationLoader(Context context, FBFace face) {
            super(context);
            this.face = face;
        }

        @Override
        public VerIDUser loadInBackground() {
            VerIDUser user;
            try {
                user = VerID.shared.registerUserWithFace("cardUser", face, false);
            } catch (Exception e) {
                e.printStackTrace();
                user = null;
            }
            return user;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: Obtain idCaptureResult and livenessDetectionResult, e.g. as intent extras
        if (livenessDetectionResult != null && idCaptureResult != null && idCaptureResult.getFace() != null && intent != null) {
            // Check whether the face on the captured ID card is being processed
            if (idCaptureResult.getRegisteredUser() == null && idCaptureResult.getFace().isBackgroundProcessing()) {
                // The ID card face is being processed. Listen for face template extraction events
                VerID.shared.getFaceTemplateExtraction().addListener(idCaptureResult.getFace().getId(), this);
            } else {
                initScoreLoaderIfReady();
            }
        } else {
            updateScore(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getLoaderManager().destroyLoader(LOADER_ID_SCORE);
        getLoaderManager().destroyLoader(LOADER_ID_REGISTRATION);
        if (idCaptureResult != null && idCaptureResult.getFace() != null && idCaptureResult.getFace().isBackgroundProcessing()) {
            VerID.shared.getFaceTemplateExtraction().removeListener(idCaptureResult.getFace().getId(), this);
        }
    }

    private void initScoreLoaderIfReady() {
        if (idCaptureResult.getRegisteredUser() != null) {
            // The face on the ID card is registered, calculate the similarity score
            getSupportLoaderManager().initLoader(LOADER_ID_SCORE, null, this).forceLoad();
        } else if (idCaptureResult.getFace().isSuitableForRecognition()) {
            // Register user on the ID card
            getSupportLoaderManager().initLoader(LOADER_ID_REGISTRATION, null, this).forceLoad();
        }
    }

    @Override
    public void onFaceTemplateExtracted(long faceId, FBFace face) {
        VerID.shared.getFaceTemplateExtraction().removeListener(faceId, this);
        if (idCaptureResult.getFace() != null && idCaptureResult.getFace().isBackgroundProcessing() && idCaptureResult.getFace().getId() == faceId) {
            // Face template has been extracted from the face in the ID card. The face is now ready to be registered
            idCaptureResult.setFace(face);
            CardCaptureResultPersistence.saveCardCaptureResult(this, idCaptureResult);
            initScoreLoaderIfReady();
        }
    }

    @Override
    public void onFaceTemplateExtractionProgress(long faceId, double progress) {
    }

    @Override
    public void onFaceTemplateExtractionFailed(long faceId, Exception exception) {
        VerID.shared.getFaceTemplateExtraction().removeListener(faceId, this);
        updateScore(null);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_REGISTRATION:
                if (idCaptureResult.getFace() != null && idCaptureResult.getFace().isSuitableForRecognition()) {
                    return new RegistrationLoader(this, idCaptureResult.getFace());
                } else {
                    updateScore(null);
                    return null;
                }
            case LOADER_ID_SCORE:
                if (idCaptureResult.getRegisteredUser() != null) {
                    return new ScoreLoader(this, idCaptureResult.getRegisteredUser(), livenessDetectionResult.getFacesSuitableForRecognition());
                } else {
                    updateScore(null);
                    return null;
                }
            default:
                return null;

        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        switch (loader.getId()) {
            case LOADER_ID_REGISTRATION:
                if (data != null && (data instanceof VerIDUser)) {
                    VerIDUser user = (VerIDUser) data;
                    idCaptureResult.setRegisteredUser(user);
                    CardCaptureResultPersistence.saveCardCaptureResult(this, idCaptureResult);
                    initScoreLoaderIfReady();
                } else {
                    updateScore(null);
                }
                break;
            case LOADER_ID_SCORE:
                Float score = (Float) data;
                updateScore(score);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
    }

    @UiThread
    private void updateScore(Float score) {
        if (score != null) {
            // Display score
        } else {
            // Display error
        }
    }
}
~~~