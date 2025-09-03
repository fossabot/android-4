package uk.trigpointing.android.compass.skins;

import android.graphics.Matrix;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

import uk.trigpointing.android.R;
import uk.trigpointing.android.compass.CompassData;
import uk.trigpointing.android.compass.CompassDataManager;
import uk.trigpointing.android.compass.CompassSkinFragment;

/**
 * Compass Rose skin with north-pointing rose, hand pointer, and trigpoint icon on circle
 */
public class CompassRoseSkinFragment extends CompassSkinFragment {
    
    private ImageView compassRose;
    private ImageView handPointer;
    private ImageView trigpointIcon;
    private TextView distanceView;
    private TextView accuracyView;
    private TextView bearingView;
    private TextView calibrationView;
    
    // Configuration constants - easy to find and tweak as requested
    private static final float HAND_ROTATION_OFFSET_DEGREES = 65f; // Hand finger pointing offset
    private static final float HAND_PIVOT_X_RATIO = 0.5f; // Rotation axis X (from bottom left)
    private static final float HAND_PIVOT_Y_RATIO = 0.5f; // Rotation axis Y (from bottom left)
    private static final float TRIGPOINT_CIRCLE_RADIUS_DP = 180f; // Circle radius for trigpoint icon
    private static final float HAND_VERTICAL_ADJUSTMENT_DP = 35f; // Extra downward tweak for hand pivot alignment
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_compass_rose_skin, container, false);
        
        compassRose = view.findViewById(R.id.compass_rose);
        handPointer = view.findViewById(R.id.hand_pointer);
        trigpointIcon = view.findViewById(R.id.trigpoint_icon);
        distanceView = view.findViewById(R.id.compass_distance);
        accuracyView = view.findViewById(R.id.compass_accuracy);
        bearingView = view.findViewById(R.id.compass_bearing);
        calibrationView = view.findViewById(R.id.compass_calibration);
        
        return view;
    }
    
    @Override
    public void updateCompassData(CompassData data) {
        if (!isAdded() || getView() == null) return;
        
        // 1. Rotate compass rose to always point North
        // North is opposite to current device azimuth
        float northRotation = -data.getCurrentAzimuthDegrees();
        compassRose.setRotation(northRotation);
        
        // 2. Rotate hand pointer to point towards trigpoint
        // The hand ImageView is positioned at "just below centre" by the layout
        // We just need to set the pivot point within the hand image - no translation needed
        float handRotation = data.getRotationDelta() - HAND_ROTATION_OFFSET_DEGREES;
        
        // Set the pivot point within the hand image to the center of the ImageView
        // This way it rotates around the "just below centre" screen position
        float handCenterOffsetY = 50 * getResources().getDisplayMetrics().density; // Convert 50dp to pixels
        float handExtraOffsetY = HAND_VERTICAL_ADJUSTMENT_DP * getResources().getDisplayMetrics().density; // Additional tweak
        if (handPointer.getWidth() > 0 && handPointer.getHeight() > 0) {
            // For now, let's rotate around the center of the ImageView to match the trigpoint circle
            // The ImageView is positioned at "just below centre" so its center should match the trigpoint circle center
            float pivotX = handPointer.getWidth() * 0.5f;  // Center of ImageView
            float pivotY = handPointer.getHeight() * 0.5f; // Center of ImageView
            
            handPointer.setPivotX(pivotX);
            handPointer.setPivotY(pivotY);
            
            // Calculate offset to position the hand within the ImageView so the desired point 
            // (defined by your ratios) aligns with the ImageView center (the rotation point)
            float desiredX = handPointer.getWidth() * HAND_PIVOT_X_RATIO;
            float desiredY = handPointer.getHeight() * (1.0f - HAND_PIVOT_Y_RATIO);
            
            float offsetX = pivotX - desiredX;  // Move hand so desired point aligns with center
            float offsetY = pivotY - desiredY;
            
            handPointer.setTranslationX(offsetX);
            handPointer.setTranslationY(offsetY + handCenterOffsetY + handExtraOffsetY);
        }
        handPointer.setRotation(handRotation);
        
        // 3. Position trigpoint icon on circle around the same "just below centre" point as the hand
        float circleRadiusPx = TRIGPOINT_CIRCLE_RADIUS_DP * getResources().getDisplayMetrics().density;
        float trigpointAngleRad = (float) Math.toRadians(data.getRotationDelta());
        
        // Calculate position on circle
        float trigpointX = circleRadiusPx * (float) Math.sin(trigpointAngleRad);
        float trigpointY = -circleRadiusPx * (float) Math.cos(trigpointAngleRad); // Negative because Y increases downward
        
        // Both the hand and trigpoint should rotate around the same "just below centre" point
        // Offset the trigpoint by the same 50dp to match the hand's rotation center
        
        trigpointIcon.setTranslationX(trigpointX);
        trigpointIcon.setTranslationY(trigpointY + handCenterOffsetY);
        
        // 4. Update text displays
        distanceView.setText(CompassDataManager.formatDistance(data.getDistance()));
        accuracyView.setText(String.format(Locale.getDefault(), "±%.0f m", data.getAccuracy()));
        bearingView.setText(String.format(Locale.getDefault(), "Bearing: %03.0f°", data.getMagneticBearing()));
        
        // 5. Update calibration warning
        if (data.isCalibrationRequired()) {
            calibrationView.setVisibility(View.VISIBLE);
        } else {
            calibrationView.setVisibility(View.GONE);
        }
    }
    
    @Override
    public String getSkinName() {
        return "Compass Rose";
    }
}
