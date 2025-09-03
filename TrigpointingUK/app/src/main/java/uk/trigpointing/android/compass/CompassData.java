package uk.trigpointing.android.compass;

import android.location.Location;

/**
 * Data structure containing all compass-related information for visualization
 */
public class CompassData {
    private final Location currentLocation;
    private final Location targetLocation;
    private final float currentAzimuthDegrees;
    private final float bearingToTarget;
    private final float distance;
    private final float accuracy;
    private final float magneticBearing;
    private final float rotationDelta;
    private final boolean isCalibrationRequired;
    
    public CompassData(Location currentLocation, Location targetLocation, 
                      float currentAzimuthDegrees, float bearingToTarget, 
                      float distance, float accuracy, float magneticBearing, 
                      float rotationDelta, boolean isCalibrationRequired) {
        this.currentLocation = currentLocation;
        this.targetLocation = targetLocation;
        this.currentAzimuthDegrees = currentAzimuthDegrees;
        this.bearingToTarget = bearingToTarget;
        this.distance = distance;
        this.accuracy = accuracy;
        this.magneticBearing = magneticBearing;
        this.rotationDelta = rotationDelta;
        this.isCalibrationRequired = isCalibrationRequired;
    }
    
    public Location getCurrentLocation() { return currentLocation; }
    public Location getTargetLocation() { return targetLocation; }
    public float getCurrentAzimuthDegrees() { return currentAzimuthDegrees; }
    public float getBearingToTarget() { return bearingToTarget; }
    public float getDistance() { return distance; }
    public float getAccuracy() { return accuracy; }
    public float getMagneticBearing() { return magneticBearing; }
    public float getRotationDelta() { return rotationDelta; }
    public boolean isCalibrationRequired() { return isCalibrationRequired; }
}
