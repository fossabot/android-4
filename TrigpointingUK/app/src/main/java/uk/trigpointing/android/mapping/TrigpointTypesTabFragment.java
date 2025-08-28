package uk.trigpointing.android.mapping;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import uk.trigpointing.android.R;
import uk.trigpointing.android.filter.Filter;

public class TrigpointTypesTabFragment extends Fragment {
    private static final String TAG = "TrigpointTypesTabFragment";

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
            int filterType = getFilterTypeFromRadioId(checkedId);
            saveFilterType(filterType);
            String jsType = convertFilterTypeToJsType(filterType);
            notifyLeafletMap(jsType);
        });
        
        return view;
    }

    private void loadCurrentSelection() {
        if (getActivity() == null) return;
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int currentFilterType = prefs.getInt(Filter.FILTERTYPE, 6); // Default to "All Types"
        
        int radioId = getRadioIdFromFilterType(currentFilterType);
        if (radioId != -1) {
            trigpointTypesRadioGroup.check(radioId);
        }
        Log.d(TAG, "Loaded filter type: " + currentFilterType);
    }

    /**
     * Convert radio button ID to Filter.FILTERTYPE integer value (0-6)
     */
    private int getFilterTypeFromRadioId(int radioId) {
        if (radioId == R.id.radioTypesPillarsOnly) return 0;        // TYPESPILLAR
        if (radioId == R.id.radioTypesPillarsFBM) return 1;         // TYPESPILLARFBM
        if (radioId == R.id.radioTypesFBMOnly) return 2;            // TYPESFBM
        if (radioId == R.id.radioTypesPassive) return 3;            // TYPESPASSIVE
        if (radioId == R.id.radioTypesIntersected) return 4;        // TYPESINTERSECTED
        if (radioId == R.id.radioTypesAllExceptIntersected) return 5; // TYPESNOINTERSECTED
        if (radioId == R.id.radioTypesAll) return 6;                // TYPESALL
        return 6; // Default to "All Types"
    }

    /**
     * Convert Filter.FILTERTYPE integer value (0-6) to radio button ID
     */
    private int getRadioIdFromFilterType(int filterType) {
        switch (filterType) {
            case 0: return R.id.radioTypesPillarsOnly;           // TYPESPILLAR
            case 1: return R.id.radioTypesPillarsFBM;            // TYPESPILLARFBM
            case 2: return R.id.radioTypesFBMOnly;               // TYPESFBM
            case 3: return R.id.radioTypesPassive;               // TYPESPASSIVE
            case 4: return R.id.radioTypesIntersected;           // TYPESINTERSECTED
            case 5: return R.id.radioTypesAllExceptIntersected;  // TYPESNOINTERSECTED
            case 6: return R.id.radioTypesAll;                   // TYPESALL
            default: return R.id.radioTypesAll;
        }
    }

    /**
     * Convert Filter.FILTERTYPE integer value to JavaScript string for Leaflet map
     */
    private String convertFilterTypeToJsType(int filterType) {
        switch (filterType) {
            case 0: return "pillars";      // TYPESPILLAR
            case 1: return "pillarsfbm";   // TYPESPILLARFBM - new value for pillars+fbm
            case 2: return "fbm";          // TYPESFBM
            case 3: return "passive";      // TYPESPASSIVE
            case 4: return "intersected";  // TYPESINTERSECTED
            case 5: return "nointersected"; // TYPESNOINTERSECTED - new value for all except intersected
            case 6: return "all";          // TYPESALL
            default: return "all";
        }
    }

    private void saveFilterType(int filterType) {
        if (getActivity() == null) return;
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.edit().putInt(Filter.FILTERTYPE, filterType).apply();
        Log.d(TAG, "Saved filter type: " + filterType);
    }

    private void notifyLeafletMap(String type) {
        if (getActivity() instanceof LeafletMapActivity) {
            ((LeafletMapActivity) getActivity()).updateTrigpointType(type);
        }
    }
}
