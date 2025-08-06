package com.trigpointinguk.android.common;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sonyericsson.zoom.DynamicZoomControl;
import com.sonyericsson.zoom.ImageZoomView;
import com.sonyericsson.zoom.LongPressZoomListener;
import com.trigpointinguk.android.R;

public class DisplayBitmapActivity extends Activity {
	private static final String TAG="DisplayBitmapActivity";

    private static final int MENU_ID_RESET = 0;
    private ImageZoomView 			mZoomView;
    private DynamicZoomControl 		mZoomControl;
    private Bitmap 					mBitmap;
    private LongPressZoomListener 	mZoomListener;
    private String					mUrl;
    private BitmapLoader 			mBitmapLoader; 
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.displaybitmap);

        // get URL from extras
        Bundle extras = getIntent().getExtras();
		if (extras == null) {return;}
		mUrl = extras.getString("URL");
		Log.i(TAG, "Loading URL: "+mUrl);

        // setup zoom control
        mZoomControl = new DynamicZoomControl();
        mZoomListener = new LongPressZoomListener(getApplicationContext());
        mZoomListener.setZoomControl(mZoomControl);
        
        mZoomView = (ImageZoomView)findViewById(R.id.zoomview);
        mZoomView.setZoomState(mZoomControl.getZoomState());
        mZoomView.setOnTouchListener(mZoomListener);
        
        mZoomView.setImage(BitmapFactory.decodeResource(getResources(), R.drawable.imageloading));
        mZoomControl.setAspectQuotient(mZoomView.getAspectQuotient());
        resetZoomState();

		mBitmapLoader = new BitmapLoader(this);
		loadBitmap();
    
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBitmap != null) {mBitmap.recycle();}
        mZoomView.setOnTouchListener(null);
        mZoomControl.getZoomState().deleteObservers();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_ID_RESET, 2, R.string.reset);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ID_RESET:
                resetZoomState();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Reset zoom state and notify observers
     */
    private void resetZoomState() {
        mZoomControl.getZoomState().setPanX(0.5f);
        mZoomControl.getZoomState().setPanY(0.5f);
        mZoomControl.getZoomState().setZoom(1f);
        mZoomControl.getZoomState().notifyObservers();
    }


    
    
    
	private void loadBitmap() {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Handler mainHandler = new Handler(Looper.getMainLooper());
		
		CompletableFuture.supplyAsync(() -> {
			// download photo, or obtain from cache
			return mBitmapLoader.getBitmap(mUrl, false);
		}, executor)
		.thenAcceptAsync(bitmap -> {
			mBitmap = bitmap;
			mZoomView.setImage(mBitmap);
			resetZoomState();
		}, mainHandler::post);
	}    
    
    
}
