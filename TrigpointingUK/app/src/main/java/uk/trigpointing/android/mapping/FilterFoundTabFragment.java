package uk.trigpointing.android.mapping;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import uk.trigpointing.android.R;

public class FilterFoundTabFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private RadioGroup filterFoundRadioGroup;
    private SharedPreferences prefs;
    private BroadcastReceiver filterChangeReceiver;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_filter_found, container, false);
        
        filterFoundRadioGroup = view.findViewById(R.id.filterFoundRadioGroup);
        
        // Initialize preferences
        if (getActivity() != null) {
            prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        }
        
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

    @Override
    public void onResume() {
        super.onResume();
        // Register preference change listener
        if (prefs != null) {
            prefs.registerOnSharedPreferenceChangeListener(this);
        }
        
        // Set up broadcast receiver for filter changes
        filterChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String filterValue = intent.getStringExtra("filter_value");
                android.util.Log.d("FilterFoundTabFragment", "Received broadcast: filter_value=" + filterValue);
                loadCurrentSelection();
            }
        };
        
        if (getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                filterChangeReceiver, 
                new IntentFilter("FILTER_FOUND_CHANGED")
            );
        }
        
        // Reload filter state when returning to this fragment (e.g., after changing filter on nearest page)
        loadCurrentSelection();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister preference change listener
        if (prefs != null) {
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }
        
        // Unregister broadcast receiver
        if (filterChangeReceiver != null && getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(filterChangeReceiver);
        }
    }

    private void loadCurrentSelection() {
        if (getActivity() == null || filterFoundRadioGroup == null || prefs == null) return;
        
        String currentFound = prefs.getString("leaflet_filter_found", "all");
        android.util.Log.d("FilterFoundTabFragment", "loadCurrentSelection: currentFound=" + currentFound);
        
        int radioId = getRadioIdFromFound(currentFound);
        android.util.Log.d("FilterFoundTabFragment", "loadCurrentSelection: radioId=" + radioId);
        if (radioId != -1) {
            // Temporarily disable listener to avoid triggering change events during programmatic update
            filterFoundRadioGroup.setOnCheckedChangeListener(null);
            filterFoundRadioGroup.check(radioId);
            android.util.Log.d("FilterFoundTabFragment", "loadCurrentSelection: checked radio " + radioId);
            // Re-enable listener
            filterFoundRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                String selectedFound = getFoundFromRadioId(checkedId);
                saveFound(selectedFound);
                notifyLeafletMap(selectedFound);
            });
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
        if (getActivity() == null || prefs == null) return;
        
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
            case "all": return "Logged or not";
            case "logged": return "Logged";
            case "notlogged": return "Not Logged";
            case "marked": return "Marked";
            case "unsynced": return "Unsynced";
            default:         return "Logged or not";
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        android.util.Log.d("FilterFoundTabFragment", "onSharedPreferenceChanged: key=" + key);
        // Listen for changes to the leaflet_filter_found preference
        if ("leaflet_filter_found".equals(key)) {
            android.util.Log.d("FilterFoundTabFragment", "Detected leaflet_filter_found change, reloading selection");
            // Reload the filter selection when preferences change
            loadCurrentSelection();
        }
    }
}
