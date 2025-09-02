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
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
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
    
    // Handler for auto-repeat on FOV calibration buttons
    private final Handler arFovRepeatHandler = new Handler(Looper.getMainLooper());

    
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
        
        // Calibration buttons with auto-repeat on press-and-hold
        Button narrowerBtn = findViewById(R.id.ar_narrower);
        Button widerBtn = findViewById(R.id.ar_wider);
        if (narrowerBtn != null) {
            setupAutoRepeatButton(narrowerBtn, -0.02f);
        }
        if (widerBtn != null) {
            setupAutoRepeatButton(widerBtn, +0.02f);
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
            boolean hasCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
            if (hasCamera) {
                initializeCamera();
            } else {
                // Device has no camera - show message and continue with location-only AR
                Toast.makeText(this, "Device has no camera - AR will work with location and compass only", Toast.LENGTH_LONG).show();
            }
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
        
        // Resume camera (only if device has camera)
        boolean hasCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        if (camera == null && hasPermissions() && hasCamera) {
            initializeCamera();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // Stop any FOV auto-repeat callbacks
        arFovRepeatHandler.removeCallbacksAndMessages(null);

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
        // Check if device has camera hardware
        boolean hasCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        
        // Location permission is always required
        boolean hasLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        
        // Camera permission only required if device has camera
        boolean hasCameraPermission = !hasCamera || 
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        
        return hasLocation && hasCameraPermission;
    }
    
    private void requestPermissions() {
        // Check if device has camera hardware
        boolean hasCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        
        // Build permission list based on device capabilities
        List<String> permissionsToRequest = new ArrayList<>();
        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        
        if (hasCamera) {
            permissionsToRequest.add(Manifest.permission.CAMERA);
        }
        
        ActivityCompat.requestPermissions(this,
            permissionsToRequest.toArray(new String[0]),
            CAMERA_PERMISSION_REQUEST);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            boolean hasCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
            boolean locationGranted = false;
            boolean cameraGranted = !hasCamera; // If no camera, consider "granted"
            
            // Check which permissions were granted
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    locationGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                } else if (permissions[i].equals(Manifest.permission.CAMERA)) {
                    cameraGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                }
            }
            
            if (locationGranted && cameraGranted) {
                if (hasCamera) {
                    initializeCamera();
                } else {
                    // Device has no camera - show message and continue with location-only AR
                    Toast.makeText(this, "Device has no camera - AR will work with location and compass only", Toast.LENGTH_LONG).show();
                }
                startLocationServices();
            } else {
                String message = hasCamera ? 
                    "Location and camera permissions are required for AR" : 
                    "Location permission is required for AR";
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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
                    // First pass: collect more candidates than needed to ensure we have
                    // enough bearings spanning what landscape can display. We'll keep
                    // at most 10 nearest after bearing filtering.
                    List<AROverlayView.TrigpointData> candidates = new ArrayList<>();
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
                            candidates.add(trigData);
                        }
                        
                    } while (cursor.moveToNext());
                    cursor.close();

                    // Second pass: pick up to 10 nearest among those whose bearings
                    // lie within the maximum FOV that could be displayed at any screen rotation.
                    // Use diagonal FOV for optimal coverage across all orientations.
                    final float maxHorizontalFovDeg = overlayView != null ? overlayView.getDiagonalFieldOfViewDegrees() : 90f;
                    // Sort by distance ascending
                    candidates.sort((a, b) -> {
                        Location la = new Location("a"); la.setLatitude(a.getLat()); la.setLongitude(a.getLon());
                        Location lb = new Location("b"); lb.setLatitude(b.getLat()); lb.setLongitude(b.getLon());
                        return Float.compare(currentLocation.distanceTo(la), currentLocation.distanceTo(lb));
                    });
                    for (AROverlayView.TrigpointData t : candidates) {
                        if (trigpoints.size() >= 10) break;
                        float bearing = currentLocation.bearingTo(new Location("tmp") {{ setLatitude(t.getLat()); setLongitude(t.getLon()); }});
                        if (bearing < 0) bearing += 360f;
                        float rel = bearing - currentAzimuth;
                        while (rel > 180f) rel -= 360f;
                        while (rel < -180f) rel += 360f;
                        if (Math.abs(rel) <= maxHorizontalFovDeg / 2f) {
                            trigpoints.add(t);
                        }
                    }
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

    // No dynamic limit – keep stable list of 10 nearest for a clean UI
    
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

            // Compute camera elevation above horizon using forward vector
            float horizontalMag = (float) Math.sqrt(fx * fx + fy * fy);
            float elevationRad = (float) Math.atan2(fz, horizontalMag); // -90..+90, + up
            float elevationDeg = (float) Math.toDegrees(elevationRad);
            currentPitch = elevationDeg; // reuse field to carry elevation to overlay
            
            // Compute roll so we can rotate overlay to keep horizon level regardless of device rotation
            // Project world-up (0,0,1) into the camera/image plane and measure its angle vs device screen-up
            float rx = rotationMatrix[0], ry = rotationMatrix[3], rz = rotationMatrix[6]; // device +X in world
            float ux = rotationMatrix[1], uy = rotationMatrix[4], uz = rotationMatrix[7]; // device +Y in world (screen up)
            // forward vector f already computed above: (fx, fy, fz)
            float dotUf = 0f * fx + 0f * fy + 1f * fz; // = fz
            float projUx = 0f - dotUf * fx;
            float projUy = 0f - dotUf * fy;
            float projUz = 1f - dotUf * fz;
            // Express projected world-up in device screen basis
            float upInScreenX = projUx * rx + projUy * ry + projUz * rz; // component along device X (right)
            float upInScreenY = projUx * ux + projUy * uy + projUz * uz; // component along device Y (up)
            float rollRad = (float) Math.atan2(upInScreenX, upInScreenY); // angle to rotate so projected up aligns with screen up
            currentRoll = (float) Math.toDegrees(rollRad);

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
                // Reload trigpoints with new FOV to ensure candidate selection uses updated values
                loadNearbyTrigpoints();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to adjust AR FOV scale", e);
        }
    }

    private void setupAutoRepeatButton(Button button, float delta) {
        final Runnable[] repeatTaskHolder = new Runnable[1];
        button.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    // Immediate adjustment on press
                    adjustArFovScale(delta);
                    // Start repeating after a short delay
                    repeatTaskHolder[0] = new Runnable() {
                        @Override public void run() {
                            adjustArFovScale(delta);
                            arFovRepeatHandler.postDelayed(this, 60);
                        }
                    };
                    arFovRepeatHandler.postDelayed(repeatTaskHolder[0], 300);
                    v.setPressed(true);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    if (repeatTaskHolder[0] != null) {
                        arFovRepeatHandler.removeCallbacks(repeatTaskHolder[0]);
                        repeatTaskHolder[0] = null;
                    }
                    return true;
            }
            // Always call performClick for accessibility when touch is handled
            v.performClick();
            return false;
        });
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
