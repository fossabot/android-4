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
    
    // Modern activity result launchers
    private ActivityResultLauncher<Intent> nearestLauncher;
    private ActivityResultLauncher<Intent> mapLauncher;
    private ActivityResultLauncher<Intent> searchLauncher;
    private ActivityResultLauncher<Intent> preferencesLauncher;
    
 	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");

        // retrieve saved instance state
        Boolean runbefore = false;
        if (savedInstanceState != null) {
        	runbefore = savedInstanceState.getBoolean(RUNBEFORE, false);
        } 
        
        setContentView(R.layout.main);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mPillarIcon 		= (ImageView) 		findViewById(R.id.countPillarImage);
        mPillarCount		= (TextView)		findViewById(R.id.countPillarText);
        mFbmIcon 			= (ImageView) 		findViewById(R.id.countFbmImage);
        mFbmCount			= (TextView)		findViewById(R.id.countFbmText);
        mPassiveIcon 		= (ImageView) 		findViewById(R.id.countPassiveImage);
        mPassiveCount		= (TextView)		findViewById(R.id.countPassiveText);
        mIntersectedIcon 	= (ImageView) 		findViewById(R.id.countIntersectedImage);
        mIntersectedCount	= (TextView)		findViewById(R.id.countIntersectedText);
        mUnsyncedCount		= (TextView)		findViewById(R.id.countUnsyncedText);
        mUnsyncedIcon		= (ImageView) 		findViewById(R.id.countUnsyncedImage);
        mPhotosCount		= (TextView)		findViewById(R.id.countPhotosText);
        mPhotosIcon			= (ImageView) 		findViewById(R.id.countPhotosImage);
        mSyncBtn 			= (Button) 			findViewById(R.id.btnSync);
        
        // Add user's name to view
        TextView user = (TextView) findViewById(R.id.txtUserName);
        user.setText(mPrefs.getString("username", getResources().getText(R.string.noUser).toString()));
        
        // Add user info to ACRA
        // ErrorReporter.getInstance().putCustomData("username", mPrefs.getString("username", ""));

        // check for empty trig database
        try {
	        DbHelper db = new DbHelper(this);
	        db.open();
	        if (! db.isTrigTablePopulated()) {
	        	showNoTrigsDialog();
	        }
	        db.close();
        } catch (Exception e) {
        	Log.e(TAG, "Caught exception - " + e.getMessage());
        }
        
        // Setup activity result launchers
        setupActivityResultLaunchers();
        
        // Setup click listeners
        setupClickListeners();
        
        //autosync
        if (mPrefs.getBoolean("autosync", false) && !runbefore) {
			doSync();     	
        }   
        
        //Experimental
        if (mPrefs.getBoolean("experimental", false)) {
        	Toast.makeText(this, "Running in experimental mode", Toast.LENGTH_LONG).show();
        } else {
        	// disable crash button
        	Button btnCrash = (Button) findViewById(R.id.btnCrash);
        	btnCrash.setVisibility(View.INVISIBLE);
        }
        // disable search button
        Button btnSearch = (Button) findViewById(R.id.btnSearch);
        btnSearch.setEnabled(false);
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
        final Button btnCrash = (Button) findViewById(R.id.btnCrash);
        btnCrash.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Log.e(TAG, "Deliberate error" + 1/0);
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
		super.onResume();
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
	
	private void populateCounts() {
		// Show loading state
		runOnUiThread(() -> {
			try {
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
				e.printStackTrace();
			}
		});
		
		// Run database operations in background
		CompletableFuture.supplyAsync(() -> {
			DbHelper mDb = new DbHelper(MainActivity.this);
			try {
				mDb.open();
				int nPillar = mDb.countLoggedPillars();
				int nFbm = mDb.countLoggedFbms();
				int nPassive = mDb.countLoggedPassives();
				int nIntersected = mDb.countLoggedIntersecteds();
				int nUnsynced = mDb.countUnsynced();
				int nPhotos = mDb.countPhotos();
				mDb.close();
				
				return new int[]{nPillar, nFbm, nPassive, nIntersected, nUnsynced, nPhotos};
			} catch (SQLException e) {
				e.printStackTrace();
				return new int[]{0, 0, 0, 0, 0, 0};
			}
		}, executor).thenAcceptAsync(counts -> {
			// Update UI on main thread
			runOnUiThread(() -> {
				int nPillar = counts[0];
				int nFbm = counts[1];
				int nPassive = counts[2];
				int nIntersected = counts[3];
				int nUnsynced = counts[4];
				int nPhotos = counts[5];
				
				if (nPillar > 0) {
					mPillarIcon.setImageResource(R.drawable.ts_pillar);
					mPillarCount.setText(String.valueOf(nPillar));				
				}
				if (nFbm > 0) {
					mFbmIcon.setImageResource(R.drawable.ts_fbm);
					mFbmCount.setText(String.valueOf(nFbm));				
				}
				if (nPassive > 0) {
					mPassiveIcon.setImageResource(R.drawable.ts_passive);
					mPassiveCount.setText(String.valueOf(nPassive));				
				}
				if (nIntersected > 0) {
					mIntersectedIcon.setImageResource(R.drawable.ts_intersected);
					mIntersectedCount.setText(String.valueOf(nIntersected));				
				}
				if (nUnsynced > 0) {
					mUnsyncedIcon.setVisibility(View.VISIBLE);
					mUnsyncedCount.setText(String.valueOf(nUnsynced));
				}
				if (nPhotos > 0) {
					mPhotosIcon.setVisibility(View.VISIBLE);
					mPhotosCount.setText(String.valueOf(nPhotos));
				}
				
				if (nUnsynced > 0 || nPhotos > 0) {
					mSyncBtn.setTextColor(getResources().getColor(R.color.syncNow));
				} else {
					mSyncBtn.setTextColor(getResources().getColor(android.R.color.primary_text_light));
				}
			});
		}, executor);
	}


}

	
	