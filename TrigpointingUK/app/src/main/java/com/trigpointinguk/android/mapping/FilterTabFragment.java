package com.trigpointinguk.android.mapping;

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

import com.trigpointinguk.android.R;

public class FilterTabFragment extends Fragment {

    private RadioGroup filterRadioGroup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_filter, container, false);
        
        filterRadioGroup = view.findViewById(R.id.filterRadioGroup);
        
        // Load current selection
        loadCurrentSelection();
        
        // Set up change listener
        filterRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedFilter = getFilterFromRadioId(checkedId);
            saveFilter(selectedFilter);
            notifyLeafletMap(selectedFilter);
        });
        
        return view;
    }

    private void loadCurrentSelection() {
        if (getActivity() == null) return;
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String currentFilter = prefs.getString("leaflet_filter", "all");
        
        int radioId = getRadioIdFromFilter(currentFilter);
        if (radioId != -1) {
            filterRadioGroup.check(radioId);
        }
    }

    private String getFilterFromRadioId(int radioId) {
        if (radioId == R.id.radioFilterAll) return "all";
        if (radioId == R.id.radioFilterPillars) return "pillars";
        if (radioId == R.id.radioFilterFBM) return "fbm";
        if (radioId == R.id.radioFilterPassive) return "passive";
        if (radioId == R.id.radioFilterIntersected) return "intersected";
        return "all";
    }

    private int getRadioIdFromFilter(String filter) {
        switch (filter) {
            case "all": return R.id.radioFilterAll;
            case "pillars": return R.id.radioFilterPillars;
            case "fbm": return R.id.radioFilterFBM;
            case "passive": return R.id.radioFilterPassive;
            case "intersected": return R.id.radioFilterIntersected;
            default: return R.id.radioFilterAll;
        }
    }

    private void saveFilter(String filter) {
        if (getActivity() == null) return;
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.edit().putString("leaflet_filter", filter).apply();
    }

    private void notifyLeafletMap(String filter) {
        if (getActivity() instanceof LeafletMapActivity) {
            ((LeafletMapActivity) getActivity()).updateFilter(filter);
        }
    }
}
