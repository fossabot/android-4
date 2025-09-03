package uk.trigpointing.android.compass.skins;

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
 * Grey pointer compass skin - similar to the original radar view but with grey styling
 */
public class GreyPointerCompassFragment extends CompassSkinFragment {
    
    private ImageView compassRose;
    private ImageView arrowView;
    private TextView distanceView;
    private TextView accuracyView;
    private TextView bearingView;
    private TextView calibrationView;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_grey_pointer_compass, container, false);
        
        compassRose = view.findViewById(R.id.compass_rose);
        arrowView = view.findViewById(R.id.compass_arrow);
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
        
        // 2. Update arrow rotation to point towards trigpoint
        arrowView.setRotation(data.getRotationDelta());
        
        // 3. Update distance
        distanceView.setText(CompassDataManager.formatDistance(data.getDistance()));
        
        // 4. Update accuracy
        accuracyView.setText(String.format(Locale.getDefault(), "±%.0f m", data.getAccuracy()));
        
        // 5. Update bearing
        bearingView.setText(String.format(Locale.getDefault(), "Bearing: %03.0f°", data.getMagneticBearing()));
        
        // 6. Update calibration warning
        if (data.isCalibrationRequired()) {
            calibrationView.setVisibility(View.VISIBLE);
        } else {
            calibrationView.setVisibility(View.GONE);
        }
    }
    
    @Override
    public String getSkinName() {
        return "Basic";
    }
}
