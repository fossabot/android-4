package com.trigpointinguk.android.mapping;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.trigpointinguk.android.R;
import com.trigpointinguk.android.mapping.MapIcon.colourScheme;

public class ColourByActivity extends AppCompatActivity {
    private static final String TAG = "ColourByActivity";
    private SharedPreferences mPrefs;
    private RadioGroup mColourRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.colour_by_activity);

        		// Enable back button in action bar
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mColourRadioGroup = findViewById(R.id.colourRadioGroup);

        // Load current selection
        String currentColouring = mPrefs.getString("iconColouring", "NONE");
        colourScheme currentScheme = colourScheme.valueOf(currentColouring);

        // Set the appropriate radio button
        switch (currentScheme) {
            case BYCONDITION:
                mColourRadioGroup.check(R.id.byCondition);
                break;
            case BYLOGGED:
                mColourRadioGroup.check(R.id.byLogged);
                break;
            case NONE:
            default:
                mColourRadioGroup.check(R.id.none);
                break;
        }

        // Set up radio group listener
        mColourRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            colourScheme newScheme;
            if (checkedId == R.id.byCondition) {
                newScheme = colourScheme.BYCONDITION;
            } else if (checkedId == R.id.byLogged) {
                newScheme = colourScheme.BYLOGGED;
            } else {
                newScheme = colourScheme.NONE;
            }

            // Save the selection
            Editor editor = mPrefs.edit();
            editor.putString("iconColouring", newScheme.toString());
            editor.apply();

            Log.i(TAG, "Colour scheme changed to: " + newScheme.toString());

            // Return result to MapActivity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("colourScheme", newScheme.toString());
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button in action bar
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 