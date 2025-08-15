package uk.trigpointing.android.mapping;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import uk.trigpointing.android.R;

public class MapStyleTabFragment extends Fragment {

    private RadioGroup mapStyleRadioGroup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_map_style, container, false);
        
        mapStyleRadioGroup = view.findViewById(R.id.mapStyleRadioGroup);
        
        // Load current selection
        loadCurrentSelection();
        
        // Set up change listener
        mapStyleRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedStyle = getStyleFromRadioId(checkedId);
            saveMapStyle(selectedStyle);
            notifyLeafletMap(selectedStyle);
        });
        
        return view;
    }

    private void loadCurrentSelection() {
        if (getActivity() == null) return;
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String currentStyle = prefs.getString("leaflet_map_style", "OpenStreetMap");
        
        int radioId = getRadioIdFromStyle(currentStyle);
        if (radioId != -1) {
            mapStyleRadioGroup.check(radioId);
        }
    }

    private String getStyleFromRadioId(int radioId) {
        if (radioId == R.id.radioOSM) return "OpenStreetMap";
        if (radioId == R.id.radioOSOutdoor3857) return "OS Outdoor (3857)";
        if (radioId == R.id.radioOSOutdoor27700) return "OS Outdoor (27700)";
        if (radioId == R.id.radioOSLeisure27700) return "OS Leisure (27700)";
        return "OpenStreetMap";
    }

    private int getRadioIdFromStyle(String style) {
        switch (style) {
            case "OpenStreetMap": return R.id.radioOSM;
            case "OS Outdoor (3857)": return R.id.radioOSOutdoor3857;
            case "OS Outdoor (27700)": return R.id.radioOSOutdoor27700;
            case "OS Leisure (27700)": return R.id.radioOSLeisure27700;
            default: return R.id.radioOSM;
        }
    }

    private void saveMapStyle(String style) {
        if (getActivity() == null) return;
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.edit().putString("leaflet_map_style", style).apply();
    }

    private void notifyLeafletMap(String style) {
        if (getActivity() instanceof LeafletMapActivity) {
            ((LeafletMapActivity) getActivity()).updateMapStyle(style);
        }
    }
}
