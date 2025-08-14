package com.trigpointinguk.android.radar;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.trigpointinguk.android.DbHelper;
import com.trigpointinguk.android.R;

public class RadarActivity extends AppCompatActivity implements SensorEventListener {

    private FusedLocationProviderClient fusedLocationClient;
    private SensorManager sensorManager;
    private Sensor rotationVector;

    private double targetLat;
    private double targetLon;

    private float currentAzimuthDeg = 0f;
    private Location lastLocation;

    private ImageView arrowView;
    private TextView distanceView;
    private TextView accuracyView;
    private TextView calibrationView;

    private Vibrator vibrator;

    private final float ALPHA = 0.15f; // low-pass smoothing

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
            Toast.makeText(this, "Location permission required", Toast.LENGTH_LONG).show();
            return;
        }
        LocationRequest req = new LocationRequest.Builder(1000L)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateIntervalMillis(500L)
                .build();
        fusedLocationClient.requestLocationUpdates(req, locationCallback, getMainLooper());
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
            float[] orientation = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientation);
            float azimuthRad = orientation[0];
            float azimuthDeg = (float) Math.toDegrees(azimuthRad);
            // low-pass filter for smoother arrow
            currentAzimuthDeg = currentAzimuthDeg + ALPHA * (azimuthDeg - currentAzimuthDeg);
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
        accuracyView.setText(String.format("±%.0f m", acc));

        // Haptic feedback when aligned and close
        if (distance < 20 && Math.abs(delta) < 5) {
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }

    private String formatDistance(float meters) {
        if (meters >= 1000f) {
            return String.format("%.2f km", meters / 1000f);
        } else if (meters >= 100f) {
            return String.format("%.0f m", meters);
        } else {
            return String.format("%.1f m", meters);
        }
    }

    private float normalizeDegrees(float deg) {
        while (deg < -180f) deg += 360f;
        while (deg > 180f) deg -= 360f;
        return deg;
    }
}


