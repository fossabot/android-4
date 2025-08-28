package uk.trigpointing.android.filter;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import uk.trigpointing.android.common.BaseActivity;

import uk.trigpointing.android.R;

public class TrigpointTypesActivity extends BaseActivity {
    private static final String TAG = "TrigpointTypesActivity";
    private SharedPreferences mPrefs;
    private RadioGroup mFilterTypeRadioGroup;
    private RadioGroup mFilterStatusRadioGroup;
    private RadioButton[] mTypeRadioButtons;
    private RadioButton[] mStatusRadioButtons;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trigpoint_types);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mFilterTypeRadioGroup = findViewById(R.id.filterTypeRadioGroup);
        mFilterStatusRadioGroup = findViewById(R.id.filterStatusRadioGroup);  

        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize radio buttons
        setupRadioButtons();

        // Load current selection
        loadCurrentSelection();
        
        // Set up automatic close on selection
        setupAutoClose();
    }

    private void setupRadioButtons() {
        // Initialize array of type radio buttons
        mTypeRadioButtons = new RadioButton[] {
            findViewById(R.id.radio_pillars_only),
            findViewById(R.id.radio_pillars_fbm),
            findViewById(R.id.radio_fbm_only),
            findViewById(R.id.radio_passive),
            findViewById(R.id.radio_intersected),
            findViewById(R.id.radio_all_except_intersected),
            findViewById(R.id.radio_all_types)
        };
        
        // Initialize array of status radio buttons
        mStatusRadioButtons = new RadioButton[] {
            findViewById(R.id.radio_status_all),
            findViewById(R.id.radio_status_logged),
            findViewById(R.id.radio_status_not_logged),
            findViewById(R.id.radio_status_marked),
            findViewById(R.id.radio_status_unsynced)
        };
        
        Log.i(TAG, "setupRadioButtons: Initialized " + mTypeRadioButtons.length + " type radio buttons and " + mStatusRadioButtons.length + " status radio buttons");
    }

    private void loadCurrentSelection() {
        // Load trigpoint type selection
        int currentType = mPrefs.getInt(Filter.FILTERTYPE, 6); // Default to "All Types"
        
        // Ensure currentType is within bounds
        if (currentType >= 0 && currentType < mTypeRadioButtons.length) {
            mTypeRadioButtons[currentType].setChecked(true);
        } else {
            // Default to "All Types" if invalid selection
            mTypeRadioButtons[6].setChecked(true);
        }
        
        // Load logging status selection
        int currentStatus = mPrefs.getInt(Filter.FILTERRADIO, 0);
        
        // Ensure currentStatus is within bounds (0=All, 1=Logged, 2=Not Logged, 3=Marked, 4=Unsynced)
        if (currentStatus >= 0 && currentStatus < mStatusRadioButtons.length) {
            mStatusRadioButtons[currentStatus].setChecked(true);
        } else {
            // Default to "All" if invalid selection
            mStatusRadioButtons[0].setChecked(true);
        }
        
        Log.i(TAG, "Loaded filter type: " + currentType + ", filter status: " + currentStatus);
    }
    
    private void setupAutoClose() {
        // Set up listeners for both radio groups
        mFilterTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Log.i(TAG, "Trigpoint type selection changed, saving and closing");
            
            // Save the selection immediately
            saveCurrentSelection();
            
            // Close the activity
            finish();
        });
        
        mFilterStatusRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Log.i(TAG, "Filter status selection changed, saving and closing");
            
            // Save the selection immediately
            saveCurrentSelection();
            
            // Close the activity
            finish();
        });
    }
    
    private void saveCurrentSelection() {
        Editor editor = mPrefs.edit();
        
        // Find which trigpoint type radio button is selected
        int filterType = 0; // default
        for (int i = 0; i < mTypeRadioButtons.length; i++) {
            if (mTypeRadioButtons[i].isChecked()) {
                filterType = i;
                break;
            }
        }
        
        // Find which status radio button is selected
        int filterStatus = 0; // default (All)
        for (int i = 0; i < mStatusRadioButtons.length; i++) {
            if (mStatusRadioButtons[i].isChecked()) {
                filterStatus = i;
                break;
            }
        }
        
        editor.putInt(Filter.FILTERTYPE, filterType);
        editor.putInt(Filter.FILTERRADIO, filterStatus);
        
        // Update the text values for display
        String typeText = mTypeRadioButtons[filterType].getText().toString();
        String statusText = mStatusRadioButtons[filterStatus].getText().toString();
        editor.putString(Filter.FILTERRADIOTEXT, statusText); // Keep for compatibility
        
        // Update leaflet_filter_found for map screen synchronization
        String leafletFilterValue = convertToLeafletFilterValue(filterStatus);
        editor.putString("leaflet_filter_found", leafletFilterValue);
        
        Log.i(TAG, "saveCurrentSelection: Saving filter type: " + filterType + " (" + typeText + "), filter status: " + filterStatus + " (" + statusText + "), leaflet_filter_found: " + leafletFilterValue);
    
        // Save to prefs
        editor.apply();
    }
    
    /**
     * Convert the filter status values to LeafletMap filter values for synchronization
     */
    private String convertToLeafletFilterValue(int filterStatus) {
        switch (filterStatus) {
            case 0: return "all";       // Logged or not
            case 1: return "logged";    // Logged  
            case 2: return "notlogged"; // Not Logged
            case 3: return "marked";    // Marked
            case 4: return "unsynced";  // Unsynced
            default: return "all";
        }
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        
        // Save current selection (fallback in case auto-close didn't trigger)
        saveCurrentSelection();
        
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
