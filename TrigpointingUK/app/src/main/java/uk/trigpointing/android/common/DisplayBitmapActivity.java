package uk.trigpointing.android.common;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import uk.trigpointing.android.common.BaseActivity;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import uk.trigpointing.android.R;

public class DisplayBitmapActivity extends BaseActivity {
	private static final String TAG="DisplayBitmapActivity";

    private static final int MENU_ID_RESET = 0;
    private ZoomableImageView 		mZoomableImageView;
    private Bitmap 					mBitmap;
    private String					mUrl;
    private BitmapLoader 			mBitmapLoader; 
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.displaybitmap);

        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // get URL from extras
        Bundle extras = getIntent().getExtras();
		if (extras == null) {return;}
		mUrl = extras.getString("URL");
		Log.i(TAG, "Loading URL: "+mUrl);

        // Setup ZoomableImageView
        mZoomableImageView = findViewById(R.id.zoomable_image_view);
        
        // Set loading image
        mZoomableImageView.setImageResource(R.drawable.imageloading);
        mBitmapLoader = new BitmapLoader(this);
		loadBitmap();
    
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBitmap != null) {mBitmap.recycle();}
        // PhotoView handles its own cleanup
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_ID_RESET, 2, R.string.reset);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button in action bar
            finish();
            return true;
        }
        
        switch (item.getItemId()) {
            case MENU_ID_RESET:
                resetZoomState();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Reset zoom state to fit image to screen
     */
    private void resetZoomState() {
        if (mZoomableImageView != null) {
            mZoomableImageView.resetZoom();
        }
    }


    
    
    
	private void loadBitmap() {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Handler mainHandler = new Handler(Looper.getMainLooper());
		
		CompletableFuture.supplyAsync(() -> {
			// Check if it's a local file path or HTTP URL
			if (mUrl.startsWith("/") || mUrl.startsWith("file://")) {
				// Load from local file directly
				String filePath = mUrl.startsWith("file://") ? mUrl.substring(7) : mUrl;
				Log.d(TAG, "Loading local file: " + filePath);
				
				try {
					Bitmap bitmap = BitmapFactory.decodeFile(filePath);
					if (bitmap != null) {
						Log.d(TAG, "Successfully loaded local file");
						return bitmap;
					} else {
						Log.w(TAG, "Failed to decode local file: " + filePath);
					}
				} catch (Exception e) {
					Log.e(TAG, "Error loading local file: " + filePath, e);
				}
				return null;
			} else {
				// Download photo from HTTP URL, or obtain from cache
				Log.d(TAG, "Loading HTTP URL: " + mUrl);
				return mBitmapLoader.getBitmap(mUrl, false);
			}
		}, executor)
		.thenAcceptAsync(bitmap -> {
			if (bitmap != null) {
				mBitmap = bitmap;
				mZoomableImageView.setImageBitmap(mBitmap);
				Log.d(TAG, "Successfully set bitmap to ZoomableImageView");
			} else {
				Log.e(TAG, "Failed to load bitmap from: " + mUrl);
				// Keep the loading placeholder image
			}
		}, mainHandler::post);
	}    
    
    
}
