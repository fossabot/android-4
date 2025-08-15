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

public class TrigpointTypesTabFragment extends Fragment {

    private RadioGroup trigpointTypesRadioGroup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_trigpoint_types, container, false);
        
        trigpointTypesRadioGroup = view.findViewById(R.id.trigpointTypesRadioGroup);
        
        // Load current selection
        loadCurrentSelection();
        
        // Set up change listener
        trigpointTypesRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedType = getTypeFromRadioId(checkedId);
            saveType(selectedType);
            notifyLeafletMap(selectedType);
        });
        
        return view;
    }

    private void loadCurrentSelection() {
        if (getActivity() == null) return;
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String currentType = prefs.getString("leaflet_trigpoint_type", "all");
        
        int radioId = getRadioIdFromType(currentType);
        if (radioId != -1) {
            trigpointTypesRadioGroup.check(radioId);
        }
    }

    private String getTypeFromRadioId(int radioId) {
        if (radioId == R.id.radioTypesAll) return "all";
        if (radioId == R.id.radioTypesPillars) return "pillars";
        if (radioId == R.id.radioTypesFBM) return "fbm";
        if (radioId == R.id.radioTypesPassive) return "passive";
        if (radioId == R.id.radioTypesIntersected) return "intersected";
        return "all";
    }

    private int getRadioIdFromType(String type) {
        switch (type) {
            case "all": return R.id.radioTypesAll;
            case "pillars": return R.id.radioTypesPillars;
            case "fbm": return R.id.radioTypesFBM;
            case "passive": return R.id.radioTypesPassive;
            case "intersected": return R.id.radioTypesIntersected;
            default: return R.id.radioTypesAll;
        }
    }

    private void saveType(String type) {
        if (getActivity() == null) return;
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.edit().putString("leaflet_trigpoint_type", type).apply();
    }

    private void notifyLeafletMap(String type) {
        if (getActivity() instanceof LeafletMapActivity) {
            ((LeafletMapActivity) getActivity()).updateTrigpointType(type);
        }
    }
}
