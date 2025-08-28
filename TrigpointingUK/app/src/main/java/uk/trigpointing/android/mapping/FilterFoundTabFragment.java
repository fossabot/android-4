package uk.trigpointing.android.mapping;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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

public class FilterFoundTabFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "FilterFoundTabFragment";

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
            int filterRadio = getFilterRadioFromRadioId(checkedId);
            saveFilterRadio(filterRadio);
            String jsValue = convertFilterRadioToJsValue(filterRadio);
            notifyLeafletMap(jsValue);
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
        
        int currentFilterRadio = prefs.getInt(Filter.FILTERRADIO, 0); // Default to "Logged or not"
        Log.d(TAG, "loadCurrentSelection: currentFilterRadio=" + currentFilterRadio);
        
        int radioId = getRadioIdFromFilterRadio(currentFilterRadio);
        Log.d(TAG, "loadCurrentSelection: radioId=" + radioId);
        if (radioId != -1) {
            // Temporarily disable listener to avoid triggering change events during programmatic update
            filterFoundRadioGroup.setOnCheckedChangeListener(null);
            filterFoundRadioGroup.check(radioId);
            Log.d(TAG, "loadCurrentSelection: checked radio " + radioId);
            // Re-enable listener
            filterFoundRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                int filterRadio = getFilterRadioFromRadioId(checkedId);
                saveFilterRadio(filterRadio);
                String jsValue = convertFilterRadioToJsValue(filterRadio);
                notifyLeafletMap(jsValue);
            });
        }
    }

    /**
     * Convert radio button ID to Filter.FILTERRADIO integer value (0-4)
     */
    private int getFilterRadioFromRadioId(int radioId) {
        if (radioId == R.id.radioFoundAll) return 0;        // Logged or not
        if (radioId == R.id.radioFoundLogged) return 1;     // Logged
        if (radioId == R.id.radioFoundNotLogged) return 2;  // Not Logged
        if (radioId == R.id.radioFoundMarked) return 3;     // Marked
        if (radioId == R.id.radioFoundUnsynced) return 4;   // Unsynced
        return 0; // Default to "Logged or not"
    }

    /**
     * Convert Filter.FILTERRADIO integer value (0-4) to radio button ID
     */
    private int getRadioIdFromFilterRadio(int filterRadio) {
        switch (filterRadio) {
            case 0: return R.id.radioFoundAll;         // Logged or not
            case 1: return R.id.radioFoundLogged;      // Logged
            case 2: return R.id.radioFoundNotLogged;   // Not Logged
            case 3: return R.id.radioFoundMarked;      // Marked
            case 4: return R.id.radioFoundUnsynced;    // Unsynced
            default: return R.id.radioFoundAll;
        }
    }

    /**
     * Convert Filter.FILTERRADIO integer value to JavaScript string for Leaflet map
     */
    private String convertFilterRadioToJsValue(int filterRadio) {
        switch (filterRadio) {
            case 0: return "all";       // Logged or not
            case 1: return "logged";    // Logged
            case 2: return "notlogged"; // Not Logged
            case 3: return "marked";    // Marked
            case 4: return "unsynced";  // Unsynced
            default: return "all";
        }
    }

    private void saveFilterRadio(int filterRadio) {
        if (getActivity() == null || prefs == null) return;
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(Filter.FILTERRADIO, filterRadio);
        
        // Also update leaflet_filter_found for JavaScript synchronization
        String jsValue = convertFilterRadioToJsValue(filterRadio);
        editor.putString("leaflet_filter_found", jsValue);
        
        // Update filter text for display purposes
        String filterText = getFilterTextFromRadio(filterRadio);
        editor.putString(Filter.FILTERRADIOTEXT, filterText);
        
        editor.apply();
        Log.d(TAG, "Saved filter radio: " + filterRadio + " (" + filterText + "), js value: " + jsValue);
    }

    private void notifyLeafletMap(String jsValue) {
        if (getActivity() instanceof LeafletMapActivity) {
            ((LeafletMapActivity) getActivity()).updateFilterFound(jsValue);
        }
    }
    
    /**
     * Get display text for filter option
     */
    private String getFilterTextFromRadio(int filterRadio) {
        switch (filterRadio) {
            case 0: return "Logged or not";
            case 1: return "Logged";
            case 2: return "Not Logged";
            case 3: return "Marked";
            case 4: return "Unsynced";
            default: return "Logged or not";
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
