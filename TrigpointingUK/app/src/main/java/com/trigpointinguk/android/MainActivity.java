package com.trigpointinguk.android;

// import org.acra.ErrorReporter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.database.SQLException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.trigpointinguk.android.common.BaseActivity;
import com.trigpointinguk.android.common.ClearCacheTask;
import com.trigpointinguk.android.logging.SyncListener;
import com.trigpointinguk.android.logging.SyncTask;
import com.trigpointinguk.android.mapping.DownloadMapsActivity;
import com.trigpointinguk.android.nearest.NearestActivity;

public class MainActivity extends BaseActivity implements SyncListener {
    public static final String 	TAG ="MainActivity";
    public static final int 	NOTRIGS = 1;
	private static final String RUNBEFORE = "RUNBEFORE";
	private static final String AUTO_SYNC_RUN = "AUTO_SYNC_RUN";
    private SharedPreferences 	mPrefs;
    private ImageView			mPillarIcon;
    private ImageView			mFbmIcon;
    private ImageView			mPassiveIcon;
    private ImageView			mIntersectedIcon;
    private ImageView			mUnsyncedIcon;
    private ImageView			mPhotosIcon;
    private TextView			mPillarCount;
    private TextView			mFbmCount;
    private TextView			mPassiveCount;
    private TextView			mIntersectedCount;
    private TextView			mUnsyncedCount;
    private TextView			mPhotosCount;
    private Button				mSyncBtn;
    private TextView			mUserName;
    
    // Modern activity result launchers
    private ActivityResultLauncher<Intent> nearestLauncher;
    private ActivityResultLauncher<Intent> mapLauncher;
    private ActivityResultLauncher<Intent> searchLauncher;
    private ActivityResultLauncher<Intent> preferencesLauncher;
    
 	@Override
    public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate: Starting MainActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Log.i(TAG, "onCreate: Setting up UI components");
        // Set up UI components
        mPillarIcon = findViewById(R.id.countPillarImage);
        mPillarCount = findViewById(R.id.countPillarText);
        mFbmIcon = findViewById(R.id.countFbmImage);
        mFbmCount = findViewById(R.id.countFbmText);
        mPassiveIcon = findViewById(R.id.countPassiveImage);
        mPassiveCount = findViewById(R.id.countPassiveText);
        mIntersectedIcon = findViewById(R.id.countIntersectedImage);
        mIntersectedCount = findViewById(R.id.countIntersectedText);
        mUnsyncedIcon = findViewById(R.id.countUnsyncedImage);
        mUnsyncedCount = findViewById(R.id.countUnsyncedText);
        mPhotosIcon = findViewById(R.id.countPhotosImage);
        mPhotosCount = findViewById(R.id.countPhotosText);
        mSyncBtn = findViewById(R.id.btnSync);
        mUserName = findViewById(R.id.txtUserName);
        
        Log.i(TAG, "onCreate: Setting up preferences");
        mPrefs = getSharedPreferences("TrigpointingUK", MODE_PRIVATE);
        
        // Reset auto sync flag on app startup
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(AUTO_SYNC_RUN, false).apply();
        Log.i(TAG, "onCreate: Reset auto sync flag for new app session");
        
        Log.i(TAG, "onCreate: Setting up activity result launchers");
        setupActivityResultLaunchers();
        
        Log.i(TAG, "onCreate: Setting up click listeners");
        setupClickListeners();
        
        Log.i(TAG, "onCreate: Updating user display");
        updateUserDisplay();
        
        Log.i(TAG, "onCreate: Populating counts");
        populateCounts();
        
        Log.i(TAG, "onCreate: MainActivity setup complete");
    }

    private void setupActivityResultLaunchers() {
        nearestLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Handle result from NearestActivity
                if (result.getResultCode() == RESULT_OK) {
                    // Handle successful result
                }
            }
        );
        
        mapLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Handle result from MapActivity
                if (result.getResultCode() == RESULT_OK) {
                    // Handle successful result
                }
            }
        );
        
        searchLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Handle result from FilterActivity
                if (result.getResultCode() == RESULT_OK) {
                    // Handle successful result
                }
            }
        );
        
        preferencesLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Handle result from PreferencesActivity
                if (result.getResultCode() == RESULT_OK) {
                    TextView user = findViewById(R.id.txtUserName);
                    user.setText(mPrefs.getString("username", ""));
                    
                    // Add user details to ACRA
                    // ErrorReporter.getInstance().putCustomData("username", mPrefs.getString("username", ""));
                    
                    populateCounts();
                }
            }
        );
    }
    
    private void setupClickListeners() {
        final Button btnNearest = findViewById(R.id.btnNearest);
        btnNearest.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent i = new Intent(MainActivity.this, NearestActivity.class);
				nearestLauncher.launch(i);
			}
		});       
        mSyncBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				doSync();
			}
		});

        final Button btnLeafletMap = findViewById(R.id.btnLeafletMap);
        btnLeafletMap.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, com.trigpointinguk.android.mapping.LeafletMapActivity.class);
                mapLauncher.launch(i);
            }
        });

    }
    
    private void showNoTrigsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dlgNoTrigs)
               .setCancelable(true)
               .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       dialog.cancel();
                       Intent i = new Intent(MainActivity.this, DownloadTrigsActivity.class);
                       startActivity(i);
                   }
               })
               .setNegativeButton("No", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       dialog.cancel();
                   }
               });
        builder.create().show();
    }

 	
 		@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(RUNBEFORE, true);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (executor != null && !executor.isShutdown()) {
			executor.shutdown();
		}
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.mainmenu, menu);
        return result;
    }    
    
    
		@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		
		if (itemId == R.id.downloadtrigs) {
			Intent i = new Intent(MainActivity.this, DownloadTrigsActivity.class);
			startActivity(i);
			return true;
		} else if (itemId == R.id.downloadmaps) {
			Intent i = new Intent(MainActivity.this, DownloadMapsActivity.class);
			startActivity(i);
			return true;
		} else if (itemId == R.id.prefs) {
			Intent i = new Intent(MainActivity.this, PreferencesActivity.class);
			// Use activity result launcher for preferences
			preferencesLauncher.launch(i);
			return true;
		} else if (itemId == R.id.about) {
			Intent i = new Intent(MainActivity.this, HelpPageActivity.class);
			i.putExtra(HelpPageActivity.PAGE, "about.html");
			startActivity(i);
			return true;
		} else if (itemId == R.id.clearcache) {
			new ClearCacheTask(MainActivity.this).execute();        	
			return true;
        } else if (itemId == R.id.test_crash) {
            // Intentionally crash the app to verify crash reporting
            throw new RuntimeException("Test Crash (menu-triggered)");
		} else if (itemId == R.id.exit) {
			// Show confirmation dialog before exiting
			new AlertDialog.Builder(this)
				.setTitle("Exit Application")
				.setMessage("Are you sure you want to exit?")
				.setPositiveButton("Yes", (dialog, which) -> {
					// Close the application
					finish();
					System.exit(0);
				})
				.setNegativeButton("No", null)
				.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
 
    
    
	@Override
	protected void onResume() {
		Log.i(TAG, "onResume: Refreshing counts and user display");
		super.onResume();
		updateUserDisplay();
		populateCounts();
		checkAndPopulateDatabase();
		checkAndPerformAutoSync();
	}


    private void doSync() {
		Log.i(TAG, "doSync");
		new SyncTask(MainActivity.this, MainActivity.this).execute();
    }
    

	@Override
	public void onSynced(int status) {
		Log.i(TAG, "onSynced");
		populateCounts();
	}





	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	
	private void updateUserDisplay() {
		Log.i(TAG, "updateUserDisplay: Updating user display");
		try {
			// Get username from preferences
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			String username = prefs.getString("username", "");
			
			Log.i(TAG, "updateUserDisplay: Username from preferences: '" + username + "'");
			
			if (username != null && !username.trim().isEmpty()) {
				Log.i(TAG, "updateUserDisplay: Setting username to: " + username);
				mUserName.setText(username);
			} else {
				Log.i(TAG, "updateUserDisplay: No username found, showing 'Not logged in'");
				mUserName.setText("Not logged in");
			}
		} catch (Exception e) {
			Log.e(TAG, "updateUserDisplay: Error updating user display", e);
			e.printStackTrace();
			mUserName.setText("Not logged in");
		}
	}
	
	private void populateCounts() {
		Log.i(TAG, "populateCounts: Starting count population");
		// Show loading state
		runOnUiThread(() -> {
			try {
				Log.i(TAG, "populateCounts: Setting loading state");
				mPillarIcon.setImageResource(R.drawable.t_pillar);
				mPillarCount.setText("");
				mFbmIcon.setImageResource(R.drawable.t_fbm);
				mFbmCount.setText("");
				mPassiveIcon.setImageResource(R.drawable.t_passive);
				mPassiveCount.setText("");
				mIntersectedIcon.setImageResource(R.drawable.t_intersected);
				mIntersectedCount.setText("");
				mUnsyncedIcon.setVisibility(View.INVISIBLE);
				mUnsyncedCount.setText("");
				mPhotosIcon.setVisibility(View.INVISIBLE);
				mPhotosCount.setText("");
				mSyncBtn.setTextColor(getResources().getColor(android.R.color.primary_text_light));
			} catch (NotFoundException e) {
				Log.e(TAG, "populateCounts: Error setting loading state", e);
				e.printStackTrace();
			}
		});
		
		// Run database operations in background
		CompletableFuture.supplyAsync(() -> {
			Log.i(TAG, "populateCounts: Starting database operations");
			DbHelper mDb = new DbHelper(MainActivity.this);
			try {
				Log.i(TAG, "populateCounts: Opening database");
				mDb.open();
				Log.i(TAG, "populateCounts: Counting pillars");
				int nPillar = mDb.countLoggedPillars();
				Log.i(TAG, "populateCounts: Counting FBMs");
				int nFbm = mDb.countLoggedFbms();
				Log.i(TAG, "populateCounts: Counting passives");
				int nPassive = mDb.countLoggedPassives();
				Log.i(TAG, "populateCounts: Counting intersected");
				int nIntersected = mDb.countLoggedIntersecteds();
				Log.i(TAG, "populateCounts: Counting unsynced");
				int nUnsynced = mDb.countUnsynced();
				Log.i(TAG, "populateCounts: Counting photos");
				int nPhotos = mDb.countPhotos();
				Log.i(TAG, "populateCounts: Closing database");
				mDb.close();
				
				Log.i(TAG, "populateCounts: Database counts - Pillars: " + nPillar + ", FBMs: " + nFbm + ", Passives: " + nPassive + ", Intersected: " + nIntersected + ", Unsynced: " + nUnsynced + ", Photos: " + nPhotos);
				return new int[]{nPillar, nFbm, nPassive, nIntersected, nUnsynced, nPhotos};
			} catch (SQLException e) {
				Log.e(TAG, "populateCounts: SQLException during database operations", e);
				e.printStackTrace();
				return new int[]{0, 0, 0, 0, 0, 0};
			} catch (Exception e) {
				Log.e(TAG, "populateCounts: Unexpected exception during database operations", e);
				e.printStackTrace();
				return new int[]{0, 0, 0, 0, 0, 0};
			}
		}, executor).thenAcceptAsync(counts -> {
			// Update UI on main thread
			runOnUiThread(() -> {
				try {
					Log.i(TAG, "populateCounts: Updating UI with counts");
					int nPillar = counts[0];
					int nFbm = counts[1];
					int nPassive = counts[2];
					int nIntersected = counts[3];
					int nUnsynced = counts[4];
					int nPhotos = counts[5];
					
					mPillarCount.setText(String.valueOf(nPillar));
					mFbmCount.setText(String.valueOf(nFbm));
					mPassiveCount.setText(String.valueOf(nPassive));
					mIntersectedCount.setText(String.valueOf(nIntersected));
					
					if (nUnsynced > 0) {
						mUnsyncedIcon.setVisibility(View.VISIBLE);
						mUnsyncedCount.setText(String.valueOf(nUnsynced));
						mSyncBtn.setTextColor(getResources().getColor(R.color.syncNow));
					}
					
					if (nPhotos > 0) {
						mPhotosIcon.setVisibility(View.VISIBLE);
						mPhotosCount.setText(String.valueOf(nPhotos));
					}
					
					Log.i(TAG, "populateCounts: UI update complete");
				} catch (Exception e) {
					Log.e(TAG, "populateCounts: Error updating UI", e);
					e.printStackTrace();
				}
			});
		}).exceptionally(throwable -> {
			Log.e(TAG, "populateCounts: Exception in async operation", throwable);
			throwable.printStackTrace();
			return null;
		});
	}
	
	private void checkAndPopulateDatabase() {
		Log.i(TAG, "checkAndPopulateDatabase: Checking if database needs population");
		
		CompletableFuture.supplyAsync(() -> {
			DbHelper mDb = new DbHelper(MainActivity.this);
			try {
				mDb.open();
				boolean isPopulated = mDb.isTrigTablePopulated();
				mDb.close();
				return isPopulated;
			} catch (Exception e) {
				Log.e(TAG, "checkAndPopulateDatabase: Error checking database", e);
				return false;
			}
		}, executor).thenAcceptAsync(isPopulated -> {
			if (!isPopulated) {
				Log.i(TAG, "checkAndPopulateDatabase: Database is empty, starting automatic population");
				runOnUiThread(() -> {
					Toast.makeText(MainActivity.this, "Database is empty. Starting automatic download...", Toast.LENGTH_LONG).show();
					Intent intent = new Intent(MainActivity.this, DownloadTrigsActivity.class);
					startActivity(intent);
				});
			} else {
				Log.i(TAG, "checkAndPopulateDatabase: Database is already populated");
			}
		});
	}
	
	private void checkAndPerformAutoSync() {
		Log.i(TAG, "checkAndPerformAutoSync: Checking if auto sync is enabled");
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean autoSyncEnabled = prefs.getBoolean("auto_sync", false);
		boolean autoSyncAlreadyRun = prefs.getBoolean(AUTO_SYNC_RUN, false);
		
		Log.i(TAG, "checkAndPerformAutoSync: Auto sync enabled: " + autoSyncEnabled + ", already run: " + autoSyncAlreadyRun);
		
		if (autoSyncEnabled && !autoSyncAlreadyRun) {
			Log.i(TAG, "checkAndPerformAutoSync: Auto sync is enabled and not yet run, starting sync");
			// Check if user has credentials
			String username = prefs.getString("username", "");
			String password = prefs.getString("plaintextpassword", "");
			
			if (username != null && !username.trim().isEmpty() && 
				password != null && !password.trim().isEmpty()) {
				Log.i(TAG, "checkAndPerformAutoSync: Credentials found, performing auto sync");
				// Mark that auto sync has been run
				prefs.edit().putBoolean(AUTO_SYNC_RUN, true).apply();
				doSync();
			} else {
				Log.i(TAG, "checkAndPerformAutoSync: No credentials found, skipping auto sync");
			}
		} else if (autoSyncEnabled && autoSyncAlreadyRun) {
			Log.i(TAG, "checkAndPerformAutoSync: Auto sync is enabled but already run this session");
		} else {
			Log.i(TAG, "checkAndPerformAutoSync: Auto sync is disabled");
		}
	}


}

	
	