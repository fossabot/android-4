package uk.trigpointing.android.ar;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

import uk.trigpointing.android.DbHelper;
import uk.trigpointing.android.R;
import uk.trigpointing.android.common.BaseActivity;
import uk.trigpointing.android.filter.Filter;
import uk.trigpointing.android.types.Trig;
import uk.trigpointing.android.trigdetails.TrigDetailsActivity;

/**
 * Sensor-based AR Activity that uses pure compass and sensor data
 * to display trigpoint icons overlaid on camera view.
 * 
 * This approach prioritizes magnetic compass bearings over visual tracking
 * and works even when the camera is covered.
 */
public class SensorARActivity extends BaseActivity implements SensorEventListener, LocationListener {
    
    private static final String TAG = "SensorARActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 1001;
    private static final int LOCATION_PERMISSION_REQUEST = 1002;
    private static final double MAX_DISTANCE_METERS = 5000; // 5km max distance
    
    // Camera components
    private CameraPreview cameraPreview;
    private Camera camera;
    
    // Sensor components
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private Sensor magneticSensor;
    private Sensor accelerometerSensor;
    
    // Orientation data
    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];
    private float currentAzimuth = 0.0f; // Compass bearing in degrees
    private float currentPitch = 0.0f;   // Tilt up/down
    private float currentRoll = 0.0f;    // Tilt left/right
    
    // Location and database
    private LocationManager locationManager;
    private Location currentLocation;
    private DbHelper dbHelper;
    private List<AROverlayView.TrigpointData> nearbyTrigpoints = new ArrayList<>();
    
    // UI components
    private AROverlayView overlayView;
    
    // Activity lifecycle flag
    private volatile boolean isDestroyed = false;
    

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_ar);
        
        Log.i(TAG, "onCreate: Starting Sensor AR Activity");
        
        // Set up action bar with back arrow
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("AR View");
        }
        
        // Initialize UI components
        cameraPreview = findViewById(R.id.camera_preview);
        overlayView = findViewById(R.id.ar_overlay);
        if (overlayView != null) {
            overlayView.setOnTrigpointClickListener(trigId -> {
                try {
                    Intent i = new Intent(SensorARActivity.this, TrigDetailsActivity.class);
                    i.putExtra(DbHelper.TRIG_ID, trigId);
                    startActivity(i);
                } catch (Exception e) {
                    Toast.makeText(SensorARActivity.this, "Unable to open trig details", Toast.LENGTH_LONG).show();
                }
            });
        }
        
        // Calibration buttons
        Button narrowerBtn = findViewById(R.id.ar_narrower);
        Button widerBtn = findViewById(R.id.ar_wider);
        if (narrowerBtn != null && widerBtn != null) {
            narrowerBtn.setOnClickListener(v -> adjustArFovScale(-0.02f));
            widerBtn.setOnClickListener(v -> adjustArFovScale(+0.02f));
        }
        
        // Initialize sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        
        // Initialize database helper
        try {
            dbHelper = new DbHelper(this);
            dbHelper.open();
            Log.i(TAG, "Database helper initialized and opened successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize database helper", e);
            dbHelper = null;
        }
        
        // Check permissions and start
        if (hasPermissions()) {
            initializeCamera();
            startLocationServices();
        } else {
            requestPermissions();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Register sensor listeners
        if (sensorManager != null) {
            if (rotationSensor != null) {
                sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
            }
            if (magneticSensor != null) {
                sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_GAME);
            }
            if (accelerometerSensor != null) {
                sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
            }
        }
        
        // Resume camera
        if (camera == null && hasPermissions()) {
            initializeCamera();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // Unregister sensor listeners
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        
        // Release camera
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Set flag to prevent further database access
        isDestroyed = true;
        
        if (dbHelper != null) {
            try {
                dbHelper.close();
                Log.i(TAG, "Database helper closed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error closing database helper", e);
            }
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back arrow click
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
            new String[]{Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION},
            CAMERA_PERMISSION_REQUEST);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeCamera();
                startLocationServices();
            } else {
                Toast.makeText(this, "Camera and location permissions are required for AR", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    
    private void initializeCamera() {
        try {
            camera = Camera.open();
            cameraPreview.setCamera(camera);
            Log.i(TAG, "Camera initialized successfully");
            // Set overlay FOVs based on camera parameters and user calibration
            if (overlayView != null) {
                float fovX = getEffectiveFovForScreenWidth();
                float fovY = getEffectiveFovForScreenHeight();
                overlayView.setFieldOfViewDegrees(fovX, fovY);
                Log.i(TAG, "AR overlay FOV set to X=" + fovX + "°, Y=" + fovY + "°");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize camera", e);
            Toast.makeText(this, "Failed to access camera", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void startLocationServices() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        
        if (locationManager != null) {
            try {
                // Request location updates
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this);
                    
                    // Try to get last known location
                    Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastKnown == null) {
                        lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                    if (lastKnown != null) {
                        onLocationChanged(lastKnown);
                    }
                    
                    Log.i(TAG, "Location services started");
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Location permission denied", e);
            }
        }
    }
    
    @Override
    public void onLocationChanged(Location location) {
        if (isDestroyed) {
            return;
        }
        
        currentLocation = location;
        Log.i(TAG, "New location: " + location.getLatitude() + ", " + location.getLongitude());
        loadNearbyTrigpoints();
    }
    
    @Override
    public void onProviderEnabled(String provider) {}
    
    @Override
    public void onProviderDisabled(String provider) {}
    
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
    
    private void loadNearbyTrigpoints() {
        if (currentLocation == null || dbHelper == null || isDestroyed) {
            return;
        }
        
        new Thread(() -> {
            try {
                // Check again if activity was destroyed while thread was starting
                if (isDestroyed) {
                    Log.i(TAG, "loadNearbyTrigpoints: Activity destroyed, aborting database query");
                    return;
                }
                
                double lat = currentLocation.getLatitude();
                double lon = currentLocation.getLongitude();
                
                // Query database for nearby trigpoints with user's filter preferences
                Log.i(TAG, "loadNearbyTrigpoints: Applying user's trigpoint type filters");
                Cursor cursor = dbHelper.fetchTrigList(currentLocation);
                List<AROverlayView.TrigpointData> trigpoints = new ArrayList<>();
                
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        long id = cursor.getLong(0);
                        String name = cursor.getString(1);
                        double trigLat = cursor.getDouble(2);
                        double trigLon = cursor.getDouble(3);
                        String type = cursor.getString(4);
                        String condition = cursor.getString(5);
                        
                        // Calculate distance
                        Location trigLocation = new Location("trigpoint");
                        trigLocation.setLatitude(trigLat);
                        trigLocation.setLongitude(trigLon);
                        float distance = currentLocation.distanceTo(trigLocation);
                        
                        // Only include trigpoints within max distance
                        if (distance <= MAX_DISTANCE_METERS) {
                            AROverlayView.TrigpointData trigData = new AROverlayView.TrigpointData(id, name, trigLat, trigLon, type, condition);
                            trigpoints.add(trigData);
                        }
                        
                        // Limit to 10 nearest trigpoints
                        if (trigpoints.size() >= 10) {
                            break;
                        }
                        
                    } while (cursor.moveToNext());
                    cursor.close();
                }
                
                // Update UI on main thread (if activity still exists)
                runOnUiThread(() -> {
                    if (!isDestroyed && overlayView != null) {
                        nearbyTrigpoints = trigpoints;
                        overlayView.setCurrentLocation(currentLocation);
                        overlayView.updateTrigpoints(trigpoints);
                        Log.i(TAG, "Loaded " + trigpoints.size() + " nearby trigpoints");
                    } else {
                        Log.i(TAG, "loadNearbyTrigpoints: Activity destroyed, skipping UI update");
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading nearby trigpoints", e);
            }
        }).start();
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isDestroyed) {
            return;
        }
        
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            // Build rotation matrix from the rotation vector
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

            // Derive camera heading: project the camera-forward vector (device -Z) into world X/Y (east/north)
            // rotationMatrix is row-major: [0..2; 3..5; 6..8]
            float fx = -rotationMatrix[2];  // world X component of camera-forward
            float fy = -rotationMatrix[5];  // world Y component of camera-forward
            float fz = -rotationMatrix[8];  // world Z component of camera-forward (not used for heading)

            // Heading relative to magnetic north, increasing eastward
            float headingRad = (float) Math.atan2(fx, fy);
            float headingDeg = (float) Math.toDegrees(headingRad);
            if (headingDeg < 0) headingDeg += 360f;
            currentAzimuth = headingDeg;

            // Also compute pitch/roll for potential vertical placement (kept from orientation for now)
            SensorManager.getOrientation(rotationMatrix, orientationAngles);
            currentPitch = (float) Math.toDegrees(orientationAngles[1]);
            currentRoll = (float) Math.toDegrees(orientationAngles[2]);

            // Update overlay with new orientation
            if (overlayView != null) {
                overlayView.updateOrientation(currentAzimuth, currentPitch, currentRoll);
            }
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle sensor accuracy changes if needed
    }

    private void adjustArFovScale(float delta) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            float scale = prefs.getFloat("ar_fov_scale", 1.0f);
            scale += delta;
            // Clamp defensively
            if (scale < 0.5f) scale = 0.5f;
            if (scale > 1.5f) scale = 1.5f;
            prefs.edit().putFloat("ar_fov_scale", scale).apply();
            if (overlayView != null) {
                overlayView.setFieldOfViewDegrees(getEffectiveFovForScreenWidth(), getEffectiveFovForScreenHeight());
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to adjust AR FOV scale", e);
        }
    }

    // Compute FOV to use for the horizontal spread of the overlay in current orientation.
    // In portrait we rotate preview by 90°, so the camera's vertical FOV maps to screen width.
    private float getEffectiveFovForScreenWidth() {
        float fallback = 60f;
        try {
            float base;
            if (camera != null) {
                Camera.Parameters p = camera.getParameters();
                float h = p != null ? p.getHorizontalViewAngle() : 0f;
                float v = p != null ? p.getVerticalViewAngle() : 0f;
                base = (v > 0f ? v : (h > 0f ? h : fallback));
            } else {
                base = fallback;
            }
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            float scale = prefs.getFloat("ar_fov_scale", 1.0f);
            if (scale < 0.5f) scale = 0.5f;
            if (scale > 1.5f) scale = 1.5f;
            return base * scale;
        } catch (Exception e) {
            Log.w(TAG, "Unable to read camera FOV; using fallback", e);
            return fallback;
        }
    }

    // Compute FOV to use for the vertical spread across screen height in current orientation.
    // In portrait we rotate preview by 90°, so the camera's horizontal FOV maps to screen height.
    private float getEffectiveFovForScreenHeight() {
        float fallback = 45f;
        try {
            float base;
            if (camera != null) {
                Camera.Parameters p = camera.getParameters();
                float h = p != null ? p.getHorizontalViewAngle() : 0f;
                float v = p != null ? p.getVerticalViewAngle() : 0f;
                base = (h > 0f ? h : (v > 0f ? v : fallback));
            } else {
                base = fallback;
            }
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            float scale = prefs.getFloat("ar_fov_scale", 1.0f);
            if (scale < 0.5f) scale = 0.5f;
            if (scale > 1.5f) scale = 1.5f;
            return base * scale;
        } catch (Exception e) {
            Log.w(TAG, "Unable to read camera FOV (height); using fallback", e);
            return fallback;
        }
    }
    

}
