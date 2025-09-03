package uk.trigpointing.android.compass;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.Locale;

/**
 * Manages compass data calculations and sensor handling.
 * Extracted from RadarActivity to be shared across different compass visualizations.
 */
public class CompassDataManager implements SensorEventListener {
    
    public interface CompassDataListener {
        void onCompassDataUpdated(CompassData data);
        void onLocationPermissionRequired();
        void onLocationPermissionDenied();
    }
    
    private final Context context;
    private final CompassDataListener listener;
    
    private FusedLocationProviderClient fusedLocationClient;
    private SensorManager sensorManager;
    private Sensor rotationVector;
    private Vibrator vibrator;
    
    private double targetLat;
    private double targetLon;
    private float currentAzimuthDeg = 0f;
    private Location lastLocation;
    private boolean isCalibrationRequired = false;
    
    private static final int REQ_LOCATION = 1001;
    
    public CompassDataManager(Context context, CompassDataListener listener) {
        this.context = context;
        this.listener = listener;
        
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }
    
    public void setTarget(double lat, double lon) {
        this.targetLat = lat;
        this.targetLon = lon;
    }
    
    public void startUpdates() {
        if (rotationVector != null) {
            sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_GAME);
        }
        startLocationUpdates();
    }
    
    public void stopUpdates() {
        sensorManager.unregisterListener(this);
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
    
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            listener.onLocationPermissionRequired();
            return;
        }
        
        LocationRequest req = new LocationRequest.Builder(1000L)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateIntervalMillis(500L)
                .build();
        fusedLocationClient.requestLocationUpdates(req, locationCallback, null);
    }
    
    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null || locationResult.getLastLocation() == null) return;
            lastLocation = locationResult.getLastLocation();
            updateCompassData();
        }
    };
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

            // Calculate hybrid direction: 45 degrees between screen normal and phone vertical axis
            // This works well for both horizontal and vertical phone orientations
            
            // Screen normal (camera direction): device -Z axis in world coordinates
            float screenNormalX = -rotationMatrix[2];  // world X component of screen normal
            float screenNormalY = -rotationMatrix[5];  // world Y component of screen normal
            
            // Phone vertical axis: device +Y axis in world coordinates
            float phoneVerticalX = rotationMatrix[1];  // world X component of phone vertical
            float phoneVerticalY = rotationMatrix[4];  // world Y component of phone vertical
            
            // Calculate 45-degree hybrid vector between screen normal and phone vertical
            float hybridX = 0.707f * screenNormalX + 0.707f * phoneVerticalX;
            float hybridY = 0.707f * screenNormalY + 0.707f * phoneVerticalY;
            
            // Calculate heading from hybrid vector (ignore Z component for compass bearing)
            float headingRad = (float) Math.atan2(hybridX, hybridY);
            float headingDeg = (float) Math.toDegrees(headingRad);
            if (headingDeg < 0) headingDeg += 360f;
            
            // Low-pass filter for smoother compass
            float ALPHA = 0.15f;
            float angleDiff = normalizeDegrees(headingDeg - currentAzimuthDeg);
            currentAzimuthDeg = currentAzimuthDeg + ALPHA * angleDiff;
            
            // Ensure currentAzimuthDeg stays within 0-360° range
            while (currentAzimuthDeg < 0) currentAzimuthDeg += 360f;
            while (currentAzimuthDeg >= 360) currentAzimuthDeg -= 360f;
            
            updateCompassData();
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            isCalibrationRequired = (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE);
        }
    }
    
    private void updateCompassData() {
        if (lastLocation == null) return;

        Location target = new Location("target");
        target.setLatitude(targetLat);
        target.setLongitude(targetLon);

        float distance = lastLocation.distanceTo(target); // meters
        float bearingTo = lastLocation.bearingTo(target); // degrees

        // Adjust device azimuth from magnetic to true north using geomagnetic declination
        GeomagneticField field = new GeomagneticField(
                (float) lastLocation.getLatitude(),
                (float) lastLocation.getLongitude(),
                (float) lastLocation.getAltitude(),
                System.currentTimeMillis());
        float trueAzimuth = currentAzimuthDeg + field.getDeclination();

        // Compute the delta the arrow should rotate: positive = rotate clockwise
        float delta = normalizeDegrees(bearingTo - trueAzimuth);

        float accuracy = Math.max(lastLocation.getAccuracy(), 3f);

        // Calculate magnetic bearing (true bearing minus declination)
        float magneticBearing = bearingTo - field.getDeclination();
        // Normalize to 0-360 degrees
        while (magneticBearing < 0) magneticBearing += 360f;
        while (magneticBearing >= 360) magneticBearing -= 360f;

        // Haptic feedback when aligned and close
        if (distance < 20 && Math.abs(delta) < 5) {
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
        
        CompassData data = new CompassData(
            lastLocation, target, currentAzimuthDeg, bearingTo, 
            distance, accuracy, magneticBearing, delta, isCalibrationRequired
        );
        
        listener.onCompassDataUpdated(data);
    }
    
    private float normalizeDegrees(float deg) {
        while (deg < -180f) deg += 360f;
        while (deg > 180f) deg -= 360f;
        return deg;
    }
    
    public static String formatDistance(float meters) {
        if (meters >= 1000f) {
            return String.format(Locale.getDefault(), "%.2f km", meters / 1000f);
        } else if (meters >= 100f) {
            return String.format(Locale.getDefault(), "%.0f m", meters);
        } else {
            return String.format(Locale.getDefault(), "%.1f m", meters);
        }
    }
}
