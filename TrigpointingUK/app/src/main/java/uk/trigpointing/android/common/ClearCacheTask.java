package uk.trigpointing.android.common;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import uk.trigpointing.android.DbHelper;
import uk.trigpointing.android.DownloadTrigsActivity;
import uk.trigpointing.android.logging.SyncTask;
import uk.trigpointing.android.logging.SyncListener;

public class ClearCacheTask implements SyncListener {
	public static final String TAG ="ClearCacheTask";
	private final Context mCtx;
    // Replaced deprecated ProgressDialog with lightweight toasts
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final boolean isCancelled = false;

	
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

		cache = new FileCache(mCtx, "strings");
		c += cache.clear();

        // Clear static map images cache (used by TrigDetails OS Map Tab)
		cache = new FileCache(mCtx, "map_images");
		c += cache.clear();

        // Clear WebView tiles cache (used by bulk download functionality)
		cache = new FileCache(mCtx, "webview_tiles");
		c += cache.clear();

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
		
		// Notify start on main thread (ProgressDialog is deprecated)
		mainHandler.post(() -> Toast.makeText(mCtx, "Clearing cache…", Toast.LENGTH_SHORT).show());
		
		// Execute background work
		CompletableFuture.supplyAsync(this::doInBackground, executor)
			.thenAcceptAsync(result -> {
				Log.d(TAG, "onPostExecute " + result);
				if (!isCancelled) {
					Toast.makeText(mCtx, "Cleared "+result+" cache files. Starting trigpoint download...", Toast.LENGTH_LONG).show();
					
					// Automatically trigger trigpoint data download
					triggerTrigpointDownload();
				} else {
					Toast.makeText(mCtx, "Cancelled!", Toast.LENGTH_LONG).show();					
				}
				// No modal dialog to dismiss
			}, mainHandler::post);
    }

	private void triggerTrigpointDownload() {
		Log.d(TAG, "triggerTrigpointDownload: Starting DownloadTrigsActivity");
		
		// Launch DownloadTrigsActivity which will download trigpoint data
		Intent intent = new Intent(mCtx, DownloadTrigsActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Required when starting from non-Activity context
		mCtx.startActivity(intent);
	}
	
	// SyncListener interface implementation
	@Override
	public void onSynced(int status) {
		Log.d(TAG, "onSynced: User data sync completed with status " + status);
		
		switch (status) {
			case SyncTask.SUCCESS:
				Toast.makeText(mCtx, "User data sync completed successfully", Toast.LENGTH_SHORT).show();
				break;
			case SyncTask.ERROR:
				Toast.makeText(mCtx, "User data sync failed", Toast.LENGTH_SHORT).show();
				break;
			case SyncTask.NOROWS:
				Toast.makeText(mCtx, "No user data to sync", Toast.LENGTH_SHORT).show();
				break;
			default:
				Toast.makeText(mCtx, "User data sync completed", Toast.LENGTH_SHORT).show();
				break;
		}
	}
}