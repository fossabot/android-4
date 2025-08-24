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
import uk.trigpointing.android.types.Trig;

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
    private List<TrigpointData> nearbyTrigpoints = new ArrayList<>();
    
    // UI components
    private TextView tvInstructions;
    private TextView tvTrigpointCount;
    private AROverlayView overlayView;
    
    // Simple data holder for trigpoint information
    private static class TrigpointData {
        long id;
        String name;
        double lat;
        double lon;
        String type;
        String condition;
        
        TrigpointData(long id, String name, double lat, double lon, String type, String condition) {
            this.id = id;
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.type = type;
            this.condition = condition;
        }
        
        public long getId() { return id; }
        public String getName() { return name; }
        public double getLat() { return lat; }
        public double getLon() { return lon; }
        public String getType() { return type; }
        public String getCondition() { return condition; }
    }
    
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
        tvInstructions = findViewById(R.id.tvInstructions);
        tvTrigpointCount = findViewById(R.id.tvTrigpointCount);
        cameraPreview = findViewById(R.id.camera_preview);
        overlayView = findViewById(R.id.ar_overlay);
        
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
        currentLocation = location;
        Log.i(TAG, "New location: " + location.getLatitude() + ", " + location.getLongitude());
        loadNearbyTrigpoints();
    }
    
    public void onLocationProviderEnabled(String provider) {}
    
    public void onLocationProviderDisabled(String provider) {}
    
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
    
    private void loadNearbyTrigpoints() {
        if (currentLocation == null || dbHelper == null) {
            return;
        }
        
        new Thread(() -> {
            try {
                double lat = currentLocation.getLatitude();
                double lon = currentLocation.getLongitude();
                
                // Query database for nearby trigpoints
                Cursor cursor = dbHelper.fetchTrigList(currentLocation);
                List<TrigpointData> trigpoints = new ArrayList<>();
                
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
                            TrigpointData trigData = new TrigpointData(id, name, trigLat, trigLon, type, condition);
                            trigpoints.add(trigData);
                        }
                        
                    } while (cursor.moveToNext());
                    cursor.close();
                }
                
                // Update UI on main thread
                runOnUiThread(() -> {
                    nearbyTrigpoints = trigpoints;
                    tvTrigpointCount.setText("Found " + trigpoints.size() + " trigpoints within 5km");
                    overlayView.updateTrigpoints(trigpoints);
                    Log.i(TAG, "Loaded " + trigpoints.size() + " nearby trigpoints");
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading nearby trigpoints", e);
                runOnUiThread(() -> tvTrigpointCount.setText("Error loading trigpoints"));
            }
        }).start();
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            // Get rotation matrix from rotation vector
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            
            // Get orientation angles
            SensorManager.getOrientation(rotationMatrix, orientationAngles);
            
            // Convert to degrees
            currentAzimuth = (float) Math.toDegrees(orientationAngles[0]);
            currentPitch = (float) Math.toDegrees(orientationAngles[1]);
            currentRoll = (float) Math.toDegrees(orientationAngles[2]);
            
            // Normalize azimuth to 0-360 degrees
            if (currentAzimuth < 0) {
                currentAzimuth += 360;
            }
            
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
    
    private int getTrigpointIconResource(TrigpointData trigpoint) {
        // For AR view, always use bright green "good condition" icons for visibility
        Trig.Physical physicalType = Trig.Physical.fromCode(trigpoint.getType());
        // Always use the highlighted (bright green) version for AR visibility
        return physicalType.icon(true); // true = use highlighted/bright version
    }
    
    /**
     * Custom view that handles camera preview
     */
    public static class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder holder;
        private Camera camera;
        
        public CameraPreview(Context context, AttributeSet attrs) {
            super(context, attrs);
            holder = getHolder();
            holder.addCallback(this);
        }
        
        public void setCamera(Camera camera) {
            this.camera = camera;
            if (holder.getSurface() != null) {
                startPreview();
            }
        }
        
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            startPreview();
        }
        
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (camera != null) {
                camera.stopPreview();
                startPreview();
            }
        }
        
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (camera != null) {
                camera.stopPreview();
            }
        }
        
        private void startPreview() {
            if (camera != null && holder.getSurface() != null) {
                try {
                    camera.setPreviewDisplay(holder);
                    camera.startPreview();
                } catch (Exception e) {
                    Log.e(TAG, "Error starting camera preview", e);
                }
            }
        }
    }
    
    /**
     * Custom view that draws trigpoint overlays on top of camera
     */
    public class AROverlayView extends View {
        private Paint textPaint;
        private Paint iconPaint;
        private List<TrigpointData> trigpoints = new ArrayList<>();
        private float deviceAzimuth = 0;
        private float devicePitch = 0;
        
        public AROverlayView(Context context, AttributeSet attrs) {
            super(context, attrs);
            initPaints();
        }
        
        private void initPaints() {
            textPaint = new Paint();
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(40);
            textPaint.setAntiAlias(true);
            textPaint.setShadowLayer(2, 1, 1, Color.BLACK);
            
            iconPaint = new Paint();
            iconPaint.setAntiAlias(true);
        }
        
        public void updateTrigpoints(List<TrigpointData> trigpoints) {
            this.trigpoints = new ArrayList<>(trigpoints);
            invalidate(); // Trigger redraw
        }
        
        public void updateOrientation(float azimuth, float pitch, float roll) {
            this.deviceAzimuth = azimuth;
            this.devicePitch = pitch;
            invalidate(); // Trigger redraw
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            if (trigpoints.isEmpty() || currentLocation == null) {
                return;
            }
            
            int screenWidth = getWidth();
            int screenHeight = getHeight();
            float fieldOfView = 60.0f; // Degrees, typical phone camera FOV
            
            for (TrigpointData trig : trigpoints) {
                // Calculate bearing to trigpoint
                Location trigLocation = new Location("trigpoint");
                trigLocation.setLatitude(trig.getLat());
                trigLocation.setLongitude(trig.getLon());
                
                float bearing = currentLocation.bearingTo(trigLocation);
                if (bearing < 0) bearing += 360; // Normalize to 0-360
                
                float distance = currentLocation.distanceTo(trigLocation);
                
                // Calculate relative bearing (difference between trigpoint bearing and device bearing)
                float relativeBearing = bearing - deviceAzimuth;
                
                // Normalize relative bearing to -180 to 180
                while (relativeBearing > 180) relativeBearing -= 360;
                while (relativeBearing < -180) relativeBearing += 360;
                
                // Only draw trigpoints within field of view
                if (Math.abs(relativeBearing) <= fieldOfView / 2) {
                    // Calculate screen position
                    float screenX = screenWidth / 2 + (relativeBearing / (fieldOfView / 2)) * (screenWidth / 2);
                    
                    // For now, put all trigpoints at horizon level (center Y)
                    // TODO: Adjust for elevation differences using devicePitch
                    float screenY = screenHeight / 2;
                    
                    // Draw trigpoint icon
                    drawTrigpointIcon(canvas, trig, screenX, screenY, distance);
                }
            }
        }
        
        private void drawTrigpointIcon(Canvas canvas, TrigpointData trig, float x, float y, float distance) {
            try {
                // Get trigpoint icon
                int iconRes = getTrigpointIconResource(trig);
                Drawable icon = ContextCompat.getDrawable(getContext(), iconRes);
                
                if (icon != null) {
                    // Scale icon based on distance (closer = larger, but with reasonable limits)
                    float scale = Math.max(0.3f, Math.min(1.0f, 1000.0f / distance));
                    int iconSize = (int) (64 * scale); // Base size 64dp
                    
                    // Draw icon
                    icon.setBounds(
                        (int) (x - iconSize / 2),
                        (int) (y - iconSize / 2),
                        (int) (x + iconSize / 2),
                        (int) (y + iconSize / 2)
                    );
                    icon.draw(canvas);
                    
                    // Draw trigpoint name below icon
                    String text = trig.getName();
                    Rect textBounds = new Rect();
                    textPaint.getTextBounds(text, 0, text.length(), textBounds);
                    
                    float textX = x - textBounds.width() / 2;
                    float textY = y + iconSize / 2 + textBounds.height() + 10;
                    
                    // Draw text background
                    canvas.drawRect(
                        textX - 5, textY - textBounds.height() - 5,
                        textX + textBounds.width() + 5, textY + 5,
                        new Paint() {{ setColor(0x80000000); }} // Semi-transparent black
                    );
                    
                    canvas.drawText(text, textX, textY, textPaint);
                    
                    // Draw distance
                    String distanceText = String.format("%.0fm", distance);
                    textPaint.getTextBounds(distanceText, 0, distanceText.length(), textBounds);
                    float distanceX = x - textBounds.width() / 2;
                    float distanceY = textY + textBounds.height() + 10;
                    
                    canvas.drawRect(
                        distanceX - 5, distanceY - textBounds.height() - 5,
                        distanceX + textBounds.width() + 5, distanceY + 5,
                        new Paint() {{ setColor(0x80000000); }}
                    );
                    
                    canvas.drawText(distanceText, distanceX, distanceY, textPaint);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error drawing trigpoint icon for " + trig.getName(), e);
            }
        }
        
        @Override
        public boolean performClick() {
            // Handle click events on trigpoint icons
            // TODO: Implement hit testing to determine which trigpoint was clicked
            return super.performClick();
        }
    }
}
