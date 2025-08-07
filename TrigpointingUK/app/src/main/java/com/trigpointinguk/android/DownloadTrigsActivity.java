package com.trigpointinguk.android;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

import androidx.appcompat.app.AppCompatActivity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.trigpointinguk.android.types.Condition;
import com.trigpointinguk.android.types.Trig;

public class DownloadTrigsActivity extends AppCompatActivity {

	private TextView 		mStatus;
	private ProgressBar 	mProgress;
	private Button 			mDownloadBtn;
	private Integer 		mDownloadCount = 0;
	private boolean 		mRunning = false;
	private static int 		mProgressMax = 10000; // value unimportant
	private int 			mAppVersion;
	
	private static final String TAG = "DownloadTrigsActivity";
	private enum DownloadStatus {OK, CANCELLED, ERROR};
	private enum ProgressType 	{UPDATE, NEWMAX};
	private CompletableFuture<DownloadStatus> mTask;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download);
		
		// Enable back button in action bar
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}
		
		try {
			mAppVersion = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			Log.e(TAG,"Couldn't get versionCode!");
			mAppVersion = 99999;
		}		
		
		mStatus = (TextView) findViewById(R.id.downloadStatus);
		mStatus.setText(R.string.downloadIdleStatus);
		mProgress = (ProgressBar) findViewById(R.id.downloadProgress);
		mProgress.setMax(mProgressMax);
		mProgress.setProgress(0);
		mDownloadBtn = (Button) findViewById(R.id.btnDownload);

		mDownloadBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (mRunning == false) {
					mTask = downloadTrigs();
				} else {
					mTask.cancel(true);
				}
			}
		});       

	}


	private CompletableFuture<DownloadStatus> downloadTrigs() {
		// Setup UI on main thread
		mDownloadCount = 0;
		mStatus.setText(R.string.downloadInsertStatus);
		mDownloadBtn.setText(R.string.btnCancel);
		mRunning = true;
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Handler mainHandler = new Handler(Looper.getMainLooper());
		
		return CompletableFuture.supplyAsync(() -> {
			Log.i(TAG, "PopulateTrigsTask: Starting download");

			DbHelper db = new DbHelper(DownloadTrigsActivity.this);
			String strLine;                
			int i=0;

			try {
				Log.i(TAG, "PopulateTrigsTask: Opening database");
				db.open();
				db.mDb.beginTransaction();

				String downloadUrl = "https://trigpointing.uk/trigs/down-android-trigs.php?appversion="+String.valueOf(mAppVersion);
				Log.i(TAG, "PopulateTrigsTask: Downloading from URL: " + downloadUrl);
				
				URL url = new URL(downloadUrl);
				Log.i(TAG, "PopulateTrigsTask: Opening connection");
				URLConnection ucon = url.openConnection();
				Log.i(TAG, "PopulateTrigsTask: Getting input stream");
				InputStream is = ucon.getInputStream();
				Log.i(TAG, "PopulateTrigsTask: Creating GZIP input stream");
				GZIPInputStream zis = new GZIPInputStream(new BufferedInputStream(is));
				Log.i(TAG, "PopulateTrigsTask: Creating buffered reader");
				BufferedReader br = new BufferedReader(new InputStreamReader(zis));

				Log.i(TAG, "PopulateTrigsTask: Reading first line");
				if ((strLine = br.readLine()) != null) {
					mProgressMax = Integer.valueOf(strLine);
					Log.i(TAG, "PopulateTrigsTask: Downloading " + mProgressMax + " trigs");
					// Update progress on main thread
					mainHandler.post(() -> {
						mProgress.setMax(mProgressMax);
					});
				}
				
				Log.i(TAG, "PopulateTrigsTask: Deleting all existing data");
				db.deleteAll();

				while ((strLine = br.readLine()) != null && !strLine.trim().equals(""))   {
					//Log.v(TAG,strLine);
					String[] csv=strLine.split("\t");
					
					// Validate CSV array has enough elements
					if (csv.length < 10) {
						Log.w(TAG, "Skipping invalid line (insufficient columns): " + strLine);
						continue;
					}
					
					try {
						int id						= Integer.valueOf(csv[0]);
						String waypoint				= csv[1];
						String name					= csv[2];
						
						// Validate lat/lon are not empty
						if (csv[3].trim().isEmpty() || csv[4].trim().isEmpty()) {
							Log.w(TAG, "Skipping line with empty lat/lon: " + strLine);
							continue;
						}
						
						double lat					= Double.valueOf(csv[3]);
						double lon					= Double.valueOf(csv[4]);
						Trig.Physical type			= Trig.Physical.fromCode(csv[5]);
						String fb					= csv[6];
						Condition condition			= Condition.fromCode(csv[7]);
						Condition logged			= Condition.TRIGNOTLOGGED;
						Trig.Current current		= Trig.Current.fromCode(csv[8]);
						Trig.Historic historic		= Trig.Historic.fromCode(csv[9]);
						db.createTrig(id, name, waypoint, lat, lon, type, condition, logged, current, historic, fb);
						
						if (i++%10==9){
							mDownloadCount=i;
							// Update progress on main thread
							final int progress = i;
							mainHandler.post(() -> {
								mProgress.setProgress(progress);
								mStatus.setText("Inserted " + progress + " trigs");
							});
						}
					} catch (NumberFormatException e) {
						Log.w(TAG, "Skipping line with invalid number format: " + strLine + " - " + e.getMessage());
						continue;
					} catch (Exception e) {
						Log.w(TAG, "Skipping line with parsing error: " + strLine + " - " + e.getMessage());
						continue;
					}
				} 
				db.mDb.execSQL("create index if not exists latlon on trig (lat, lon)");
				db.mDb.setTransactionSuccessful();
			}catch (IOException e) {
				Log.e(TAG, "Error: " + e.getMessage(), e);
				return DownloadStatus.ERROR;
			} catch (Exception e) {
				Log.e(TAG, "Unexpected error: " + e.getMessage(), e);
				return DownloadStatus.ERROR;
			} finally {
				db.mDb.endTransaction();
				db.close();
				mDownloadCount = i;
			}
			return DownloadStatus.OK;
		}, executor)
		.thenApplyAsync(result -> {
			switch (result) {
			case OK:
				mStatus.setText("Finished downloading " + mDownloadCount +" trigs");
				mDownloadBtn.setText(R.string.btnDownloadFinished);
				mProgress.setProgress(mProgressMax);
				mDownloadBtn.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {
							DownloadTrigsActivity.this.finish();
					}
				});       
				break;
			case ERROR:
				mStatus.setText("Download failed!");
				mDownloadBtn.setText(R.string.btnDownload);
				mProgress.setProgress(0);
				break;
			case CANCELLED:
				mStatus.setText("Download Cancelled!");
				mDownloadBtn.setText(R.string.btnDownload);
				mProgress.setProgress(0);
				break;
			}
			mRunning = false;
			return result;
		}, mainHandler::post);
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
