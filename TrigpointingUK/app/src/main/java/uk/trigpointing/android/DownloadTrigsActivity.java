package uk.trigpointing.android;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

import uk.trigpointing.android.common.BaseActivity;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import uk.trigpointing.android.logging.SyncTask;
import uk.trigpointing.android.logging.SyncListener;
import uk.trigpointing.android.types.Condition;
import uk.trigpointing.android.types.Trig;
import android.content.Intent;

public class DownloadTrigsActivity extends BaseActivity implements SyncListener {

	private TextView 		mStatus;
	private ProgressBar 	mProgress;
	private Integer 		mDownloadCount = 0;
    private static int 		mProgressMax = 10000; // value unimportant
	private int 			mAppVersion;
	private Handler 		mainHandler;
	// Retry/backoff state
	private int 			retryDelaySeconds = 5; // initial delay for exponential backoff
	private int 			countdownRemainingSeconds = 0;
	private Runnable 		countdownRunnable;
	
	private static final String TAG = "DownloadTrigsActivity";
	private enum DownloadStatus {OK, CANCELLED, ERROR}


    @SuppressLint("SetTextI18n")
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
		
		mStatus = findViewById(R.id.downloadStatus);
		mStatus.setText("Starting download...");
		mProgress = findViewById(R.id.downloadProgress);
		mProgress.setMax(mProgressMax);
		mProgress.setProgress(0);
		
		// Initialize main handler
		mainHandler = new Handler(Looper.getMainLooper());
		
		// Automatically start the download
        CompletableFuture<DownloadStatus> mTask = downloadTrigs();

	}


	@SuppressLint("SetTextI18n")
    private CompletableFuture<DownloadStatus> downloadTrigs() {
		// Setup UI on main thread
		mDownloadCount = 0;
		mStatus.setText("Downloading trigpoint data...");

        ExecutorService executor = Executors.newSingleThreadExecutor();
		
		return CompletableFuture.supplyAsync(() -> {
			Log.i(TAG, "PopulateTrigsTask: Starting download");

			DbHelper db = new DbHelper(DownloadTrigsActivity.this);
			String strLine;                
			int i=0;

			try {
				Log.i(TAG, "PopulateTrigsTask: Opening database");
				db.open();
				db.mDb.beginTransaction();

				String downloadUrl = "https://trigpointing.uk/trigs/down-android-trigs.php?appversion="+ mAppVersion;
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
					mProgressMax = Integer.parseInt(strLine);
					Log.i(TAG, "PopulateTrigsTask: Downloading " + mProgressMax + " trigs");
					// Update progress on main thread
					mainHandler.post(() -> mProgress.setMax(mProgressMax));
				}
				
				Log.i(TAG, "PopulateTrigsTask: Deleting all existing data");
				db.deleteAll();

				while ((strLine = br.readLine()) != null && !strLine.trim().isEmpty())   {
					//Log.v(TAG,strLine);
					String[] csv=strLine.split("\t");
					
					// Validate CSV array has enough elements
					if (csv.length < 10) {
						Log.w(TAG, "Skipping invalid line (insufficient columns): " + strLine);
						continue;
					}
					
					try {
						int id						= Integer.parseInt(csv[0]);
						String waypoint				= csv[1];
						String name					= csv[2];
						
						// Validate lat/lon are not empty
						if (csv[3].trim().isEmpty() || csv[4].trim().isEmpty()) {
							Log.w(TAG, "Skipping line with empty lat/lon: " + strLine);
							continue;
						}
						
						double lat					= Double.parseDouble(csv[3]);
						double lon					= Double.parseDouble(csv[4]);
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
                    } catch (Exception e) {
						Log.w(TAG, "Skipping line with parsing error: " + strLine + " - " + e.getMessage());
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
				mStatus.setText("Download complete! " + mDownloadCount + " trigpoints downloaded. Starting sync...");
				mProgress.setProgress(mProgressMax);
				// Start sync after successful download with auto-sync flag
				mainHandler.post(() -> new SyncTask(DownloadTrigsActivity.this, DownloadTrigsActivity.this).execute(true));
				break;
			case ERROR:
				mProgress.setProgress(0);
				// Schedule retry with exponential backoff and per-second countdown
				scheduleRetryWithCountdown();
				break;
			case CANCELLED:
				mStatus.setText("Download cancelled.");
				mProgress.setProgress(0);
				// Auto-close after 3 seconds
				mainHandler.postDelayed(DownloadTrigsActivity.this::finish, 3000);
				break;
			}
            return result;
		}, mainHandler::post);
	}

	private void scheduleRetryWithCountdown() {
		// Initialize countdown
		countdownRemainingSeconds = retryDelaySeconds;
		updateRetryStatusText();

		// Cancel any existing countdown before starting a new one
		if (countdownRunnable != null) {
			mainHandler.removeCallbacks(countdownRunnable);
		}

		countdownRunnable = new Runnable() {
			@Override
			public void run() {
				countdownRemainingSeconds--;
				if (countdownRemainingSeconds > 0) {
					updateRetryStatusText();
					mainHandler.postDelayed(this, 1000);
				} else {
					// Time to retry now
					mStatus.setText(getString(R.string.retrying_download));
					// Increase delay for the next potential retry (exponential backoff with 300s cap)
					retryDelaySeconds = Math.min(retryDelaySeconds * 2, 300);
					// Start the download again
					downloadTrigs();
				}
			}
		};

		mainHandler.postDelayed(countdownRunnable, 1000);
	}

	@SuppressLint("SetTextI18n")
	private void updateRetryStatusText() {
		mStatus.setText("Download failed!  Will retry in " + countdownRemainingSeconds + " seconds");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Clean up any pending countdown callbacks to avoid leaks
		if (mainHandler != null && countdownRunnable != null) {
			mainHandler.removeCallbacks(countdownRunnable);
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
	
	@SuppressLint("SetTextI18n")
    @Override
	public void onSynced(int status) {
		Log.i(TAG, "onSynced: Sync completed with status: " + status);
		
		mainHandler.post(() -> {
			Log.i(TAG, "onSynced: Updating UI and scheduling return to MainActivity");
			switch (status) {
			case SyncTask.SUCCESS:
				mStatus.setText("Sync complete! All data synchronised.");
				break;
			case SyncTask.NOROWS:
				mStatus.setText("Sync complete! No data to sync.");
				break;
			case SyncTask.ERROR:
				// For first-time users, this is expected - show appropriate message
				mStatus.setText("Sync skipped - Please login for a more personalised experience.");
				break;
			case SyncTask.CANCELLED:
				mStatus.setText("Sync cancelled.");
				break;
			default:
				mStatus.setText("Sync completed with unknown status.");
				break;
			}
			
			Log.i(TAG, "onSynced: Scheduling return to MainActivity in 1.5 seconds");
			// Return to MainActivity after a shorter delay (1.5 seconds instead of 3)
			mainHandler.postDelayed(() -> {
				Log.i(TAG, "onSynced: Returning to MainActivity now");
				finish();
				// Ensure we return to MainActivity
				Intent intent = new Intent(DownloadTrigsActivity.this, MainActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}, 1500);
		});
	}
}
