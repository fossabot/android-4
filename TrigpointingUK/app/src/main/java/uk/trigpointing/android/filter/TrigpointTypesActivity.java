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
    private RadioButton[] mRadioButtons;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trigpoint_types);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mFilterTypeRadioGroup = findViewById(R.id.filterTypeRadioGroup);  

        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize radio buttons
        setupRadioButtons();

        // Load current selection
        loadCurrentSelection();
    }

    private void setupRadioButtons() {
        // Initialize array of radio buttons
        mRadioButtons = new RadioButton[] {
            findViewById(R.id.radio_pillars_only),
            findViewById(R.id.radio_pillars_fbm),
            findViewById(R.id.radio_fbm_only),
            findViewById(R.id.radio_passive),
            findViewById(R.id.radio_intersected),
            findViewById(R.id.radio_all_except_intersected),
            findViewById(R.id.radio_all_types)
        };
        
        Log.i(TAG, "setupRadioButtons: Initialized " + mRadioButtons.length + " radio buttons");
    }

    private void loadCurrentSelection() {
        int currentType = mPrefs.getInt(Filter.FILTERTYPE, 0);
        
        // Ensure currentType is within bounds
        if (currentType >= 0 && currentType < mRadioButtons.length) {
            mRadioButtons[currentType].setChecked(true);
        } else {
            // Default to "All Types" if invalid selection
            mRadioButtons[6].setChecked(true);
        }
        
        Log.i(TAG, "Loaded filter type: " + currentType);
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        
        Editor editor = mPrefs.edit();
        
        // Find which radio button is selected
        int filterType = 0; // default
        for (int i = 0; i < mRadioButtons.length; i++) {
            if (mRadioButtons[i].isChecked()) {
                filterType = i;
                break;
            }
        }
        
        editor.putInt(Filter.FILTERTYPE, filterType);
        Log.i(TAG, "onPause: Saving filter type: " + filterType);
    
        // Save to prefs
        editor.apply();
        
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
