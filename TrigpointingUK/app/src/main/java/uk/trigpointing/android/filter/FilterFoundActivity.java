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

public class FilterFoundActivity extends BaseActivity {
    private static final String TAG = "FilterFoundActivity";
    private SharedPreferences mPrefs;
    private RadioGroup mFilterRadio;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filter_found);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mFilterRadio = findViewById(R.id.filterRadio);

        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Load current selection
        loadCurrentSelection();
    }

    private void loadCurrentSelection() {
        int currentRadio = mPrefs.getInt(Filter.FILTERRADIO, 0);
        
        // Map the stored value to the correct radio button
        int radioId = R.id.filterAll; // Default
        switch (currentRadio) {
            case 0: radioId = R.id.filterAll; break;
            case 1: radioId = R.id.filterLogged; break;
            case 2: radioId = R.id.filterNotLogged; break;
            case 3: radioId = R.id.filterMarked; break;
            case 4: radioId = R.id.filterUnsynced; break;
        }
        
        mFilterRadio.check(radioId);
        Log.i(TAG, "Loaded filter radio: " + currentRadio + " -> " + radioId);
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        saveFilterState();
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop");
        // Save state in onStop as well to ensure synchronization
        saveFilterState();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        // Save state in onDestroy as final backup
        saveFilterState();
        super.onDestroy();
    }

    private void saveFilterState() {
        if (mPrefs == null || mFilterRadio == null) return;
        
        Editor editor = mPrefs.edit();
        
        // Get the correct radio button value based on the checked ID
        int filterRadio = 0; // Default to "All"
        int checkedId = mFilterRadio.getCheckedRadioButtonId();
        Log.i(TAG, "saveFilterState: Checked radio button ID: " + checkedId);
        
        if (checkedId == R.id.filterAll) {
            filterRadio = 0;
        } else if (checkedId == R.id.filterLogged) {
            filterRadio = 1;
        } else if (checkedId == R.id.filterNotLogged) {
            filterRadio = 2;
        } else if (checkedId == R.id.filterMarked) {
            filterRadio = 3;
        } else if (checkedId == R.id.filterUnsynced) {
            filterRadio = 4;
        }
        
        editor.putInt(Filter.FILTERRADIO, filterRadio);
        Log.i(TAG, "saveFilterState: Saving filter radio: " + filterRadio);
        
        // Get text of selected radiobox item
        RadioButton btnSelected = findViewById(checkedId);
        if (btnSelected != null) {
            String radioText = btnSelected.getText().toString();
            editor.putString(Filter.FILTERRADIOTEXT, radioText);
            Log.i(TAG, "saveFilterState: Saving filter radio text: '" + radioText + "'");
        }
        
        // Also update the leaflet map filter preference for synchronization
        String leafletFilterValue = convertToLeafletFilterValue(filterRadio);
        editor.putString("leaflet_filter_found", leafletFilterValue);
        Log.i(TAG, "saveFilterState: Syncing with leaflet filter: " + leafletFilterValue);
    
        // Save to prefs
        editor.apply();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Convert the FilterFoundActivity radio values to LeafletMap filter values for synchronization
     */
    private String convertToLeafletFilterValue(int filterRadio) {
        switch (filterRadio) {
            case 0: return "all";      // filterAll
            case 1: return "logged";   // filterLogged  
            case 2: return "notlogged"; // filterNotLogged
            case 3: return "marked";   // filterMarked
            case 4: return "unsynced"; // filterUnsynced
            default: return "all";
        }
    }
}
