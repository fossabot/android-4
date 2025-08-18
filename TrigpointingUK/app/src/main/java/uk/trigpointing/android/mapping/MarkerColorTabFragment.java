package uk.trigpointing.android.mapping;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import uk.trigpointing.android.R;

public class MarkerColorTabFragment extends Fragment {

    private RadioGroup markerColorRadioGroup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_marker_color, container, false);
        
        markerColorRadioGroup = view.findViewById(R.id.markerColorRadioGroup);
        
        // Load current selection
        loadCurrentSelection();
        
        // Set up change listener
        markerColorRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedColor = getColorFromRadioId(checkedId);
            saveMarkerColor(selectedColor);
            notifyLeafletMap(selectedColor);
        });
        
        return view;
    }

    private void loadCurrentSelection() {
        if (getActivity() == null) return;
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String currentColor = prefs.getString("leaflet_marker_color", "none");
        
        int radioId = getRadioIdFromColor(currentColor);
        if (radioId != -1) {
            markerColorRadioGroup.check(radioId);
        }
    }

    private String getColorFromRadioId(int radioId) {
        if (radioId == R.id.radioByCondition) return "condition";
        if (radioId == R.id.radioByLogged) return "logged";
        if (radioId == R.id.radioNone) return "none";
        return "none";
    }

    private int getRadioIdFromColor(String color) {
        switch (color) {
            case "condition": return R.id.radioByCondition;
            case "logged": return R.id.radioByLogged;
            case "none": return R.id.radioNone;
            default: return R.id.radioNone;
        }
    }

    private void saveMarkerColor(String color) {
        if (getActivity() == null) return;
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.edit().putString("leaflet_marker_color", color).apply();
    }

    private void notifyLeafletMap(String color) {
        if (getActivity() instanceof LeafletMapActivity) {
            ((LeafletMapActivity) getActivity()).updateMarkerColor(color);
        }
    }
}
