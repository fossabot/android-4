package com.trigpointinguk.android.mapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

// import org.acra.ErrorReporter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.trigpointinguk.android.R;

public class DownloadMapsActivity extends Activity {
	private TextView mStatus;
	private ProgressBar mProgress;
	private Button mDownloadBtn;
	private Spinner mTileSource;
	private RadioGroup mCacheTypeGroup;
	private CheckBox mOsmDroidCacheCheck;
	private CheckBox mWebViewCacheCheck;
	private int mProgressMax=100;
	private int mDownloadCount;
	private boolean mRunning=false;
	private static final String TAG = "DownloadMapsActivity";
	private static final int STATUS_OK 			= 0;
	private static final int STATUS_CORRUPT		= 1;
	private static final int STATUS_CANCELLED	= 2;
	private static final int STATUS_NOTFOUND	= 3;
	private static final int STATUS_NOSPACE		= 4;
	private static final int STATUS_IOERROR		= 5;
	
	// Cache types
	private static final int CACHE_OSMDROID = 1;
	private static final int CACHE_WEBVIEW = 2;
	private static final int CACHE_BOTH = 3;
	
	private CompletableFuture<Integer> mTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mapdownload);

		// Enable back button in action bar
		if (getActionBar() != null) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}

		mStatus 		= (TextView)	findViewById(R.id.downloadMapsStatus);
		mProgress 		= (ProgressBar)	findViewById(R.id.downloadMapsProgress);
		mDownloadBtn	= (Button)		findViewById(R.id.btnDownloadMaps);
		mTileSource 	= (Spinner)		findViewById(R.id.downloadMapsTileSource);
		mOsmDroidCacheCheck = (CheckBox) findViewById(R.id.osmDroidCacheCheck);
		mWebViewCacheCheck = (CheckBox) findViewById(R.id.webViewCacheCheck);  

		mStatus.setText(R.string.downloadMapsIdleStatus);
		mProgress.setProgress(0);

		mDownloadBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (!mRunning) {
					// Check which caches are selected
					boolean osmDroidSelected = mOsmDroidCacheCheck.isChecked();
					boolean webViewSelected = mWebViewCacheCheck.isChecked();
					
					if (!osmDroidSelected && !webViewSelected) {
						new AlertDialog.Builder(DownloadMapsActivity.this)
							.setTitle("No Cache Selected")
							.setMessage("Please select at least one cache destination.")
							.setPositiveButton("OK", null)
							.show();
						return;
					}
					
					// Determine cache type
					int cacheType = 0;
					if (osmDroidSelected) cacheType |= CACHE_OSMDROID;
					if (webViewSelected) cacheType |= CACHE_WEBVIEW;
					
					// find which item is selected
					final int tilePos = mTileSource.getSelectedItemPosition();
					// get the list of URLs
					final String tileURL = getResources().getStringArray(R.array.downloadMapsArrayValues)[tilePos];
					// get the number of tiles
					mProgressMax = getResources().getIntArray(R.array.downloadMapsArrayTiles)[tilePos];
					Log.i(TAG, "Downloading from: " + tileURL);
					Log.i(TAG, "Expected tiles: " + mProgressMax);
					Log.i(TAG, "Cache type: " + cacheType);
					mTask = downloadMaps(tileURL, cacheType);
				} else {
					mTask.cancel(true);
				}
			}
		});       
	}

	
	


	@Override
	protected void onDestroy() {
		Log.i(TAG, "Destroyed");
		if (mTask != null && !mTask.isDone()) {
			mTask.cancel(true);
		}
		super.onDestroy();
	}





	private CompletableFuture<Integer> downloadMaps(String tileURL, int cacheType) {
		// Setup UI on main thread
		mStatus.setText(R.string.downloadMapsInsertStatus);
		mDownloadBtn.setText(R.string.btnCancel);
		mTileSource.setEnabled(false);
		mOsmDroidCacheCheck.setEnabled(false);
		mWebViewCacheCheck.setEnabled(false);
		mProgress.setMax(mProgressMax);
		mProgress.setProgress(0);
		mDownloadCount = 0;
		mRunning = true;
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Handler mainHandler = new Handler(Looper.getMainLooper());
		
				return CompletableFuture.supplyAsync(() -> {
			// Determine cache directories
			String osmDroidCacheDir = Environment.getExternalStorageDirectory().getPath() + "/osmdroid/tiles/";
			String webViewCacheDir = getFilesDir().getPath() + "/webview_tiles/";
			
			Log.d(TAG, "OSMdroid cache: " + osmDroidCacheDir);
			Log.d(TAG, "WebView cache: " + webViewCacheDir);
			Log.d(TAG, "Cache type flags: " + cacheType);
			
			int i=0; // not using mDownloadcount in loop for performance reasons

			String[] files = tileURL.split(",");
			for (String file : files) {
				try {
					Log.i(TAG, "Getting : " + file);
					URL url = new URL(file);
					URLConnection ucon = url.openConnection();
					InputStream is = ucon.getInputStream();
					ZipInputStream zis = new ZipInputStream(is); 
					ZipEntry ze; 

					byte[] buffer = new byte[1024];
					int length;

					while ((ze = zis.getNextEntry()) != null) { 
						Log.v(TAG, "Unzipping " + ze.getName()); 

						if(ze.isDirectory()) {
							// Create directories in selected caches
							if ((cacheType & CACHE_OSMDROID) != 0) {
								dirChecker(osmDroidCacheDir + ze.getName()); 
							}
							if ((cacheType & CACHE_WEBVIEW) != 0) {
								dirChecker(webViewCacheDir + ze.getName()); 
							}
						} else {
							// Read tile data once
							byte[] tileData = readZipEntryData(zis);
							zis.closeEntry();
							
							// Write to selected caches
							if ((cacheType & CACHE_OSMDROID) != 0) {
								writeToCache(osmDroidCacheDir + ze.getName(), tileData);
							}
							if ((cacheType & CACHE_WEBVIEW) != 0) {
								writeToCache(webViewCacheDir + ze.getName(), tileData);
							}
							
							i++;
						} 
						if (i % 10 == 0) {
							// Update progress on main thread
							final int progress = i;
							mainHandler.post(() -> {
								mProgress.setProgress(progress);
								String cacheInfo = "";
								if ((cacheType & CACHE_OSMDROID) != 0 && (cacheType & CACHE_WEBVIEW) != 0) {
									cacheInfo = " (both caches)";
								} else if ((cacheType & CACHE_OSMDROID) != 0) {
									cacheInfo = " (OSMdroid cache)";
								} else if ((cacheType & CACHE_WEBVIEW) != 0) {
									cacheInfo = " (WebView cache)";
								}
								mStatus.setText("Downloaded " + progress + " tiles" + cacheInfo);
							});
						}
					} 
					zis.close(); 
					mDownloadCount=i;
				} 
				catch (ZipException e) {
					Log.w(TAG, "Error: " + e);
					// ErrorReporter.getInstance().handleSilentException(e); // ACRA temporarily disabled
					mDownloadCount = i;
					return STATUS_CORRUPT;					
				}
				catch (FileNotFoundException e) {
					Log.w(TAG, "Error: " + e);
					// ErrorReporter.getInstance().handleSilentException(e); // ACRA temporarily disabled
					mDownloadCount = i;
					return STATUS_NOTFOUND;										
				}
				catch (IOException e) {
					Log.w(TAG, "Error: " + e);
					Log.w(TAG, "Message: " + e.getMessage());
					Log.w(TAG, "Cause: " + e.getCause());
					// ErrorReporter.getInstance().handleSilentException(e); // ACRA temporarily disabled
					mDownloadCount = i;
					if (e.getMessage().equals("No space left on device")) {
						return STATUS_NOSPACE;
					}
					return STATUS_IOERROR;
				}
			}
			return STATUS_OK;
		}, executor)
		.thenApplyAsync(result -> {
			mProgress.setProgress(mDownloadCount);
			switch (result) {
			case STATUS_OK:
				mStatus.setText("Finished downloading " + mDownloadCount +" tiles");
				break;
			case STATUS_CORRUPT:
				mStatus.setText("Error - corrupt file! " + mDownloadCount +" tiles");
				break;
			case STATUS_NOTFOUND:
				mStatus.setText("Error - file not found! " + mDownloadCount +" tiles");
				break;
			case STATUS_CANCELLED:
				mStatus.setText("Download Cancelled! " + mDownloadCount +" tiles");
				break;
			case STATUS_NOSPACE:
				mStatus.setText("Error - out of space! " + mDownloadCount +" tiles");
				break;
			case STATUS_IOERROR:
				mStatus.setText("Error - I/O failed! " + mDownloadCount +" tiles");
				break;
			default:
				mStatus.setText("Error downloading! " + mDownloadCount + " tiles");
			}
			mDownloadBtn.setText(R.string.btnDownload);
			mTileSource.setEnabled(true);
			mOsmDroidCacheCheck.setEnabled(true);
			mWebViewCacheCheck.setEnabled(true);
			mRunning = false;
			return result;
		}, mainHandler::post);
	}
	
	private byte[] readZipEntryData(ZipInputStream zis) throws IOException {
		byte[] buffer = new byte[1024];
		byte[] result = new byte[0];
		int length;
		
		while ((length = zis.read(buffer)) > 0) {
			byte[] newResult = new byte[result.length + length];
			System.arraycopy(result, 0, newResult, 0, result.length);
			System.arraycopy(buffer, 0, newResult, result.length, length);
			result = newResult;
		}
		
		return result;
	}
	
	private void writeToCache(String filePath, byte[] data) throws IOException {
		File file = new File(filePath);
		file.getParentFile().mkdirs(); // Ensure directory exists
		
		try (FileOutputStream fout = new FileOutputStream(file)) {
			fout.write(data);
		}
	}
	
	private void dirChecker(String dir) { 
		File f = new File(dir); 
		if(!f.isDirectory()) { 
			f.mkdirs(); 
		} 
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			// Handle back button in action bar
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
