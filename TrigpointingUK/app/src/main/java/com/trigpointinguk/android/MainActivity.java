package com.trigpointinguk.android;

// import org.acra.ErrorReporter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.database.SQLException;
import android.os.AsyncTask;
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
import androidx.appcompat.app.AppCompatActivity;

import com.trigpointinguk.android.common.ClearCacheTask;
import com.trigpointinguk.android.filter.FilterActivity;
import com.trigpointinguk.android.logging.SyncListener;
import com.trigpointinguk.android.logging.SyncTask;
import com.trigpointinguk.android.mapping.DownloadMapsActivity;
import com.trigpointinguk.android.mapping.MapActivity;
import com.trigpointinguk.android.nearest.NearestActivity;

public class MainActivity extends AppCompatActivity implements SyncListener {
    public static final String 	TAG ="MainActivity";
    public static final int 	NOTRIGS = 1;
	private static final String RUNBEFORE = "RUNBEFORE";
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
        mPillarIcon = (ImageView) findViewById(R.id.countPillarImage);
        mPillarCount = (TextView) findViewById(R.id.countPillarText);
        mFbmIcon = (ImageView) findViewById(R.id.countFbmImage);
        mFbmCount = (TextView) findViewById(R.id.countFbmText);
        mPassiveIcon = (ImageView) findViewById(R.id.countPassiveImage);
        mPassiveCount = (TextView) findViewById(R.id.countPassiveText);
        mIntersectedIcon = (ImageView) findViewById(R.id.countIntersectedImage);
        mIntersectedCount = (TextView) findViewById(R.id.countIntersectedText);
        mUnsyncedIcon = (ImageView) findViewById(R.id.countUnsyncedImage);
        mUnsyncedCount = (TextView) findViewById(R.id.countUnsyncedText);
        mPhotosIcon = (ImageView) findViewById(R.id.countPhotosImage);
        mPhotosCount = (TextView) findViewById(R.id.countPhotosText);
        mSyncBtn = (Button) findViewById(R.id.btnSync);
        mUserName = (TextView) findViewById(R.id.txtUserName);
        
        Log.i(TAG, "onCreate: Setting up preferences");
        mPrefs = getSharedPreferences("TrigpointingUK", MODE_PRIVATE);
        
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
                    TextView user = (TextView) findViewById(R.id.txtUserName);
                    user.setText(mPrefs.getString("username", ""));
                    
                    // Add user details to ACRA
                    // ErrorReporter.getInstance().putCustomData("username", mPrefs.getString("username", ""));
                    
                    populateCounts();
                }
            }
        );
    }
    
    private void setupClickListeners() {
        final Button btnNearest = (Button) findViewById(R.id.btnNearest);
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

        final Button btnMap = (Button) findViewById(R.id.btnMap);
        btnMap.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent i = new Intent(MainActivity.this, MapActivity.class);
				mapLauncher.launch(i);
			}
		});
        final Button btnSearch = (Button) findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
	            Intent i = new Intent(MainActivity.this, FilterActivity.class);
	            searchLauncher.launch(i);
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
		}
		return super.onOptionsItemSelected(item);
	}
 
    
    
	@Override
	protected void onResume() {
		Log.i(TAG, "onResume: Refreshing counts and user display");
		super.onResume();
		updateUserDisplay();
		populateCounts();
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





	private ExecutorService executor = Executors.newSingleThreadExecutor();
	
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


}

	
	