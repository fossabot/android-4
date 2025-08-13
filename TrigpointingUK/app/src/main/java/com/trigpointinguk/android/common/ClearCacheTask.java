package com.trigpointinguk.android.common;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.trigpointinguk.android.DbHelper;



public class ClearCacheTask {
	public static final String TAG ="ClearCacheTask";
	private Context mCtx;
    private ProgressDialog mProgressDialog;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isCancelled = false;

	
    public ClearCacheTask(Context pCtx) {
		this.mCtx = pCtx;
	}
	
	private Integer doInBackground() {
		Log.d(TAG, "doInBackground");
		FileCache cache;
		int c = 0;
		
		cache = new FileCache(mCtx, "bitmaps");
		c += cache.clear();
		if (isCancelled){return 0;}
		
		cache = new FileCache(mCtx, "images");
		c += cache.clear();
		if (isCancelled){return 0;}
		
		cache = new FileCache(mCtx, "strings");
		c += cache.clear();
		if (isCancelled){return 0;}
		
		// Clear static map images cache (used by TrigDetails OS Map Tab)
		cache = new FileCache(mCtx, "map_images");
		c += cache.clear();
		if (isCancelled){return 0;}
		
		// Clear WebView tiles cache (used by bulk download functionality)
		cache = new FileCache(mCtx, "webview_tiles");
		c += cache.clear();
		if (isCancelled){return 0;}
		
		// Also delete the database
		try {
			DbHelper db = new DbHelper(mCtx);
			db.deleteDatabase();
			Log.d(TAG, "doInBackground: Database deleted");
		} catch (Exception e) {
			Log.e(TAG, "doInBackground: Error deleting database", e);
		}
		
		return c;
	}
    public void execute() {
		Log.d(TAG, "execute");
		
		// Show progress dialog on main thread
		mainHandler.post(() -> {
			mProgressDialog = new ProgressDialog(mCtx);
			mProgressDialog.setMessage("Clearing cache");
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setCancelable(false);
			mProgressDialog.show();
		});
		
		// Execute background work
		CompletableFuture.supplyAsync(this::doInBackground, executor)
			.thenAcceptAsync(result -> {
				Log.d(TAG, "onPostExecute " + result);
				if (!isCancelled) {
					Toast.makeText(mCtx, "Cleared "+result+" cache files", Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(mCtx, "Cancelled!", Toast.LENGTH_LONG).show();					
				}
				if (mProgressDialog != null) {mProgressDialog.dismiss();}
			}, mainHandler::post);
    }
    
    public void cancel() {
		isCancelled = true;
		executor.shutdown();
	}
}