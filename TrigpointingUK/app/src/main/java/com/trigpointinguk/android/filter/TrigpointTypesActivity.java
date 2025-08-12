package com.trigpointinguk.android.filter;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;

import com.trigpointinguk.android.R;

public class TrigpointTypesActivity extends AppCompatActivity {
    private static final String TAG = "TrigpointTypesActivity";
    private SharedPreferences mPrefs;
    private Spinner mFilterType;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trigpoint_types);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mFilterType = (Spinner) findViewById(R.id.filterType);  

        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Populate spinner with trigpoint type options
        setupSpinner();

        // Load current selection
        loadCurrentSelection();
    }

    private void setupSpinner() {
        // Get trigpoint type names from resources
        String[] trigpointTypeNames = getResources().getStringArray(R.array.trigpoint_type_names);
        
        // Create adapter and set it to spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, trigpointTypeNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mFilterType.setAdapter(adapter);
        
        Log.i(TAG, "setupSpinner: Populated spinner with " + trigpointTypeNames.length + " items");
    }

    private void loadCurrentSelection() {
        int currentType = mPrefs.getInt(Filter.FILTERTYPE, 0);
        mFilterType.setSelection(currentType);
        Log.i(TAG, "Loaded filter type: " + currentType);
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        
        Editor editor = mPrefs.edit();
        
        // Get identifier for selected item from physical type list
        int filterType = mFilterType.getSelectedItemPosition();
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
