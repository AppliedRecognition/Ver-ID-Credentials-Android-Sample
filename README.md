# Ver-ID ID Capture
The Ver-ID ID Capture SDK allows your app to capture an image of the user's ID card and read the PDF 417 barcode on the back of the card. You can use Ver-ID to compare the face on the front of the ID card to a live selfie.

## Adding Ver-ID ID Capture in Your Android Studio Project

1. Obtain an API key and secret for your app at [dev.ver-id.com](https://dev.ver-id.com).
1. Open your project's **gradle.build** file and under `allprojects/repositories` add

	```
	maven {
		url 'https://dev.ver-id.com/artifactory/gradle-release'
	}
	```
1. Open your app module's **build.gradle** file and under `dependencies` add

	```
	compile 'com.appliedrec:shared:1.8'
	compile 'com.appliedrec:det-rec-lib:1.8'
	compile 'com.appliedrec:ver-id:1.8'
	compile 'com.appliedrec:id-capture:1.8'
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

~~~java
public class CaptureResultActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks {

    // Loader IDs
    private static final int LOADER_ID_SCORE = 458;
    private static final int LOADER_ID_CARD_FACE = 86;
    private static final int LOADER_ID_LIVE_FACE = 355;

    // Extracted card and face images
    private Bitmap cardFaceImage;
    private Bitmap liveFaceImage;

    /**
     * Loader that compares the face from the card with the live face
     */
    private static class ScoreLoader extends AsyncTaskLoader<Float> {

        private final Bitmap image1;
        private final Bitmap image2;

        public ScoreLoader(Context context, Bitmap image1, Bitmap image2) {
            super(context);
            this.image1 = image1;
            this.image2 = image2;
        }

        @Override
        public Float loadInBackground() {
            try {
                double score = VerID.shared.compareFacesInImages(image1, image2);
                return (float) score;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Loader that loads an image and crops it to the given bounds
     */
    private static class ImageLoader extends AsyncTaskLoader<Bitmap> {

        private Uri imageUri;
        private Rect faceBounds;

        public ImageLoader(Context context, Uri imageUri, Rect faceBounds) {
            super(context);
            this.imageUri = imageUri;
            this.faceBounds = faceBounds;
        }

        @Override
        public Bitmap loadInBackground() {
            try {
                InputStream inputStream = getContext().getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (bitmap != null) {
                    Rect imageRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
                    // Add a bit of space around the face for better detection
                    faceBounds.inset(0-(int)((double)faceBounds.width()*0.1), 0-(int)((double)faceBounds.height()*0.1));
                    // Ensure the face is contained within the bounds of the image
                    //noinspection CheckResult
                    imageRect.intersect(faceBounds);
                    return Bitmap.createBitmap(bitmap, imageRect.left, imageRect.top, imageRect.width(), imageRect.height());
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the cropped face images
        getSupportLoaderManager().initLoader(LOADER_ID_CARD_FACE, intent.getExtras(), this).forceLoad();
        getSupportLoaderManager().initLoader(LOADER_ID_LIVE_FACE, intent.getExtras(), this).forceLoad();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getLoaderManager().destroyLoader(LOADER_ID_CARD_FACE);
        getLoaderManager().destroyLoader(LOADER_ID_LIVE_FACE);
        getLoaderManager().destroyLoader(LOADER_ID_SCORE);
    }

    private void initScoreLoader() {
        if (cardFaceImage != null && liveFaceImage != null) {
            getSupportLoaderManager().initLoader(LOADER_ID_SCORE, null, this).forceLoad();
        }
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_CARD_FACE:
                Uri cardImageUri = idCaptureResult.getFrontImageUri();
                Rect cardImageFaceBounds = new Rect();
                idCaptureResult.getFaceBounds().round(cardImageFaceBounds);
                return new ImageLoader(this, cardImageUri, cardImageFaceBounds);
            case LOADER_ID_LIVE_FACE:
                Uri faceImageUri = livenessDetectionResult.getImageUris(VerID.Bearing.STRAIGHT)[0];
                Rect faceImageFaceBounds = livenessDetectionResult.getFaceBounds(VerID.Bearing.STRAIGHT)[0];
                return new ImageLoader(this, faceImageUri, faceImageFaceBounds);
            case LOADER_ID_SCORE:
                if (liveFaceImage != null && cardFaceImage != null) {
                    return new ScoreLoader(this, liveFaceImage, cardFaceImage);
                } else {
                    return null;
                }
            default:
                return null;

        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        switch (loader.getId()) {
            case LOADER_ID_CARD_FACE:
                if (data != null) {
                    cardFaceImage = (Bitmap) data;
                    initScoreLoader();
                }
                break;
            case LOADER_ID_LIVE_FACE:
                if (data != null) {
                    liveFaceImage = (Bitmap) data;
                    initScoreLoader();
                }
                break;
            case LOADER_ID_SCORE:
                Float score = (Float) data;
                /**
                 * We now have the likeness score.
                 */
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
        
    }
}
~~~