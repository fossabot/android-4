package uk.trigpointing.android.radar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.Locale;

import uk.trigpointing.android.common.BaseActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import uk.trigpointing.android.DbHelper;
import uk.trigpointing.android.R;

public class RadarActivity extends BaseActivity implements SensorEventListener {

    private FusedLocationProviderClient fusedLocationClient;
    private SensorManager sensorManager;
    private Sensor rotationVector;

    private static final int REQ_LOCATION = 1001;

    private double targetLat;
    private double targetLon;

    private float currentAzimuthDeg = 0f;
    private Location lastLocation;

    private ImageView arrowView;
    private TextView distanceView;
    private TextView accuracyView;
    private TextView bearingView;
    private TextView calibrationView;

    private Vibrator vibrator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        targetLat = getIntent().getDoubleExtra(DbHelper.TRIG_LAT, 0);
        targetLon = getIntent().getDoubleExtra(DbHelper.TRIG_LON, 0);

        arrowView = findViewById(R.id.radar_arrow);
        distanceView = findViewById(R.id.radar_distance);
        accuracyView = findViewById(R.id.radar_accuracy);
        bearingView = findViewById(R.id.radar_bearing);
        calibrationView = findViewById(R.id.radar_calibration);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rotationVector != null) {
            sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_GAME);
        }
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQ_LOCATION);
            return;
        }
        LocationRequest req = new LocationRequest.Builder(1000L)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateIntervalMillis(500L)
                .build();
        fusedLocationClient.requestLocationUpdates(req, locationCallback, getMainLooper());
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
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null || locationResult.getLastLocation() == null) return;
            lastLocation = locationResult.getLastLocation();
            updateUi();
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
            // rotationMatrix is row-major: [0..2; 3..5; 6..8]
            float screenNormalX = -rotationMatrix[2];  // world X component of screen normal
            float screenNormalY = -rotationMatrix[5];  // world Y component of screen normal
            float screenNormalZ = -rotationMatrix[8];  // world Z component of screen normal
            
            // Phone vertical axis: device +Y axis in world coordinates
            float phoneVerticalX = rotationMatrix[1];  // world X component of phone vertical
            float phoneVerticalY = rotationMatrix[4];  // world Y component of phone vertical
            float phoneVerticalZ = rotationMatrix[7];  // world Z component of phone vertical
            
            // Calculate 45-degree hybrid vector between screen normal and phone vertical
            // Use equal weighting (cos(45°) ≈ 0.707) for both vectors
            float hybridX = 0.707f * screenNormalX + 0.707f * phoneVerticalX;
            float hybridY = 0.707f * screenNormalY + 0.707f * phoneVerticalY;
            
            // Calculate heading from hybrid vector (ignore Z component for compass bearing)
            float headingRad = (float) Math.atan2(hybridX, hybridY);
            float headingDeg = (float) Math.toDegrees(headingRad);
            if (headingDeg < 0) headingDeg += 360f;
            
            // low-pass filter for smoother arrow
            // low-pass smoothing with proper angle difference handling
            float ALPHA = 0.15f;
            float angleDiff = normalizeDegrees(headingDeg - currentAzimuthDeg);
            currentAzimuthDeg = currentAzimuthDeg + ALPHA * angleDiff;
            // Ensure currentAzimuthDeg stays within 0-360° range
            while (currentAzimuthDeg < 0) currentAzimuthDeg += 360f;
            while (currentAzimuthDeg >= 360) currentAzimuthDeg -= 360f;
            updateUi();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
                calibrationView.setVisibility(View.VISIBLE);
            } else {
                calibrationView.setVisibility(View.GONE);
            }
        }
    }

    private void updateUi() {
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

        arrowView.setRotation(delta);
        distanceView.setText(formatDistance(distance));

        float acc = Math.max(lastLocation.getAccuracy(), 3f);
        accuracyView.setText(String.format(Locale.getDefault(), "±%.0f m", acc));

        // Calculate magnetic bearing (true bearing minus declination)
        float magneticBearing = bearingTo - field.getDeclination();
        // Normalize to 0-360 degrees
        while (magneticBearing < 0) magneticBearing += 360f;
        while (magneticBearing >= 360) magneticBearing -= 360f;
        bearingView.setText(String.format(Locale.getDefault(), "Bearing: %03.0f°", magneticBearing));

        // Haptic feedback when aligned and close
        if (distance < 20 && Math.abs(delta) < 5) {
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }

    private String formatDistance(float meters) {
        if (meters >= 1000f) {
            return String.format(Locale.getDefault(), "%.2f km", meters / 1000f);
        } else if (meters >= 100f) {
            return String.format(Locale.getDefault(), "%.0f m", meters);
        } else {
            return String.format(Locale.getDefault(), "%.1f m", meters);
        }
    }

    private float normalizeDegrees(float deg) {
        while (deg < -180f) deg += 360f;
        while (deg > 180f) deg -= 360f;
        return deg;
    }
}


