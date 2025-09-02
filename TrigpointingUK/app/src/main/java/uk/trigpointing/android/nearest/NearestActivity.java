package uk.trigpointing.android.nearest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.net.Uri;
import android.provider.Settings;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import uk.trigpointing.android.DbHelper;
import uk.trigpointing.android.R;
import uk.trigpointing.android.common.BaseActivity;
import uk.trigpointing.android.filter.Filter;
import uk.trigpointing.android.trigdetails.TrigDetailsActivity;
import uk.trigpointing.android.types.LatLon;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.style.RelativeSizeSpan;


public class NearestActivity extends BaseActivity implements SensorEventListener {
	private Cursor 					mCursor;
	private Location 				mCurrentLocation;
	private double 					mHeading = 0;
	private NearestCursorAdapter 	mListAdapter;
	private DbHelper 				mDb;
	static int 						mUpdateCount = 0;
	static int 						mLocationCount = 0;
	private boolean 				mTaskRunning = false;
	private LocationListener 		mLocationListener;
	private LocationManager 		mLocationManager;
	TextView						mStrLocation;
	TextView						mStrFilter;
	TextView						mNorthText;
	ImageView						mCompassArrow;
	private SharedPreferences       mPrefs;
	private float[] 				mGravity;
	private float[] 				mGeomagnetic;
	private SensorManager 			mSensorManager;
	private Sensor 					accelerometer;
	private Sensor 					magnetometer;
	private int mOrientation;
	private boolean mUsingCompass;
	private boolean mRelativeMode = false;
	private Location mReferenceLocation; // Source for distance/bearing calculations and sorting
	private String mAnchorWaypoint;
	private String mAnchorName;
	private static final String     USECOMPASS="useCompass";
	private static final String TAG = "NearestActivity";
	private static final int DETAILS = 1;
    private static final int REQ_LOCATION = 1;
	
	// Modern activity result launcher
	private ActivityResultLauncher<Intent> detailsLauncher;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.triglist);
		
		// Enable back button in action bar
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setTitle("Nearest Trig Points");
		}

		// Register activity result launcher for modern navigation
		detailsLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			result -> {
				Log.i(TAG, "onActivityResult");
				refreshList();
				updateFilterHeader();
			}
		);

		// find view references
		mStrLocation 			= findViewById(R.id.trigListLocation);
		
		// Content positioning is now handled by BaseActivity
		mStrFilter	 			= findViewById(R.id.trigListHeader);
		mNorthText	 			= findViewById(R.id.north);
		mCompassArrow			= findViewById(R.id.compassArrow);
		
		// Set up compass area click handler
		findViewById(R.id.compassArea).setOnClickListener(v -> {
			if (!mRelativeMode) {
				useCompass(!mUsingCompass);
			}
		});

		// Clicking the location header exits relative mode (when active)
		mStrLocation.setOnClickListener(v -> {
			if (mRelativeMode) {
				exitRelativeMode();
			}
		});
		
		// Set up trigpoint types filter click handler
		mStrFilter.setOnClickListener(v -> {
			Intent i = new Intent(NearestActivity.this, uk.trigpointing.android.filter.TrigpointTypesActivity.class);
			detailsLauncher.launch(i);
		});
		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		// create various objects
		try {
			mDb = new DbHelper(NearestActivity.this);
			mDb.open();
		} catch (SQLException e) {
			e.printStackTrace();
			Toast.makeText(this, "Error opening database.  Please try again shortly.", Toast.LENGTH_SHORT).show();
			finish();
		}
		
		// Set up ListView since we're no longer using ListActivity
		ListView listView = findViewById(android.R.id.list);
		if (listView != null) {
			listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
					onListItemClick((ListView) parent, view, position, id);
				}
			});
		}
	
		// Start off with no location + no trigs
		mListAdapter = new NearestCursorAdapter(this, R.layout.trigrow, null, new String[]{}, new int[]{}, null);
		if (listView != null) {
			listView.setAdapter(mListAdapter);
		}

		// Find a cached location
		mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

		// Initialize sensors early so onResume/useCompass never sees nulls even if we return early
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		accelerometer = mSensorManager != null ? mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
		magnetometer = mSensorManager != null ? mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) : null;
		
		// Define a listener that responds to location updates (init early so it's never null)
		mLocationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				mLocationCount++;
				updateLocationHeader("listener");
				if (isBetterLocation(location, mCurrentLocation)) {
					mCurrentLocation = location;
					refreshList();
				}
			}
			public void onStatusChanged(String provider, int status, Bundle extras) {}
			public void onProviderEnabled(String provider) {}
			public void onProviderDisabled(String provider) {}
		};
		
		// Check for location permissions
		if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
			checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			// Request location permissions
			requestPermissions(new String[]{
				android.Manifest.permission.ACCESS_FINE_LOCATION,
				android.Manifest.permission.ACCESS_COARSE_LOCATION
			}, REQ_LOCATION);
			updateLocationHeader("permission_required");
			return;
		} else {
			// We have permission, proceed with location access
			// Try to get last known location from available providers
			mCurrentLocation = getLastKnownLocationFromAvailableProviders();
			updateLocationHeader("cached");
			if (mCurrentLocation != null && !mTaskRunning) {findTrigs();}
		}
		

		// Sensors already initialized above
		
		// Is the screen rotated? (using modern API instead of deprecated getDefaultDisplay/getOrientation)
		WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		// Use modern rotation detection
		mOrientation = getResources().getConfiguration().orientation;
		
		Log.i(TAG, "getOrientation(): " + mOrientation);
		mListAdapter.setOrientation(mOrientation);

		// Handle possible relative-mode launch
		handleIncomingIntent(getIntent());
		 
	}


	

	
	@Override
	protected void onPause() {
		// save compass preference (but don't overwrite during relative mode)
		if (!mRelativeMode) {
			Editor editor = mPrefs.edit();
			editor.putBoolean(USECOMPASS, mUsingCompass);
			editor.apply();
		}
		// stop listening to the GPS and compass
		try {
			if (mLocationManager != null && (
					checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
					checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
				mLocationManager.removeUpdates(mLocationListener);
			}
		} catch (SecurityException ignore) {
			// If we do not have permission yet, ignore
		}
		useCompass(false);
		super.onPause();
	}





	@Override
	protected void onResume() {
		super.onResume();
		// Register the listener with the Location Manager to receive location updates
		if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
			checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			// Request location updates from available providers only
			requestLocationUpdatesFromAvailableProviders();
		}
		else {
			// If returning here after a permission denial, ensure headers/UI reflect state
			updateLocationHeader("permission_required");
		}
		// Decide whether to use the compass
		if (mRelativeMode) {
			useCompass(false);
		} else {
			useCompass(mPrefs.getBoolean(USECOMPASS, false));
		}
		// Setup header icons
		updateFilterHeader();
	}

	/**
	 * Safely get last known location from available providers
	 * Network provider was deprecated and removed in newer Android versions
	 */
	private Location getLastKnownLocationFromAvailableProviders() {
		Location bestLocation = null;
		
		try {
			// Try GPS provider first (most accurate)
			if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				Location gpsLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				if (isBetterLocation(gpsLocation, bestLocation)) {
					bestLocation = gpsLocation;
				}
			}
			
			// Try network provider only if it exists (deprecated/removed on newer devices)
			try {
				if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
					Location networkLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
					if (isBetterLocation(networkLocation, bestLocation)) {
						bestLocation = networkLocation;
					}
				}
			} catch (IllegalArgumentException e) {
				// Network provider doesn't exist on this device - this is expected on newer Android versions
				Log.d(TAG, "Network provider not available (this is normal on newer Android versions)");
			}
			
			// Try passive provider as fallback
			try {
				if (mLocationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
					Location passiveLocation = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
					if (isBetterLocation(passiveLocation, bestLocation)) {
						bestLocation = passiveLocation;
					}
				}
			} catch (IllegalArgumentException e) {
				Log.d(TAG, "Passive provider not available");
			}
			
		} catch (SecurityException e) {
			Log.w(TAG, "No location permission for getLastKnownLocation");
		} catch (Exception e) {
			Log.e(TAG, "Error getting last known location", e);
		}
		
		return bestLocation;
	}
	
	/**
	 * Safely request location updates from available providers
	 */
	private void requestLocationUpdatesFromAvailableProviders() {
		try {
			// Always try GPS provider (most accurate, available on all devices)
			if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000*300, 250, mLocationListener);
				Log.d(TAG, "Requested location updates from GPS provider");
			}
			
			// Try network provider only if it exists (deprecated/removed on newer devices)
			try {
				if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
					mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000*30, 250, mLocationListener);
					Log.d(TAG, "Requested location updates from network provider");
				}
			} catch (IllegalArgumentException e) {
				// Network provider doesn't exist on this device - this is expected on newer Android versions
				Log.d(TAG, "Network provider not available for location updates (this is normal on newer Android versions)");
			}
			
		} catch (SecurityException e) {
			Log.w(TAG, "No location permission for requestLocationUpdates");
		} catch (Exception e) {
			Log.e(TAG, "Error requesting location updates", e);
		}
	}

	private void useCompass(boolean use) {
		mUsingCompass = use;
		mListAdapter.setUsingCompass(use);
		if (mUsingCompass) {
			mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		    mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
		    					mNorthText.setTextColor(ContextCompat.getColor(this, R.color.compassEnabled));
	    } else {
		    mSensorManager.unregisterListener(this);
		    mHeading = 0;
		    mCompassArrow.setImageResource(mListAdapter.getArrow(0));
		    					mNorthText.setTextColor(ContextCompat.getColor(this, R.color.compassDisabled));
        	mListAdapter.setHeading(0);
        	mListAdapter.notifyDataSetChanged();
		}
	}
	


	private void refreshList() {
		refreshList(false);
	}

	private void refreshList(boolean force) {
		if (force) {
			findTrigs();
		} else if (!mTaskRunning) {
			findTrigs();
		}
	}
	
	
	private void updateLocationHeader(String comment) {
		if (mRelativeMode) {
			String waypointText = "Near to " + (mAnchorWaypoint != null ? mAnchorWaypoint : "selected trig");
			String nameText = (mAnchorName != null && mAnchorName.length() > 0) ? ("\n" + mAnchorName) : "";
			SpannableStringBuilder ssb = new SpannableStringBuilder(waypointText + nameText);
			// Bold the first line (waypoint)
			ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, waypointText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			// Make the second line smaller if present
			if (nameText.length() > 0) {
				ssb.setSpan(new RelativeSizeSpan(0.85f), waypointText.length(), waypointText.length() + nameText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			mStrLocation.setText(ssb);
			Log.d(TAG, "Relative mode header updated. " + comment);
			return;
		}
		if (null != mCurrentLocation) {
			LatLon ll = new LatLon(mCurrentLocation);
			mStrLocation.setTypeface(null, android.graphics.Typeface.NORMAL);
			mStrLocation.setText(String.format("Near to %s   (from %s)" 
					, mCurrentLocation.getProvider().equals("gps") ? ll.getOSGB10() : ll.getOSGB6()
					, mCurrentLocation.getProvider()
			));
			Log.d(TAG, "Location update count : " + mUpdateCount + " Location count : " + mLocationCount + " " + comment);
		} else {
			mStrLocation.setTypeface(null, android.graphics.Typeface.NORMAL);
			mStrLocation.setText(getString(R.string.location_unknown));
			Log.d(TAG, "Location unknown : " + mUpdateCount + " Location count : " + mLocationCount + " " + comment);
		}
	}

	private void updateFilterHeader() {
		// Get trigpoint type text
		int filterType = mPrefs.getInt(Filter.FILTERTYPE, 6); // Default to "All Types"
		String typeText = getTypeTextFromIndex(filterType);
		
		// Get logging status text directly from Filter.FILTERRADIO for consistency
		int filterRadio = mPrefs.getInt(Filter.FILTERRADIO, 0); // Default to "Logged or not"
		String statusText = getStatusTextFromIndex(filterRadio);
		
		// Create two-line display
		String headerText = typeText + "\n" + statusText;
		mStrFilter.setText(headerText);
		Log.d(TAG, "Updated filter header: " + typeText + " / " + statusText);
	}
	
	private String getTypeTextFromIndex(int filterType) {
		switch (filterType) {
			case 0: return "Pillars Only";
			case 1: return "Pillars + FBM";
			case 2: return "FBM Only";
			case 3: return "Passive Stations";
			case 4: return "Intersected Stations";
			case 5: return "All except Intersected";
			case 6: return "All Types";
			default: return "All Types";
		}
	}
	
	private String getStatusTextFromIndex(int filterRadio) {
		switch (filterRadio) {
			case 0: return "Logged or not";
			case 1: return "Logged";
			case 2: return "Not Logged";
			case 3: return "Marked";
			case 4: return "Unsynced";
			default: return "Logged or not";
		}
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.nearestmenu, menu);
        return result;
    }    
    
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		
		if (itemId == android.R.id.home) {
			// Handle back button in action bar
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
        
	@Override
	protected void onDestroy() {
		try {
			mDb.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.onDestroy();
	}

    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent i = new Intent(this, TrigDetailsActivity.class);
        i.putExtra(DbHelper.TRIG_ID, id);
        Log.i(TAG, "Trig_id = " +id);
        detailsLauncher.launch(i);
    }
	
	
    
	

	
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == REQ_LOCATION) {
			boolean granted = false;
			if (grantResults != null && grantResults.length > 0) {
				for (int res : grantResults) {
					if (res == PackageManager.PERMISSION_GRANTED) {
						granted = true;
						break;
					}
				}
			}
			if (granted) {
				// Check permissions explicitly before calling getLastKnownLocation
				if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
					ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
					// Try to get last known location from available providers
					mCurrentLocation = getLastKnownLocationFromAvailableProviders();
					updateLocationHeader("cached");
					if (mCurrentLocation != null && !mTaskRunning) {findTrigs();}
				}
			} else {
				boolean showRationaleFine = shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION);
				boolean showRationaleCoarse = shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_COARSE_LOCATION);
				if (showRationaleFine || showRationaleCoarse) {
					new androidx.appcompat.app.AlertDialog.Builder(this)
							.setTitle("Location permission required")
							.setMessage("This feature needs your location to find nearby trig points.")
							.setPositiveButton("Try again", (d, which) -> requestPermissions(new String[]{
									android.Manifest.permission.ACCESS_FINE_LOCATION,
									android.Manifest.permission.ACCESS_COARSE_LOCATION
							}, REQ_LOCATION))
							.setNegativeButton("Cancel", (d, w) -> Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show())
							.show();
				} else {
					new androidx.appcompat.app.AlertDialog.Builder(this)
							.setTitle("Enable location in Settings")
							.setMessage("Permission was permanently denied. Open App Settings to enable it.")
							.setPositiveButton("Open Settings", (d, which) -> openAppSettings())
							.setNegativeButton("Cancel", (d, w) -> Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show())
							.show();
				}
				updateLocationHeader("permission_denied");
			}
		}
	}

	private void openAppSettings() {
		Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
		Uri uri = Uri.fromParts("package", getPackageName(), null);
		intent.setData(uri);
		startActivity(intent);
	}




	private void findTrigs() {
		Log.i(TAG, "FindTrigsTask.onPreExecute");
		mTaskRunning = true;
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Handler mainHandler = new Handler(Looper.getMainLooper());
		
		CompletableFuture.supplyAsync(() -> {
			Log.i(TAG, "FindTrigsTask.doInBackground");
			Cursor c = null;
			try {
				Location source = mRelativeMode ? mReferenceLocation : mCurrentLocation;
				c = mDb.fetchTrigList(source);
				// startManagingCursor is deprecated - cursor will be managed manually
			} catch (Exception e) {
				e.printStackTrace();
			}
			mUpdateCount++;
			return c;
		}, executor)
		.thenAcceptAsync(cursor -> {
			Log.i(TAG, "FindTrigsTask.onPostExecute " + cursor);
			try {
				mCursor = cursor;
				Location source = mRelativeMode ? mReferenceLocation : mCurrentLocation;
				mListAdapter.swapCursor(mCursor, source);
			} catch (Exception e) {
				e.printStackTrace();
				Location source = mRelativeMode ? mReferenceLocation : mCurrentLocation;
				mListAdapter.swapCursor(null, source);
			}
			updateLocationHeader("task");
			mTaskRunning = false;
		}, mainHandler::post);
	}

	private void handleIncomingIntent(Intent intent) {
		if (intent == null) { return; }
		if (intent.hasExtra("extra_anchor_waypoint") && intent.hasExtra("extra_anchor_lat") && intent.hasExtra("extra_anchor_lon")) {
			String waypoint = intent.getStringExtra("extra_anchor_waypoint");
			String name = intent.getStringExtra("extra_anchor_name");
			double lat = intent.getDoubleExtra("extra_anchor_lat", Double.NaN);
			double lon = intent.getDoubleExtra("extra_anchor_lon", Double.NaN);
			if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
				enterRelativeMode(waypoint, name, lat, lon);
			}
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		handleIncomingIntent(intent);
	}

	private void enterRelativeMode(String waypoint, String name, double lat, double lon) {
		mRelativeMode = true;
		mAnchorWaypoint = waypoint;
		mAnchorName = name;
		mReferenceLocation = new Location("anchor");
		mReferenceLocation.setLatitude(lat);
		mReferenceLocation.setLongitude(lon);
		// Lock compass to north-up and ignore toggles
		useCompass(false);
		updateLocationHeader("enter_relative");
		refreshList(true);
	}

	private void exitRelativeMode() {
		mRelativeMode = false;
		mAnchorWaypoint = null;
		mAnchorName = null;
		mReferenceLocation = mCurrentLocation;
		// Restore compass preference state
		useCompass(mPrefs.getBoolean(USECOMPASS, false));
		updateLocationHeader("exit_relative");
		refreshList(true);
	}






	/** Determines whether one Location reading is better than the current Location fix
	 * @param location  The new Location that you want to evaluate
	 * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	 */
	private static final int TWO_MINUTES = 1000 * 60 * 2;
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}
		
		if (location == null) {
			// A null location is never better than anything
			return false;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else return isNewer && !isSignificantlyLessAccurate && isFromSameProvider;
    }

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}




	// handle compass events
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}	
	@Override
	public void onSensorChanged(SensorEvent event) {
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			mGravity = event.values;
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
	        mGeomagnetic = event.values;
	        break;
		}

		if (!mUsingCompass) {return;}
		
		if (mGravity != null && mGeomagnetic != null) {
	    	float[] R = new float[9];
	        float[] I = new float[9];
	        boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
	        if (success) {
	        	float[] orientation = new float[3];
	        	SensorManager.getOrientation(R, orientation);
	        	mHeading = orientation[0] * 180.0/Math.PI; // orientation contains: azimuth[0], pitch[1] and roll[2]
	        	//Log.d(TAG, "Heading = " + mHeading);
	        	mListAdapter.setHeading(mHeading);
				mListAdapter.notifyDataSetChanged();
				mCompassArrow.setImageResource(mListAdapter.getArrow(-mHeading));
	        }
	    }
	}





}
