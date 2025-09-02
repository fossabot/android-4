package uk.trigpointing.android.filter;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import uk.trigpointing.android.common.BaseActivity;

import uk.trigpointing.android.R;

public class FilterActivity extends BaseActivity {
    private static final String TAG = "FilterActivity";
    private SharedPreferences     mPrefs;
    private Spinner                mFilterType;
    private RadioGroup             mFilterRadio;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filter);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mFilterType     = findViewById(R.id.filterType);
        mFilterRadio    = findViewById(R.id.filterRadio);

        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    
    


    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        
        Editor editor = mPrefs.edit();
        
        // Get identifier for selected item from physical type list
        int filterType = mFilterType.getSelectedItemPosition();
        editor.putInt(Filter.FILTERTYPE, filterType);
        Log.i(TAG, "onPause: Saving filter type: " + filterType);
    
        // Get the correct radio button value based on the checked ID
        int filterRadio = 0; // Default to "All"
        int checkedId = mFilterRadio.getCheckedRadioButtonId();
        Log.i(TAG, "onPause: Checked radio button ID: " + checkedId);
        
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
        Log.i(TAG, "onPause: Saving filter radio: " + filterRadio);
        
        // Get text of selected radiobox item
        RadioButton btnSelected = findViewById(checkedId);
        if (btnSelected != null) {
            String radioText = btnSelected.getText().toString();
            editor.putString(Filter.FILTERRADIOTEXT, radioText);
            Log.i(TAG, "onPause: Saving filter radio text: '" + radioText + "'");
        }
    
        // Save to prefs
        editor.apply();
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        
        // Restore filter type
        int filterType = mPrefs.getInt(Filter.FILTERTYPE, 0);
        mFilterType.setSelection(filterType);
        Log.i(TAG, "onResume: Restored filter type: " + filterType);
        
        // Restore radio button selection based on saved value
        int filterRadio = mPrefs.getInt(Filter.FILTERRADIO, 0);
        Log.i(TAG, "onResume: Restored filter radio: " + filterRadio);
        
        int radioButtonId = R.id.filterAll; // Default to "All"
        switch (filterRadio) {
            case 0:
                radioButtonId = R.id.filterAll;
                break;
            case 1:
                radioButtonId = R.id.filterLogged;
                break;
            case 2:
                radioButtonId = R.id.filterNotLogged;
                break;
            case 3:
                radioButtonId = R.id.filterMarked;
                break;
            case 4:
                radioButtonId = R.id.filterUnsynced;
                break;
        }
        
        mFilterRadio.check(radioButtonId);
        Log.i(TAG, "onResume: Set radio button to ID: " + radioButtonId);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button in action bar
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
