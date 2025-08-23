package uk.trigpointing.android.ar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import uk.trigpointing.android.common.BaseActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import uk.trigpointing.android.DbHelper;
import uk.trigpointing.android.R;
import uk.trigpointing.android.types.Trig;
import uk.trigpointing.android.types.Condition;

public class ARTrigpointActivity extends BaseActivity implements LocationListener {
    
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
        
        public String getName() { return name; }
        public double getLat() { return lat; }
        public double getLon() { return lon; }
    }
    private static final String TAG = "ARTrigpointActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 1001;
    private static final int LOCATION_PERMISSION_REQUEST = 1002;
    private static final double MAX_DISTANCE_METERS = 5000; // 5km max distance
    private static final int MAX_TRIGPOINTS = 10; // Show max 10 trigpoints
    
    private ArSceneView arSceneView;
    private Session arSession;
    private TextView tvInstructions;
    private TextView tvTrigpointCount;
    private Button btnClose;
    
    private DbHelper dbHelper;
    private LocationManager locationManager;
    private Location currentLocation;
    private List<TrigpointData> nearbyTrigpoints = new ArrayList<>();
    private List<Node> trigpointNodes = new ArrayList<>();
    
    private boolean installRequested = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_trigpoint);
        
        Log.i(TAG, "onCreate: Starting AR Trigpoint Activity");
        Log.i(TAG, "onCreate: Process ID = " + android.os.Process.myPid());
        Log.i(TAG, "onCreate: Thread ID = " + Thread.currentThread().getId());
        
        // Initialize UI components
        arSceneView = findViewById(R.id.arSceneView);
        tvInstructions = findViewById(R.id.tvInstructions);
        tvTrigpointCount = findViewById(R.id.tvTrigpointCount);
        btnClose = findViewById(R.id.btnClose);
        
        // Initialize database helper
        try {
            dbHelper = new DbHelper(this);
            Log.i(TAG, "Database helper initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize database helper", e);
        }
        
        // Set up close button
        btnClose.setOnClickListener(v -> finish());
        
        // Check permissions
        Log.i(TAG, "onCreate: Checking permissions...");
        if (!hasPermissions()) {
            Log.w(TAG, "onCreate: Permissions not granted, requesting...");
            requestPermissions();
        } else {
            Log.i(TAG, "onCreate: Permissions OK, initializing AR...");
            initializeAR();
        }
    }
    
    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA);
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), CAMERA_PERMISSION_REQUEST);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                initializeAR();
            } else {
                Toast.makeText(this, "Camera and location permissions are required for AR view", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    
    private void initializeAR() {
        Log.i(TAG, "initializeAR: Initializing ARCore");
        
        // Check if ARCore is supported and installed
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this);
        if (availability.isTransient()) {
            // Continue to query availability at 5Hz while compatibility is checked in the background.
            new android.os.Handler().postDelayed(this::initializeAR, 200);
            return;
        }
        
        if (availability != ArCoreApk.Availability.SUPPORTED_INSTALLED) {
            switch (availability) {
                case SUPPORTED_APK_TOO_OLD:
                case SUPPORTED_NOT_INSTALLED:
                    try {
                        // Request ARCore installation or update if needed.
                        ArCoreApk.InstallStatus installStatus = ArCoreApk.getInstance().requestInstall(this, !installRequested);
                        switch (installStatus) {
                            case INSTALL_REQUESTED:
                                Log.i(TAG, "ARCore installation requested.");
                                installRequested = true;
                                return;
                            case INSTALLED:
                                break;
                        }
                    } catch (UnavailableUserDeclinedInstallationException e) {
                        Toast.makeText(this, "ARCore is required for this feature", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    } catch (Exception e) {
                        Log.e(TAG, "ARCore installation failed", e);
                        Toast.makeText(this, "ARCore installation failed", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    break;
                case UNSUPPORTED_DEVICE_NOT_CAPABLE:
                    Toast.makeText(this, "This device does not support AR", Toast.LENGTH_LONG).show();
                    finish();
                    return;
            }
        }
        
        // Create ARCore session
        try {
            arSession = new Session(this);
            
            // Configure session
            Config config = new Config(arSession);
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            arSession.configure(config);
            
            // Set up the scene view
            arSceneView.setupSession(arSession);
            
            Log.i(TAG, "ARCore session created successfully");
            
            // Start location services
            startLocationServices();
            
            // Add a test fallback after 5 seconds if no location is received
            new android.os.Handler().postDelayed(() -> {
                if (currentLocation == null) {
                    Log.w(TAG, "No location received after 5 seconds, trying fallback");
                    runOnUiThread(() -> {
                        tvTrigpointCount.setText("No GPS location available. Check location permissions and GPS settings.");
                    });
                }
            }, 5000);
            
        } catch (UnavailableArcoreNotInstalledException e) {
            Toast.makeText(this, "Please install ARCore", Toast.LENGTH_LONG).show();
            finish();
        } catch (UnavailableApkTooOldException e) {
            Toast.makeText(this, "Please update ARCore", Toast.LENGTH_LONG).show();
            finish();
        } catch (UnavailableSdkTooOldException e) {
            Toast.makeText(this, "Please update this app", Toast.LENGTH_LONG).show();
            finish();
        } catch (UnavailableDeviceNotCompatibleException e) {
            Toast.makeText(this, "This device does not support AR", Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Failed to create AR session", e);
            Toast.makeText(this, "Failed to start AR session", Toast.LENGTH_LONG).show();
            finish();
        }
    }
    
    private void startLocationServices() {
        Log.i(TAG, "startLocationServices: Starting location services");
        
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "startLocationServices: Location permission not granted!");
            return;
        }
        
        Log.i(TAG, "startLocationServices: Location permission granted, setting up location updates...");
        
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this);
            
            // Try to get last known location
            Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnown == null) {
                lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            
            if (lastKnown != null) {
                Log.i(TAG, "Using last known location: " + lastKnown.getLatitude() + ", " + lastKnown.getLongitude());
                onLocationChanged(lastKnown);
            } else {
                Log.w(TAG, "No last known location available");
                // Try to load trigpoints anyway with a default location for testing
                runOnUiThread(() -> {
                    tvTrigpointCount.setText("Waiting for GPS location...");
                });
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start location services", e);
        }
    }
    
    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.i(TAG, "onLocationChanged: New location received - " + location.getLatitude() + ", " + location.getLongitude() + " accuracy: " + location.getAccuracy());
        currentLocation = location;
        loadNearbyTrigpoints();
    }
    
    private void loadNearbyTrigpoints() {
        if (currentLocation == null) {
            Log.w(TAG, "loadNearbyTrigpoints: No current location available");
            return;
        }
        
        if (dbHelper == null) {
            Log.e(TAG, "loadNearbyTrigpoints: Database helper is null");
            return;
        }
        
        Log.i(TAG, "loadNearbyTrigpoints: Loading trigpoints near " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude());
        
        try {
            // Query database for nearby trigpoints
            Cursor cursor = dbHelper.fetchTrigList(currentLocation);
            Log.i(TAG, "loadNearbyTrigpoints: Database query completed, cursor: " + cursor);
            
            nearbyTrigpoints.clear();
            int count = 0;
            
            if (cursor != null && cursor.moveToFirst()) {
                Log.i(TAG, "loadNearbyTrigpoints: Cursor has data, processing trigpoints...");
                do {
                    if (count >= MAX_TRIGPOINTS) break;
                    
                    long id = cursor.getLong(cursor.getColumnIndex(DbHelper.TRIG_ID));
                    String name = cursor.getString(cursor.getColumnIndex(DbHelper.TRIG_NAME));
                    double lat = cursor.getDouble(cursor.getColumnIndex(DbHelper.TRIG_LAT));
                    double lon = cursor.getDouble(cursor.getColumnIndex(DbHelper.TRIG_LON));
                    String type = cursor.getString(cursor.getColumnIndex(DbHelper.TRIG_TYPE));
                    String condition = cursor.getString(cursor.getColumnIndex(DbHelper.TRIG_CONDITION));
                    
                    Log.d(TAG, "Processing trigpoint: " + name + " at " + lat + ", " + lon);
                    
                    // Calculate distance
                    Location trigLocation = new Location("trigpoint");
                    trigLocation.setLatitude(lat);
                    trigLocation.setLongitude(lon);
                    float distance = currentLocation.distanceTo(trigLocation);
                    
                    // Only include trigpoints within max distance
                    if (distance <= MAX_DISTANCE_METERS) {
                        TrigpointData trigData = new TrigpointData(id, name, lat, lon, type, condition);
                        nearbyTrigpoints.add(trigData);
                        count++;
                    }
                    
                } while (cursor.moveToNext());
                
                cursor.close();
            }
            
            Log.i(TAG, "Found " + nearbyTrigpoints.size() + " nearby trigpoints");
            
            // Update UI
            runOnUiThread(() -> {
                String message = "Found " + nearbyTrigpoints.size() + " trigpoints within 5km";
                Log.i(TAG, "Updating UI: " + message);
                tvTrigpointCount.setText(message);
                createTrigpointMarkers();
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading nearby trigpoints", e);
        }
    }
    
    private void createTrigpointMarkers() {
        if (currentLocation == null) {
            Log.w(TAG, "createTrigpointMarkers: No current location available");
            return;
        }
        
        if (nearbyTrigpoints.isEmpty()) {
            Log.w(TAG, "createTrigpointMarkers: No nearby trigpoints to display");
            return;
        }
        
        Log.i(TAG, "createTrigpointMarkers: Creating AR markers for " + nearbyTrigpoints.size() + " trigpoints");
        
        // Clear existing markers
        for (Node node : trigpointNodes) {
            arSceneView.getScene().removeChild(node);
        }
        trigpointNodes.clear();
        
        // Create new markers
        for (TrigpointData trig : nearbyTrigpoints) {
            createTrigpointMarker(trig);
        }
    }
    
    private void createTrigpointMarker(TrigpointData trig) {
        // Calculate bearing and distance from current location to trigpoint
        Location trigLocation = new Location("trigpoint");
        trigLocation.setLatitude(trig.getLat());
        trigLocation.setLongitude(trig.getLon());
        
        float bearing = currentLocation.bearingTo(trigLocation);
        float distance = currentLocation.distanceTo(trigLocation);
        
        // Convert bearing to radians
        double bearingRad = Math.toRadians(bearing);
        
        // Calculate position in AR space (simplified - place markers in a circle around user)
        // In a real implementation, you'd use ARCore's coordinate system
        float x = (float) (Math.sin(bearingRad) * Math.min(distance / 100.0f, 10.0f)); // Scale distance
        float z = (float) (-Math.cos(bearingRad) * Math.min(distance / 100.0f, 10.0f)); // Negative Z is forward
        float y = 0.5f; // Height above ground
        
        // Create a simple text marker
        ViewRenderable.builder()
            .setView(this, R.layout.ar_trigpoint_marker)
            .build()
            .thenAccept(renderable -> {
                Node markerNode = new Node();
                markerNode.setRenderable(renderable);
                markerNode.setLocalPosition(new Vector3(x, y, z));
                
                // Set up the marker view with trigpoint data
                View markerView = renderable.getView();
                TextView nameText = markerView.findViewById(R.id.trigpoint_name);
                TextView distanceText = markerView.findViewById(R.id.trigpoint_distance);
                
                if (nameText != null) {
                    nameText.setText(trig.getName());
                }
                if (distanceText != null) {
                    distanceText.setText(String.format("%.0fm", distance));
                }
                
                arSceneView.getScene().addChild(markerNode);
                trigpointNodes.add(markerNode);
                
                Log.d(TAG, "Created marker for " + trig.getName() + " at distance " + distance + "m");
            })
            .exceptionally(throwable -> {
                Log.e(TAG, "Failed to create marker for " + trig.getName(), throwable);
                return null;
            });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        if (arSceneView != null) {
            try {
                arSceneView.resume();
            } catch (CameraNotAvailableException e) {
                Log.e(TAG, "Camera not available", e);
                finish();
            }
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        if (arSceneView != null) {
            arSceneView.pause();
        }
        
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (arSession != null) {
            arSession.close();
        }
        
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
    
    // LocationListener methods
    @Override
    public void onProviderEnabled(@NonNull String provider) {}
    
    @Override
    public void onProviderDisabled(@NonNull String provider) {}
    
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
}
