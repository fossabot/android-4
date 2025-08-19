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

public class FilterFoundTabFragment extends Fragment {

    private RadioGroup filterFoundRadioGroup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_filter_found, container, false);
        
        filterFoundRadioGroup = view.findViewById(R.id.filterFoundRadioGroup);
        
        // Load current selection
        loadCurrentSelection();
        
        // Set up change listener
        filterFoundRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedFound = getFoundFromRadioId(checkedId);
            saveFound(selectedFound);
            notifyLeafletMap(selectedFound);
        });
        
        return view;
    }

    private void loadCurrentSelection() {
        if (getActivity() == null) return;
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String currentFound = prefs.getString("leaflet_filter_found", "all");
        
        int radioId = getRadioIdFromFound(currentFound);
        if (radioId != -1) {
            filterFoundRadioGroup.check(radioId);
        }
    }

    private String getFoundFromRadioId(int radioId) {
        if (radioId == R.id.radioFoundAll) return "all";
        if (radioId == R.id.radioFoundLogged) return "logged";
        if (radioId == R.id.radioFoundNotLogged) return "notlogged";
        if (radioId == R.id.radioFoundMarked) return "marked";
        if (radioId == R.id.radioFoundUnsynced) return "unsynced";
        return "all";
    }

    private int getRadioIdFromFound(String found) {
        switch (found) {
            case "all": return R.id.radioFoundAll;
            case "logged": return R.id.radioFoundLogged;
            case "notlogged": return R.id.radioFoundNotLogged;
            case "marked": return R.id.radioFoundMarked;
            case "unsynced": return R.id.radioFoundUnsynced;
            default: return R.id.radioFoundAll;
        }
    }

    private void saveFound(String found) {
        if (getActivity() == null) return;
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("leaflet_filter_found", found);
        
        // Also update the Filter.FILTERRADIO values for synchronization with nearest page
        int filterRadio = convertToFilterRadioValue(found);
        editor.putInt("filterRadio", filterRadio);
        
        // Update filter text for display purposes
        String filterText = getFilterTextFromFound(found);
        editor.putString("filterRadioText", filterText);
        
        editor.apply();
    }

    private void notifyLeafletMap(String found) {
        if (getActivity() instanceof LeafletMapActivity) {
            ((LeafletMapActivity) getActivity()).updateFilterFound(found);
        }
    }
    
    /**
     * Convert leaflet filter values to FilterFoundActivity radio values for synchronization
     */
    private int convertToFilterRadioValue(String found) {
        switch (found) {
            case "all": return 0;      // filterAll
            case "logged": return 1;   // filterLogged
            case "notlogged": return 2; // filterNotLogged
            case "marked": return 3;   // filterMarked
            case "unsynced": return 4; // filterUnsynced
            default: return 0;
        }
    }
    
    /**
     * Get display text for filter option
     */
    private String getFilterTextFromFound(String found) {
        switch (found) {
            case "all": return "All";
            case "logged": return "Logged";
            case "notlogged": return "Not Logged";
            case "marked": return "Marked";
            case "unsynced": return "Unsynced";
            default: return "All";
        }
    }
}
