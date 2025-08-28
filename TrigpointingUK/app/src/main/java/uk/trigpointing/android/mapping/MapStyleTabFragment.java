package uk.trigpointing.android.mapping;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import uk.trigpointing.android.R;

public class MapStyleTabFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private RadioGroup mapStyleRadioGroup;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_map_style, container, false);
        
        mapStyleRadioGroup = view.findViewById(R.id.mapStyleRadioGroup);
        
        // Initialize preferences
        if (getActivity() != null) {
            prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        }
        
        // Load current selection
        loadCurrentSelection();
        
        // Set up change listener - only update map, don't save preferences
        mapStyleRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedStyle = getStyleFromRadioId(checkedId);
            // Don't save to preferences - this is a temporary map change
            // Only notify the map to update visually
            notifyLeafletMap(selectedStyle);
        });
        
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register preference change listener
        if (prefs != null) {
            prefs.registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister preference change listener
        if (prefs != null) {
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("MapStyleTabFragment", "Preference changed: " + key);
        if ("leaflet_map_style".equals(key)) {
            String currentStyle = sharedPreferences.getString(key, "OpenStreetMap");
            Log.d("MapStyleTabFragment", "Map style preference changed to: " + currentStyle);
            
            // Reload the current selection when the preference changes from elsewhere
            loadCurrentSelection();
            // Also notify the map to update
            notifyLeafletMap(currentStyle);
        }
    }

    private void loadCurrentSelection() {
        if (getActivity() == null) return;
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String currentStyle = prefs.getString("leaflet_map_style", "OpenStreetMap");
        
        Log.d("MapStyleTabFragment", "Loading current selection: " + currentStyle);
        
        int radioId = getRadioIdFromStyle(currentStyle);
        if (radioId != -1) {
            mapStyleRadioGroup.check(radioId);
            Log.d("MapStyleTabFragment", "Set radio button for style: " + currentStyle);
        } else {
            Log.w("MapStyleTabFragment", "No radio button found for style: " + currentStyle);
        }
    }

    private String getStyleFromRadioId(int radioId) {
        if (radioId == R.id.radioOSM) return "OpenStreetMap";
        if (radioId == R.id.radioOSOutdoor3857) return "ESRI.WorldImagery"; // This button represents Satellite
        if (radioId == R.id.radioOSOutdoor27700) return "OS Outdoor (3857)"; // OS Digital uses 3857 projection
        if (radioId == R.id.radioOSLeisure27700) return "OS Leisure (27700)"; // OS Paper uses 27700 projection
        return "OpenStreetMap";
    }

    private int getRadioIdFromStyle(String style) {
        switch (style) {
            case "OpenStreetMap": return R.id.radioOSM;
            case "ESRI.WorldImagery": return R.id.radioOSOutdoor3857; // Satellite maps to this button
            case "OS Outdoor (3857)": return R.id.radioOSOutdoor27700; // OS Digital maps to this button
            case "OS Leisure (27700)": return R.id.radioOSLeisure27700;
            // Handle legacy values
            case "OS Outdoor (27700)": return R.id.radioOSOutdoor27700; // Support old incorrect value
            default: return R.id.radioOSM;
        }
    }



    private void notifyLeafletMap(String style) {
        Log.d("MapStyleTabFragment", "Notifying map to update style to: " + style);
        if (getActivity() instanceof LeafletMapActivity) {
            LeafletMapActivity mapActivity = (LeafletMapActivity) getActivity();
            // Update the map to use the new style
            mapActivity.updateMapStyle(style);
            // Update session storage so the new preference becomes the session default
            mapActivity.updateSessionMapStyle(style);
            
            // Mark that the next map load should use preferences (in case user leaves and returns)
            // This ensures preference changes always take effect immediately
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            prefs.edit().putBoolean("is_first_map_load", true).apply();
            Log.d("MapStyleTabFragment", "Reset first map load flag due to preference change");
        } else {
            Log.w("MapStyleTabFragment", "Activity is not LeafletMapActivity, cannot notify map");
        }
    }
}
