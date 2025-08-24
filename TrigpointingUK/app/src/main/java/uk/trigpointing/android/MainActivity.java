package uk.trigpointing.android;

// import org.acra.ErrorReporter;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.database.SQLException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;
import uk.trigpointing.android.common.BaseActivity;
import uk.trigpointing.android.common.ClearCacheTask;
import uk.trigpointing.android.logging.SyncListener;
import uk.trigpointing.android.logging.SyncTask;
import uk.trigpointing.android.mapping.DownloadMapsActivity;
import uk.trigpointing.android.nearest.NearestActivity;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import uk.trigpointing.android.api.AuthApiClient;
import uk.trigpointing.android.api.AuthPreferences;
import uk.trigpointing.android.api.User;
import coil.Coil;
import coil.ImageLoader;
import coil.request.ImageRequest;

public class MainActivity extends BaseActivity implements SyncListener {
    public static final String 	TAG ="MainActivity";
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
    private Button				mARCoreBtn;
    private TextView			mUserName;
    private ImageView			mUserMapImage;
    
    // API authentication components
    private AuthApiClient authApiClient;
    private AuthPreferences authPreferences;
    
    // Modern activity result launchers
    private ActivityResultLauncher<Intent> nearestLauncher;
    private ActivityResultLauncher<Intent> mapLauncher;
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
        mARCoreBtn = findViewById(R.id.btnARCore);
        mUserName = findViewById(R.id.txtUserName);
        mUserMapImage = findViewById(R.id.userMapImage);
        
        Log.i(TAG, "onCreate: Setting up preferences");
        mPrefs = getSharedPreferences("TrigpointingUK", MODE_PRIVATE);
        
        // Initialize API authentication components
        authApiClient = new AuthApiClient();
        authPreferences = new AuthPreferences(this);
        
        // Reset auto sync flag on app startup
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(AUTO_SYNC_RUN, false).apply();
        Log.i(TAG, "onCreate: Reset auto sync flag for new app session");
        
        Log.i(TAG, "onCreate: Setting up activity result launchers");
        setupActivityResultLaunchers();
        
        Log.i(TAG, "onCreate: Setting up click listeners");
        setupClickListeners();
        
        Log.i(TAG, "onCreate: Setting up back button handling");
        setupBackButtonHandling();
        
        Log.i(TAG, "onCreate: Updating user display");
        updateUserDisplay();
        
        Log.i(TAG, "onCreate: Populating counts");
        populateCounts();
        
        Log.i(TAG, "onCreate: Checking for OS API key");
        checkAndFetchOsApiKey();
        
        Log.i(TAG, "onCreate: MainActivity setup complete");
    }

    private void checkAndFetchOsApiKey() {
        CompletableFuture.runAsync(() -> {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String osApiKey = prefs.getString("os_api_key", "");

            if (osApiKey != null && !osApiKey.trim().isEmpty()) {
                Log.i(TAG, "OS API key already exists.");
                return;
            }

            Log.i(TAG, "OS API key is missing. Fetching from URL...");
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://trigpointinguk-maps.s3.eu-west-1.amazonaws.com/OS_API_KEY.txt")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String apiKey = response.body().string().trim();
                    if (!apiKey.isEmpty()) {
                        prefs.edit().putString("os_api_key", apiKey).apply();
                        Log.i(TAG, "OS API key fetched and saved successfully.");
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "OS API Key updated.", Toast.LENGTH_SHORT).show());
                    } else {
                        Log.e(TAG, "Fetched OS API key is empty.");
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to update OS API Key: Key file is empty.", Toast.LENGTH_LONG).show());
                    }
                } else {
                    Log.e(TAG, "Failed to fetch OS API key. Response code: " + response.code());
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to update OS API Key: Server error.", Toast.LENGTH_LONG).show());
                }
            } catch (IOException e) {
                Log.e(TAG, "Error checking or fetching OS API key", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to update OS API Key: Network error.", Toast.LENGTH_LONG).show());
            }
        }, executor);
    }

    private void setupActivityResultLaunchers() {
        nearestLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {}
        );
        
        mapLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {}
        );

        // Handle result from FilterActivity
        // Handle successful result
        ActivityResultLauncher<Intent> searchLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {}
        );
        
        preferencesLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Handle result from PreferencesActivity
                if (result.getResultCode() == RESULT_OK) {
                    TextView user = findViewById(R.id.txtUserName);
                    user.setText(mPrefs.getString("username", ""));
                    
                    // Add user details to ACRA (disabled)

                    populateCounts();
                }
            }
        );
    }
    
    private void setupClickListeners() {
        final Button btnNearest = findViewById(R.id.btnNearest);
        btnNearest.setOnClickListener(arg0 -> {
            Intent i = new Intent(MainActivity.this, NearestActivity.class);
            nearestLauncher.launch(i);
        });
        mSyncBtn.setOnClickListener(arg0 -> doSync());

        final Button btnLeafletMap = findViewById(R.id.btnLeafletMap);
        btnLeafletMap.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, uk.trigpointing.android.mapping.LeafletMapActivity.class);
            mapLauncher.launch(i);
        });

        // ARCore button (developer mode only)
        mARCoreBtn.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, uk.trigpointing.android.ar.ARTrigpointActivity.class);
            startActivity(i);
        });

    }
    
    private void setupBackButtonHandling() {
        // Handle back button press to prevent app from closing on main activity
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Show a toast to inform user that back button is disabled on main page
                Toast.makeText(MainActivity.this, "Press home button to minimize app", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Back button pressed on main activity - staying on main page");
                // Do nothing - this prevents the default back behavior (closing the app)
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
        Log.i(TAG, "setupBackButtonHandling: Back button handling configured");
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
        getMenuInflater().inflate(R.menu.mainmenu, menu);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean devMode = prefs.getBoolean("dev_mode", false);
        
        // Check both API authentication and legacy authentication
        boolean loggedIn = authPreferences.isLoggedIn() || !prefs.getString("username", "").isEmpty();

        // These items are always visible, so no changes needed for them.
        // menu.findItem(R.id.action_settings).setVisible(true);
        // menu.findItem(R.id.action_about).setVisible(true);

        // These items are only visible in developer mode.
        menu.findItem(R.id.action_refresh).setVisible(devMode);
        menu.findItem(R.id.action_clearcache).setVisible(devMode);
        menu.findItem(R.id.action_cachestatus).setVisible(devMode);
        menu.findItem(R.id.action_testcrash).setVisible(devMode);
        menu.findItem(R.id.action_exit).setVisible(devMode);

        menu.findItem(R.id.action_login).setVisible(!loggedIn);
        menu.findItem(R.id.action_logout).setVisible(loggedIn);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (itemId == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        } else if (itemId == R.id.action_login) {
            showLoginDialog();
            return true;
        } else if (itemId == R.id.action_logout) {
            doLogout();
            return true;
        } else if (itemId == R.id.action_refresh) {
            startActivity(new Intent(this, DownloadTrigsActivity.class));
            return true;
        } else if (itemId == R.id.action_clearcache) {
            new ClearCacheTask(this).execute();
            return true;
        } else if (itemId == R.id.action_cachestatus) {
            // Implement cache status functionality
            return true;
        } else if (itemId == R.id.action_testcrash) {
            throw new RuntimeException("Test Crash");
        } else if (itemId == R.id.action_exit) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showLoginDialog() {
        showLoginDialog(null);
    }

    private void showLoginDialog(String errorMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Login");

        View view = getLayoutInflater().inflate(R.layout.dialog_login, null);
        builder.setView(view);

        final EditText username = view.findViewById(R.id.username);
        final EditText password = view.findViewById(R.id.password);

        // If there's an error message, add it to the dialog
        if (errorMessage != null && !errorMessage.isEmpty()) {
            TextView errorText = new TextView(this);
            errorText.setText(errorMessage);
            errorText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            errorText.setPadding(24, 8, 24, 16);
            
            // Create a new container to include the error message
            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.VERTICAL);
            container.addView(errorText);
            container.addView(view);
            builder.setView(container);
        }

        builder.setPositiveButton("Login", null); // Set to null initially
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // Clear any stored authentication data on cancel
            authPreferences.clearAuthData();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("username");
            editor.remove("plaintextpassword");
            editor.apply();
            
            updateUserDisplay();
            invalidateOptionsMenu();
            dialog.cancel();
        });

        AlertDialog dialog = builder.create();
        
        // Override the positive button to handle authentication
        dialog.setOnShowListener(dialogInterface -> {
            Button loginButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            loginButton.setOnClickListener(v -> {
                String user = username.getText().toString().trim();
                String pass = password.getText().toString().trim();

                if (user.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Disable the login button and show progress
                loginButton.setEnabled(false);
                loginButton.setText("Logging in...");

                // Authenticate with the new API
                authApiClient.authenticate(user, pass, new AuthApiClient.AuthCallback() {
                    @Override
                    public void onSuccess(uk.trigpointing.android.api.AuthResponse authResponse) {
                        runOnUiThread(() -> {
                            Log.i(TAG, "API authentication successful for user: " + authResponse.getUser().getName());
                            
                            // Store the API authentication data
                            authPreferences.storeAuthData(authResponse);
                            
                            // Also store legacy credentials for backward compatibility
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("username", user);
                            editor.putString("plaintextpassword", pass);
                            editor.apply();

                            // Update UI and close dialog
                            updateUserDisplay();
                            invalidateOptionsMenu();
                            dialog.dismiss();
                            
                            // Start sync
                            doSync();
                            
                            Toast.makeText(MainActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            Log.w(TAG, "API authentication failed: " + errorMessage);
                            
                            // Re-enable the login button
                            loginButton.setEnabled(true);
                            loginButton.setText("Login");
                            
                            // Close current dialog and show new one with error
                            dialog.dismiss();
                            showLoginDialog(errorMessage);
                        });
                    }
                });
            });
        });

        dialog.show();
    }

    private void doLogout() {
        // Clear new API authentication data
        authPreferences.clearAuthData();
        
        // Clear legacy authentication data
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("username");
        editor.remove("plaintextpassword");
        editor.apply();

        // Clear user logs from database
        DbHelper dbHelper = new DbHelper(this);
        dbHelper.open();
        dbHelper.clearUserLogs();
        dbHelper.close();

        updateUserDisplay();
        invalidateOptionsMenu();
        
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }
 
    
    
	@Override
	protected void onResume() {
		Log.i(TAG, "onResume: Refreshing counts and user display");
		super.onResume();
		invalidateOptionsMenu();
		updateUserDisplay();
		populateCounts();
		checkAndPopulateDatabase();
		checkAndPerformAutoSync();
	}


    private void doSync() {
		Log.i(TAG, "doSync");
		new SyncTask(MainActivity.this, MainActivity.this).execute(false);
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
			View countSection = findViewById(R.id.count_section);
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			boolean devMode = prefs.getBoolean("dev_mode", false);
			
			String displayName;
			boolean isLoggedIn = false;
			
			// Check if we have API authentication data first
			if (authPreferences.isLoggedIn()) {
				Log.i(TAG, "updateUserDisplay: User is logged in via API");
				
				if (devMode) {
					displayName = authPreferences.getDisplayNameWithId();
					Log.i(TAG, "updateUserDisplay: Developer mode - showing name with ID: " + displayName);
				} else {
					displayName = authPreferences.getDisplayName();
					Log.i(TAG, "updateUserDisplay: Normal mode - showing name: " + displayName);
				}
				isLoggedIn = true;
			} else {
				// Fallback to legacy username for backward compatibility
				String legacyUsername = prefs.getString("username", "");
				if (!legacyUsername.trim().isEmpty()) {
					Log.i(TAG, "updateUserDisplay: Using legacy username: " + legacyUsername);
					displayName = legacyUsername;
					if (devMode) {
						displayName += " (legacy)";
					}
					isLoggedIn = true;
				} else {
					Log.i(TAG, "updateUserDisplay: No authentication found");
					displayName = "Not logged in";
					isLoggedIn = false;
				}
			}
			
			mUserName.setText(displayName);
			
			if (isLoggedIn) {
				countSection.setVisibility(View.VISIBLE);
				mSyncBtn.setVisibility(View.VISIBLE);
				updateUserMap();
			} else {
				countSection.setVisibility(View.INVISIBLE);
				mSyncBtn.setVisibility(View.INVISIBLE);
				mUserMapImage.setVisibility(View.GONE);
			}
			
			// Show ARCore button only for developers
			if (devMode) {
				mARCoreBtn.setVisibility(View.VISIBLE);
			} else {
				mARCoreBtn.setVisibility(View.GONE);
			}
			
		} catch (Exception e) {
			Log.e(TAG, "updateUserDisplay: Error updating user display", e);
			e.printStackTrace();
			mUserName.setText("Not logged in");
		}
	}
	
	private void updateUserMap() {
		Log.i(TAG, "updateUserMap: Updating user map image");
		try {
			// Only load map if user is logged in via API
			if (authPreferences.isLoggedIn()) {
				User user = authPreferences.getUser();
				if (user != null) {
					String mapUrl = "https://trigpointing.uk/pics/make_map.php?u=" + user.getId() + "&v=y";
					Log.i(TAG, "updateUserMap: Loading map for user ID " + user.getId() + " from URL: " + mapUrl);
					
					// Load the image using Coil
					ImageRequest request = new ImageRequest.Builder(this)
							.data(mapUrl)
							.target(mUserMapImage)
							.placeholder(android.R.drawable.ic_menu_mapmode) // Show placeholder while loading
							.error(android.R.drawable.ic_dialog_alert) // Show error icon if loading fails
							.build();
					
					// Show the ImageView and load the image
					mUserMapImage.setVisibility(View.VISIBLE);
					ImageLoader imageLoader = Coil.imageLoader(this);
					imageLoader.enqueue(request);
					
					// Add click listener to open full map view
					mUserMapImage.setOnClickListener(v -> {
						String fullMapUrl = "https://trigpointing.uk/pics/make_map.php?u=" + user.getId() + "&v=y";
						android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(fullMapUrl));
						startActivity(intent);
					});
				} else {
					Log.w(TAG, "updateUserMap: User object is null, hiding map");
					mUserMapImage.setVisibility(View.GONE);
				}
			} else {
				Log.i(TAG, "updateUserMap: User not logged in via API, hiding map");
				mUserMapImage.setVisibility(View.GONE);
			}
		} catch (Exception e) {
			Log.e(TAG, "updateUserMap: Error updating user map", e);
			e.printStackTrace();
			mUserMapImage.setVisibility(View.GONE);
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
				                mSyncBtn.setTextColor(ContextCompat.getColor(this, android.R.color.primary_text_light));
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
						                mSyncBtn.setTextColor(ContextCompat.getColor(this, R.color.syncNow));
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
			
			if (!username.trim().isEmpty() && !password.trim().isEmpty()) {
				Log.i(TAG, "checkAndPerformAutoSync: Credentials found, performing auto sync");
				// Mark that auto sync has been run
				prefs.edit().putBoolean(AUTO_SYNC_RUN, true).apply();
				new SyncTask(MainActivity.this, MainActivity.this).execute(false);
			} else {
				Log.i(TAG, "checkAndPerformAutoSync: No credentials found, skipping auto sync");
			}
		} else if (autoSyncEnabled) {
			Log.i(TAG, "checkAndPerformAutoSync: Auto sync is enabled but already run this session");
		} else {
			Log.i(TAG, "checkAndPerformAutoSync: Auto sync is disabled");
		}
	}


}

	
	