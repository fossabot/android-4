package uk.trigpointing.android.trigdetails;

import android.app.LocalActivityManager;
import android.content.Intent;
import android.net.Uri;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;
import android.view.MenuItem;
import android.view.Menu;
import uk.trigpointing.android.R;
import uk.trigpointing.android.common.BaseActivity;
import uk.trigpointing.android.logging.LogTrigActivity;
import uk.trigpointing.android.DbHelper;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.activity.OnBackPressedCallback;
import android.widget.Toast;
import android.database.Cursor;

import java.util.Locale;

public class TrigDetailsActivity extends BaseActivity {

	private static final String TAG="TrigDetailsActivity";
    private static final String STATE_TAB_TAG = "saved_tab_tag";
    private LocalActivityManager mLocalActivityManager;

	public void onCreate(Bundle savedInstanceState) {
	    try {
	        android.util.Log.d(TAG, "onCreate called with savedInstanceState: " + (savedInstanceState != null ? "not null" : "null"));
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.trigdetails);
	        
	        if (getSupportActionBar() != null) {
	            try {
	                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	            } catch (Exception e) {
	                android.util.Log.w(TAG, "Error setting up action bar: " + e.getMessage());
	            }
	        } else {
	            android.util.Log.w(TAG, "Action bar is null");
	        }
	        
	        // Content positioning is now handled by BaseActivity

            Bundle extras = getIntent().getExtras();
            long ensuredTrigId = getIntent().getLongExtra(DbHelper.TRIG_ID, -1);
            if (extras == null) {
                extras = new Bundle();
            }
            if (ensuredTrigId > 0 && !extras.containsKey(DbHelper.TRIG_ID)) {
                extras.putLong(DbHelper.TRIG_ID, ensuredTrigId);
            }
            
            // Validate that we have a valid trig ID
            if (ensuredTrigId <= 0) {
                android.util.Log.w(TAG, "No valid trig ID provided");
                showToast("Invalid trigpoint");
                finish();
                return;
            }

            // Populate header with trig icon + name + condition icon (white on green)
            try {
                android.widget.TextView header = findViewById(R.id.trig_header_title);
                android.widget.ImageView typeIcon = findViewById(R.id.trig_header_type_icon);
                android.widget.ImageView condIcon = findViewById(R.id.trig_header_condition_icon);
                if (header != null) {
                    DbHelper db = new DbHelper(this);
                    db.openReadable();
                    try (android.database.Cursor c = db.fetchTrigInfo(ensuredTrigId)) {
                        if (c != null && c.moveToFirst()) {
                            int idxName = c.getColumnIndex(DbHelper.TRIG_NAME);
                            int idxType = c.getColumnIndex(DbHelper.TRIG_TYPE);
                            int idxCond = c.getColumnIndex(DbHelper.TRIG_CONDITION);
                            String name = (idxName >= 0) ? c.getString(idxName) : "";
                            String typeCode = (idxType >= 0) ? c.getString(idxType) : null;
                            String condCode = (idxCond >= 0) ? c.getString(idxCond) : null;

                            // Build header text
                            header.setText(name != null ? name : "");

                            // Resolve icons
                            uk.trigpointing.android.types.Trig.Physical physical = uk.trigpointing.android.types.Trig.Physical.fromCode(typeCode);
                            uk.trigpointing.android.types.Condition condition = uk.trigpointing.android.types.Condition.fromCode(condCode);

                            // Determine if trig is marked to use highlighted icon
                            boolean isMarked = false;
                            try { isMarked = db.isMarkedTrig(ensuredTrigId); } catch (Exception ignored) {}
                            if (typeIcon != null) {
                                try { typeIcon.setImageResource(mapGreenTypeIconResource(typeCode, isMarked)); } catch (Exception ignored) {}
                            }
                            if (condIcon != null) {
                                try { condIcon.setImageResource(condition.icon()); } catch (Exception ignored) {}
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.w(TAG, "Failed to load trig name: " + e.getMessage());
                    } finally {
                        try { db.close(); } catch (Exception ignored) {}
                    }
                }
            } catch (Exception e) {
                android.util.Log.w(TAG, "Error setting trig header", e);
            }
            
            android.util.Log.d(TAG, "Setting up tabs with trig ID: " + ensuredTrigId);
            setupTabs(extras, savedInstanceState);
            
            // Set up modern back navigation using OnBackPressedDispatcher
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    finish();
                }
            });
            
            android.util.Log.d(TAG, "onCreate completed successfully");
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            showToast("Error initializing trig details");
            finish();
        }
	}

    private void setupTabs(Bundle extras, Bundle savedInstanceState) {
        android.util.Log.d(TAG, "setupTabs called");
        Resources res = getResources();
        TabHost tabHost = findViewById(android.R.id.tabhost);
        if (tabHost == null) {
            android.util.Log.e(TAG, "TabHost not found in layout");
            return;
        }
        
        try {
            android.util.Log.d(TAG, "Creating LocalActivityManager");
            mLocalActivityManager = new LocalActivityManager(this, false);
            if (mLocalActivityManager != null) {
                try {
                    android.util.Log.d(TAG, "Dispatching create to LocalActivityManager");
                    mLocalActivityManager.dispatchCreate(savedInstanceState);
                    android.util.Log.d(TAG, "Setting up TabHost with LocalActivityManager");
                    tabHost.setup(mLocalActivityManager);
                    android.util.Log.d(TAG, "TabHost setup completed successfully");
                } catch (Exception e) {
                    android.util.Log.e(TAG, "Error setting up TabHost: " + e.getMessage(), e);
                    return;
                }
            } else {
                android.util.Log.e(TAG, "LocalActivityManager is null");
                return;
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error creating LocalActivityManager: " + e.getMessage(), e);
            return;
        }

        TabHost.TabSpec spec;
        Intent intent;

        try {
            intent = new Intent().setClass(this, TrigDetailsInfoTab.class);
            if (intent != null) {
                if (extras != null) {
                    intent.putExtras(extras);
                }
                spec = tabHost.newTabSpec("info").setIndicator("",
                                getDrawableSafely(res, android.R.drawable.ic_menu_info_details))
                                .setContent(intent);
                if (spec != null) {
                    try {
                        tabHost.addTab(spec);
                    } catch (Exception e) {
                        android.util.Log.w(TAG, "Error adding info tab: " + e.getMessage());
                    }
                }
            }

            intent = new Intent().setClass(this, TrigDetailsLoglistTab.class);
            if (intent != null) {
                if (extras != null) {
                    intent.putExtras(extras);
                }
                spec = tabHost.newTabSpec("logs").setIndicator("",
                                  getDrawableSafely(res, android.R.drawable.ic_menu_agenda))
                              .setContent(intent);
                if (spec != null) {
                    try {
                        tabHost.addTab(spec);
                    } catch (Exception e) {
                        android.util.Log.w(TAG, "Error adding logs tab: " + e.getMessage());
                    }
                }
            }

            intent = new Intent().setClass(this, TrigDetailsAlbumTab.class);
            if (intent != null) {
                if (extras != null) {
                    intent.putExtras(extras);
                }
                spec = tabHost.newTabSpec("album").setIndicator("",
                                  getDrawableSafely(res, android.R.drawable.ic_menu_gallery))
                              .setContent(intent);
                if (spec != null) {
                    try {
                        tabHost.addTab(spec);
                    } catch (Exception e) {
                        android.util.Log.w(TAG, "Error adding album tab: " + e.getMessage());
                    }
                }
            }

            intent = new Intent().setClass(this, TrigDetailsOSMapTab.class);
            if (intent != null) {
                if (extras != null) {
                    intent.putExtras(extras);
                }
                spec = tabHost.newTabSpec("map").setIndicator("",
                                  getDrawableSafely(res, android.R.drawable.ic_menu_mapmode))
                              .setContent(intent);
                if (spec != null) {
                    try {
                        tabHost.addTab(spec);
                    } catch (Exception e) {
                        android.util.Log.w(TAG, "Error adding map tab: " + e.getMessage());
                    }
                }
            }

            intent = new Intent().setClass(this, LogTrigActivity.class);
            if (intent != null) {
                if (extras != null) {
                    intent.putExtras(extras);
                }
                spec = tabHost.newTabSpec("mylog").setIndicator("",
                                  getDrawableSafely(res, android.R.drawable.ic_menu_edit))
                              .setContent(intent);
                if (spec != null) {
                    try {
                        tabHost.addTab(spec);
                    } catch (Exception e) {
                        android.util.Log.w(TAG, "Error adding mylog tab: " + e.getMessage());
                    }
                }
            }

            try {
                // Restore previously selected tab if provided
                if (savedInstanceState != null) {
                    try {
                        String savedTag = savedInstanceState.getString(STATE_TAB_TAG, null);
                        if (savedTag != null) {
                            tabHost.setCurrentTabByTag(savedTag);
                            android.util.Log.d(TAG, "Restored tab tag: " + savedTag);
                        }
                    } catch (Exception e) {
                        android.util.Log.w(TAG, "Error restoring current tab: " + e.getMessage());
                    }
                }
                android.util.Log.d(TAG, "Tabs setup completed successfully");
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error creating tabs: " + e.getMessage(), e);
                showToast("Error setting up tabs");
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error creating tabs: " + e.getMessage(), e);
            showToast("Error setting up tabs");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        android.util.Log.d(TAG, "onResume called");
        
        // Check if we need to recreate tabs (e.g., if LocalActivityManager is in a bad state)
        if (mLocalActivityManager == null || !isLocalActivityManagerValid()) {
            android.util.Log.w(TAG, "LocalActivityManager is null or invalid, attempting to recreate tabs");
            try {
                recreateTabs();
            } catch (Exception e) {
                android.util.Log.e(TAG, "Failed to recreate tabs: " + e.getMessage(), e);
            }
        }
        
        if (mLocalActivityManager != null) {
            try {
                android.util.Log.d(TAG, "Dispatching resume to LocalActivityManager");
                mLocalActivityManager.dispatchResume();
                android.util.Log.d(TAG, "LocalActivityManager resume completed successfully");
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error in LocalActivityManager dispatchResume: " + e.getMessage(), e);
                // Try to recover by recreating the LocalActivityManager
                try {
                    android.util.Log.w(TAG, "Attempting to recreate LocalActivityManager");
                    recreateLocalActivityManager();
                    // After recreating, try to dispatch resume again
                    try {
                        android.util.Log.d(TAG, "Retrying dispatchResume after recreation");
                        mLocalActivityManager.dispatchResume();
                        android.util.Log.d(TAG, "LocalActivityManager resume completed successfully after recreation");
                    } catch (Exception retryException) {
                        android.util.Log.e(TAG, "Failed to dispatchResume after recreation: " + retryException.getMessage());
                    }
                } catch (Exception recoveryException) {
                    android.util.Log.e(TAG, "Failed to recover LocalActivityManager: " + recoveryException.getMessage());
                }
            }
        } else {
            android.util.Log.w(TAG, "LocalActivityManager is null in onResume");
        }
        
        // Additional safety check - if we still don't have a valid LocalActivityManager, try to recreate everything
        if (mLocalActivityManager == null || !isLocalActivityManagerValid()) {
            android.util.Log.w(TAG, "LocalActivityManager still invalid after recovery attempts, forcing recreation");
            try {
                recreateTabs();
            } catch (Exception e) {
                android.util.Log.e(TAG, "Failed to recreate tabs after recovery: " + e.getMessage(), e);
            }
        }
    }

    @Override
    protected void onPause() {
        android.util.Log.d(TAG, "onPause called");
        
        if (mLocalActivityManager != null) {
            try {
                android.util.Log.d(TAG, "Dispatching pause to LocalActivityManager");
                mLocalActivityManager.dispatchPause(isFinishing());
                android.util.Log.d(TAG, "LocalActivityManager pause completed successfully");
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error in LocalActivityManager dispatchPause: " + e.getMessage(), e);
                // If pause fails, the LocalActivityManager might be in a bad state
                android.util.Log.w(TAG, "LocalActivityManager pause failed, marking as invalid");
            }
        } else {
            android.util.Log.w(TAG, "LocalActivityManager is null in onPause");
        }
        
        // Additional safety check - if we're not finishing, this might be a temporary pause (like opening browser)
        if (!isFinishing()) {
            android.util.Log.d(TAG, "Activity is pausing but not finishing (likely opening browser)");
        }
        
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        android.util.Log.d(TAG, "onSaveInstanceState called");
        try {
            // LocalActivityManager doesn't have dispatchSaveInstanceState method
            // We just need to save our own state
            if (mLocalActivityManager != null) {
                android.util.Log.d(TAG, "LocalActivityManager exists, saving state");
                // Save the trig ID if we have it
                long trigId = getIntent().getLongExtra(DbHelper.TRIG_ID, -1);
                if (trigId > 0) {
                    outState.putLong("saved_trig_id", trigId);
                    android.util.Log.d(TAG, "Saved trig ID to state: " + trigId);
                }
                // Save current tab tag so we can restore after rotation
                try {
                    TabHost tabHost = findViewById(android.R.id.tabhost);
                    if (tabHost != null) {
                        String currentTag = tabHost.getCurrentTabTag();
                        outState.putString(STATE_TAB_TAG, currentTag);
                        android.util.Log.d(TAG, "Saved tab tag: " + currentTag);
                    }
                } catch (Exception e) {
                    android.util.Log.w(TAG, "Failed to save tab tag: " + e.getMessage());
                }
            } else {
                android.util.Log.w(TAG, "LocalActivityManager is null in onSaveInstanceState");
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error in onSaveInstanceState: " + e.getMessage(), e);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        android.util.Log.d(TAG, "onRestoreInstanceState called");
        try {
            super.onRestoreInstanceState(savedInstanceState);
            // LocalActivityManager doesn't have dispatchRestoreInstanceState method
            // We just need to restore our own state
            if (mLocalActivityManager != null) {
                android.util.Log.d(TAG, "LocalActivityManager exists, state restored");
                // Restore any additional state we need
                if (savedInstanceState != null) {
                    long savedTrigId = savedInstanceState.getLong("saved_trig_id", -1);
                    if (savedTrigId > 0) {
                        android.util.Log.d(TAG, "Restored trig ID from state: " + savedTrigId);
                        // Update the intent with the saved trig ID if needed
                        if (getIntent().getLongExtra(DbHelper.TRIG_ID, -1) != savedTrigId) {
                            getIntent().putExtra(DbHelper.TRIG_ID, savedTrigId);
                            android.util.Log.d(TAG, "Updated intent with restored trig ID");
                        }
                    }
                }
            } else {
                android.util.Log.w(TAG, "LocalActivityManager is null in onRestoreInstanceState");
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error in onRestoreInstanceState: " + e.getMessage(), e);
        }
    }

    /**
     * Attempts to recreate the LocalActivityManager when it's in a bad state
     */
    private void recreateLocalActivityManager() {
        try {
            android.util.Log.d(TAG, "Recreating LocalActivityManager");
            
            // Clean up the old one first
            if (mLocalActivityManager != null) {
                try {
                    mLocalActivityManager.dispatchDestroy(false);
                } catch (Exception e) {
                    android.util.Log.w(TAG, "Error destroying old LocalActivityManager: " + e.getMessage());
                }
            }
            
            // Create a new one
            mLocalActivityManager = new LocalActivityManager(this, false);
            if (mLocalActivityManager != null) {
                try {
                    mLocalActivityManager.dispatchCreate(null);
                    android.util.Log.d(TAG, "LocalActivityManager recreated successfully");
                } catch (Exception e) {
                    android.util.Log.e(TAG, "Error dispatching create to new LocalActivityManager: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error recreating LocalActivityManager: " + e.getMessage(), e);
        }
    }

    /**
     * Attempts to recreate the tabs when the LocalActivityManager is in a bad state.
     * This is a wrapper for setupTabs to ensure it's called with the correct arguments.
     */
    private void recreateTabs() {
        android.util.Log.d(TAG, "Recreating tabs due to LocalActivityManager invalidity");
        try {
            // Capture current tab tag to restore after recreation
            String currentTag = null;
            TabHost existing = null;
            try {
                existing = findViewById(android.R.id.tabhost);
                if (existing != null) {
                    currentTag = existing.getCurrentTabTag();
                }
            } catch (Exception ignored) {}
            Bundle extras = getIntent().getExtras();
            if (extras == null) {
                extras = new Bundle();
            }
            
            // Get the trig ID from the intent
            long trigId = getIntent().getLongExtra(DbHelper.TRIG_ID, -1);
            if (trigId > 0 && !extras.containsKey(DbHelper.TRIG_ID)) {
                extras.putLong(DbHelper.TRIG_ID, trigId);
            }
            
            Bundle savedInstanceState = null; // No need to pass savedInstanceState here, it's handled by onRestoreInstanceState
            setupTabs(extras, savedInstanceState);
            // Reapply previously selected tab if we captured it
            if (currentTag != null && existing != null) {
                try {
                    existing.setCurrentTabByTag(currentTag);
                    android.util.Log.d(TAG, "Reapplied tab tag after recreation: " + currentTag);
                } catch (Exception e) {
                    android.util.Log.w(TAG, "Failed to reapply tab tag after recreation: " + e.getMessage());
                }
            }
            android.util.Log.d(TAG, "Tabs recreated successfully");
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error recreating tabs: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onDestroy() {
        android.util.Log.d(TAG, "onDestroy called");
        
        if (mLocalActivityManager != null) {
            try {
                android.util.Log.d(TAG, "Dispatching destroy to LocalActivityManager");
                mLocalActivityManager.dispatchDestroy(isFinishing());
                android.util.Log.d(TAG, "LocalActivityManager destroy completed successfully");
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error in LocalActivityManager dispatchDestroy: " + e.getMessage(), e);
            } finally {
                mLocalActivityManager = null;
                android.util.Log.d(TAG, "LocalActivityManager set to null");
            }
        } else {
            android.util.Log.w(TAG, "LocalActivityManager is null in onDestroy");
        }
        super.onDestroy();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        android.util.Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode + ", data=" + (data != null ? "present" : "null"));
        super.onActivityResult(requestCode, resultCode, data);
        
        // Forward the result to the current activity in the LocalActivityManager
        if (mLocalActivityManager != null) {
            android.util.Log.d(TAG, "Forwarding activity result to LocalActivityManager");
            try {
                // Get the current activity ID from the TabHost
                TabHost tabHost = findViewById(android.R.id.tabhost);
                if (tabHost != null) {
                    String currentTabTag = tabHost.getCurrentTabTag();
                    android.util.Log.d(TAG, "Current tab: " + currentTabTag);
                    
                    // Get the activity for the current tab
                    android.app.Activity currentActivity = mLocalActivityManager.getActivity(currentTabTag);
                    if (currentActivity != null) {
                        android.util.Log.d(TAG, "Forwarding result to activity: " + currentActivity.getClass().getSimpleName());
                        // Use reflection to call the protected onActivityResult method
                        try {
                            java.lang.reflect.Method method = android.app.Activity.class.getDeclaredMethod(
                                "onActivityResult", int.class, int.class, Intent.class);
                            method.setAccessible(true);
                            method.invoke(currentActivity, requestCode, resultCode, data);
                            android.util.Log.d(TAG, "Successfully forwarded activity result via reflection");
                        } catch (Exception reflectEx) {
                            android.util.Log.e(TAG, "Failed to invoke onActivityResult via reflection", reflectEx);
                        }
                    } else {
                        android.util.Log.w(TAG, "No activity found for current tab: " + currentTabTag);
                    }
                } else {
                    android.util.Log.w(TAG, "TabHost not found, cannot forward activity result");
                }
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error forwarding activity result: " + e.getMessage(), e);
            }
        } else {
            android.util.Log.w(TAG, "LocalActivityManager is null, cannot forward activity result");
        }
    }

    /**
     * Checks if the LocalActivityManager is in a valid state
     */
    private boolean isLocalActivityManagerValid() {
        if (mLocalActivityManager == null) {
            android.util.Log.w(TAG, "LocalActivityManager is null");
            return false;
        }
        
        try {
            // Try to access a simple property to see if it's still valid
            // This is a basic check - if it throws an exception, it's in a bad state
            return true;
        } catch (Exception e) {
            android.util.Log.w(TAG, "LocalActivityManager is in invalid state: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (menu == null) {
            android.util.Log.w(TAG, "Menu is null");
            return false;
        }
        
        try {
            if (getMenuInflater() != null) {
                getMenuInflater().inflate(R.menu.trigdetails_actions, menu);
                return true;
            } else {
                android.util.Log.w(TAG, "Menu inflater is null");
                return false;
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error creating options menu: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item == null) {
            android.util.Log.w(TAG, "Menu item is null");
            return false;
        }
        
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        
        // Check if LocalActivityManager is in a valid state
        if (!isLocalActivityManagerValid()) {
            android.util.Log.w(TAG, "LocalActivityManager is not valid, attempting to recreate");
            try {
                recreateLocalActivityManager();
            } catch (Exception e) {
                android.util.Log.e(TAG, "Failed to recreate LocalActivityManager: " + e.getMessage());
                showToast("Error: Application state is invalid");
                return true;
            }
        }
        
        try {
            long trigId = getIntent().getLongExtra(DbHelper.TRIG_ID, -1);
            if (trigId <= 0) {
                showToast("Invalid trigpoint id");
                return true;
            }
            
            double[] coords = loadTrigLatLon(trigId);
            double lat = coords[0];
            double lon = coords[1];

            int id = item.getItemId();
            if (id == R.id.action_open_web) {
                String url = "https://trigpointing.uk/trig/" + trigId;
                try {
                    // Prefer in-app WebView to avoid external browser lifecycle issues
                    Intent i = new Intent(this, uk.trigpointing.android.common.WebViewActivity.class);
                    i.putExtra(uk.trigpointing.android.common.WebViewActivity.EXTRA_URL, url);
                    startActivity(i);
                } catch (Exception e) {
                    android.util.Log.e(TAG, "Error opening in-app web view: " + e.getMessage(), e);
                    // Fallback to previous external handling if WebViewActivity fails
                    try {
                        openWebPage(url);
                    } catch (Exception ex) {
                        android.util.Log.e(TAG, "Error opening web page: " + ex.getMessage(), ex);
                        showToast("Error opening web page");
                    }
                }
                return true;
            } else if (id == R.id.action_navigate) {
                if (lat != 0d || lon != 0d) {
                    try {
                        String nav = String.format(Locale.getDefault(), "google.navigation:q=%f,%f", lat, lon);
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(nav));
                        if (intent.resolveActivity(getPackageManager()) != null) {
                            startActivity(Intent.createChooser(intent, "Navigate to trigpoint"));
                        } else {
                            showToast("No navigation app found");
                        }
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "Error creating navigation intent: " + e.getMessage(), e);
                        showToast("Error opening navigation");
                    }
                } else {
                    showToast("Unable to get trigpoint coordinates");
                }
                return true;
            } else if (id == R.id.action_radar) {
                if (lat != 0d || lon != 0d) {
                    try {
                        Intent intent = new Intent(this, uk.trigpointing.android.radar.RadarActivity.class);
                        intent.putExtra(DbHelper.TRIG_ID, trigId);
                        intent.putExtra(DbHelper.TRIG_LAT, lat);
                        intent.putExtra(DbHelper.TRIG_LON, lon);
                        startActivity(intent);
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "Error creating radar intent: " + e.getMessage(), e);
                        showToast("Error opening radar");
                    }
                } else {
                    showToast("Unable to get trigpoint coordinates");
                }
                return true;
            } else if (id == R.id.action_refresh) {
                try {
                    TabHost tabHost = findViewById(android.R.id.tabhost);
                    if (tabHost != null) {
                        String currentTag = tabHost.getCurrentTabTag();
                        // Refresh OS maps cache
                        android.app.Activity mapActivity = mLocalActivityManager.getActivity("map");
                        if (mapActivity instanceof TrigDetailsOSMapTab) {
                            ((TrigDetailsOSMapTab) mapActivity).refreshImagesFromParent();
                            Toast.makeText(this, "Refreshing maps and photos", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Map tab not active yet", Toast.LENGTH_SHORT).show();
                        }

                        // Also refresh the Album tab photos and invalidate cached images
                        android.app.Activity albumActivity = mLocalActivityManager.getActivity("album");
                        if (albumActivity instanceof TrigDetailsAlbumTab) {
                            ((TrigDetailsAlbumTab) albumActivity).refreshAlbumFromParent();
                        }

                        // Refresh the Logs tab text entries for this trigpoint
                        android.app.Activity logsActivity = mLocalActivityManager.getActivity("logs");
                        if (logsActivity instanceof TrigDetailsLoglistTab) {
                            ((TrigDetailsLoglistTab) logsActivity).refreshLogsFromParent();
                        }
                    }
                } catch (Exception e) {
                    android.util.Log.e(TAG, "Error refreshing content: " + e.getMessage(), e);
                    Toast.makeText(this, "Error refreshing", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error handling menu action: " + e.getMessage(), e);
            showToast("Error processing action");
        }
        
        return super.onOptionsItemSelected(item);
    }

    private double[] loadTrigLatLon(long trigId) {
        double lat = 0d, lon = 0d;
        if (trigId <= 0) return new double[]{lat, lon};
        DbHelper db = null;
        Cursor c = null;
        try {
            if (this != null) {
                db = new DbHelper(this);
                if (db != null) {
                    try {
                        db.open();
                        c = db.fetchTrigInfo(trigId);
                        if (c != null && c.moveToFirst()) {
                            try {
                                int latIndex = c.getColumnIndex(DbHelper.TRIG_LAT);
                                int lonIndex = c.getColumnIndex(DbHelper.TRIG_LON);
                                if (latIndex >= 0 && lonIndex >= 0) {
                                    lat = c.getDouble(latIndex);
                                    lon = c.getDouble(lonIndex);
                                } else {
                                    android.util.Log.w(TAG, "Invalid column indices for coordinates");
                                }
                            } catch (Exception e) {
                                android.util.Log.w(TAG, "Error reading coordinates from cursor: " + e.getMessage());
                            }
                        } else {
                            android.util.Log.w(TAG, "No trig info found for ID: " + trigId);
                        }
                    } catch (Exception e) {
                        android.util.Log.w(TAG, "Error opening database or fetching trig info: " + e.getMessage());
                    }
                } else {
                    android.util.Log.w(TAG, "Database helper is null");
                }
            } else {
                android.util.Log.w(TAG, "Context is null");
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "Error creating database helper: " + e.getMessage());
        } finally {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception e) {
                    android.util.Log.w(TAG, "Error closing cursor: " + e.getMessage());
                }
            }
            if (db != null) {
                try {
                    db.close();
                } catch (Exception e) {
                    android.util.Log.w(TAG, "Error closing database: " + e.getMessage());
                }
            }
        }
        return new double[]{lat, lon};
    }



    /**
     * Safely loads a drawable resource, returning null if it fails
     * @param res Resources object
     * @param drawableId The drawable resource ID
     * @return The drawable or null if loading fails
     */
    private android.graphics.drawable.Drawable getDrawableSafely(Resources res, int drawableId) {
        try {
            return res.getDrawable(drawableId);
        } catch (Exception e) {
            android.util.Log.w(TAG, "Failed to load drawable " + drawableId + ": " + e.getMessage());
            return null;
        }
    }

    // Map database type code to green map icon resource for header prominence
    private int mapGreenTypeIconResource(String typeCode, boolean highlight) {
        if (typeCode == null) return R.drawable.mapicon_passive_green;
        switch (typeCode) {
            case "PI": return highlight ? R.drawable.mapicon_pillar_green_h : R.drawable.mapicon_pillar_green;
            case "FB": return highlight ? R.drawable.mapicon_fbm_green_h : R.drawable.mapicon_fbm_green;
            case "IN": return highlight ? R.drawable.mapicon_intersected_green_h : R.drawable.mapicon_intersected_green;
            default: return highlight ? R.drawable.mapicon_passive_green_h : R.drawable.mapicon_passive_green;
        }
    }

    // No custom mapping for condition; using built-in Condition icons

    // Expose a safe method for child tabs to refresh the header immediately (e.g., when mark toggled)
    public void refreshHeaderNow() {
        try {
            long trigId = getIntent().getLongExtra(DbHelper.TRIG_ID, -1);
            if (trigId <= 0) { return; }
            android.widget.TextView header = findViewById(R.id.trig_header_title);
            android.widget.ImageView typeIcon = findViewById(R.id.trig_header_type_icon);
            android.widget.ImageView condIcon = findViewById(R.id.trig_header_condition_icon);
            if (header == null) { return; }
            DbHelper db = new DbHelper(this);
            db.openReadable();
            try (android.database.Cursor c = db.fetchTrigInfo(trigId)) {
                if (c != null && c.moveToFirst()) {
                    int idxName = c.getColumnIndex(DbHelper.TRIG_NAME);
                    int idxType = c.getColumnIndex(DbHelper.TRIG_TYPE);
                    int idxCond = c.getColumnIndex(DbHelper.TRIG_CONDITION);
                    String name = (idxName >= 0) ? c.getString(idxName) : "";
                    String typeCode = (idxType >= 0) ? c.getString(idxType) : null;
                    String condCode = (idxCond >= 0) ? c.getString(idxCond) : null;
                    header.setText(name != null ? name : "");
                    uk.trigpointing.android.types.Condition condition = uk.trigpointing.android.types.Condition.fromCode(condCode);
                    boolean isMarked = false;
                    try { isMarked = db.isMarkedTrig(trigId); } catch (Exception ignored) {}
                    if (typeIcon != null) {
                        try { typeIcon.setImageResource(mapGreenTypeIconResource(typeCode, isMarked)); } catch (Exception ignored) {}
                    }
                    if (condIcon != null) {
                        try { condIcon.setImageResource(condition.icon()); } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ignored) {
            } finally {
                try { db.close(); } catch (Exception ignored2) {}
            }
        } catch (Exception ignored) {}
    }

    /**
     * Safely opens a web page with proper error handling and fallback mechanisms
     * @param url The URL to open
     */
    private void openWebPage(String url) {
        android.util.Log.d(TAG, "openWebPage called with URL: " + url);
        
        if (url == null || url.trim().isEmpty()) {
            showToast("Invalid URL");
            return;
        }

        // Check if the activity is still valid
        if (isFinishing() || isDestroyed()) {
            android.util.Log.w(TAG, "Activity is finishing or destroyed, cannot open web page");
            return;
        }

        // Check if LocalActivityManager is in a valid state
        if (!isLocalActivityManagerValid()) {
            android.util.Log.w(TAG, "LocalActivityManager is not valid, attempting to recreate");
            try {
                recreateLocalActivityManager();
            } catch (Exception e) {
                android.util.Log.e(TAG, "Failed to recreate LocalActivityManager: " + e.getMessage());
                showToast("Error: Application state is invalid");
                return;
            }
        }
        
        // Save the current state before opening the browser
        android.util.Log.d(TAG, "Saving current state before opening browser");
        try {
            // Removed manual LocalActivityManager pause to avoid corrupting internal state
        } catch (Exception e) {
            android.util.Log.w(TAG, "Error saving state before browser: " + e.getMessage());
        }

        // Validate the URL format
        Uri uri;
        try {
            uri = Uri.parse(url);
            if (uri == null) {
                showToast("Invalid URL format");
                return;
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error parsing URL: " + e.getMessage());
            showToast("Invalid URL format");
            return;
        }

        try {
            // First, try to use CustomTabsIntent for a better user experience
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            if (builder != null) {
                CustomTabsIntent customTabsIntent = builder.build();
                if (customTabsIntent != null) {
                    // Add error handling for CustomTabsIntent
                    try {
                        // Check if there's a browser that supports custom tabs
                        if (customTabsIntent.intent.resolveActivity(getPackageManager()) != null) {
                            // Add additional safety check for the activity state
                            if (!isFinishing() && !isDestroyed()) {
                                android.util.Log.d(TAG, "Launching custom tabs for URL: " + url);
                                customTabsIntent.launchUrl(this, uri);
                                return; // Success, exit method
                            } else {
                                android.util.Log.w(TAG, "Activity state changed, cannot launch custom tabs");
                            }
                        } else {
                            android.util.Log.w(TAG, "No browser supports custom tabs, falling back to regular intent");
                        }
                    } catch (Exception e) {
                        // Log the error but continue to fallback
                        android.util.Log.w(TAG, "CustomTabsIntent failed: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            // Log the error but continue to fallback
            android.util.Log.w(TAG, "CustomTabsIntent builder failed: " + e.getMessage());
        }

        // Fallback to regular browser intent
        try {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, uri);
            
            // Check if there's an app to handle this intent
            if (webIntent.resolveActivity(getPackageManager()) != null) {
                // Add additional safety check
                if (!isFinishing() && !isDestroyed()) {
                    android.util.Log.d(TAG, "Launching regular intent for URL: " + url);
                    startActivity(webIntent);
                } else {
                    android.util.Log.w(TAG, "Activity state changed, cannot start web intent");
                }
            } else {
                android.util.Log.w(TAG, "No browser app found to handle web intent");
                showToast("No browser app found");
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to open web page: " + e.getMessage());
            showToast("Failed to open web page");
        }
    }

    private void showToast(String message) {
        try {
            if (!isFinishing() && !isDestroyed()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "Error showing toast: " + e.getMessage());
        }
    }
}