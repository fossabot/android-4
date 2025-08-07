package com.trigpointinguk.android.mapping;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RadioGroup;

import com.trigpointinguk.android.R;

public class MapSourceActivity extends Activity {
    private static final String TAG = "MapSourceActivity";
    private SharedPreferences mPrefs;
    private RadioGroup mMapSourceRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_source_activity);

        // Enable back button in action bar
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mMapSourceRadioGroup = findViewById(R.id.mapSourceRadioGroup);

        // Load current selection
        String currentTileSource = mPrefs.getString("tileSource", "NONE");
        MapActivity.TileSource currentSource = MapActivity.TileSource.valueOf(currentTileSource);

        // Set the appropriate radio button
        switch (currentSource) {
            case MAPNIK:
                mMapSourceRadioGroup.check(R.id.mapnik);
                break;
            case MAPQUEST:
                mMapSourceRadioGroup.check(R.id.mapquest);
                break;
            case CLOUDMADE:
                mMapSourceRadioGroup.check(R.id.cloudmade);
                break;
            case CYCLEMAP:
                mMapSourceRadioGroup.check(R.id.cyclemap);
                break;
            case BING_AERIAL:
                mMapSourceRadioGroup.check(R.id.bingaerial);
                break;
            case BING_AERIAL_LABELS:
                mMapSourceRadioGroup.check(R.id.bingaeriallabels);
                break;
            case BING_ROAD:
                mMapSourceRadioGroup.check(R.id.bingroad);
                break;
            case BING_OSGB:
            case NONE:
            default:
                mMapSourceRadioGroup.check(R.id.bingosgb);
                break;
        }

        // Set up radio group listener
        mMapSourceRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            MapActivity.TileSource newSource;
            if (checkedId == R.id.mapnik) {
                newSource = MapActivity.TileSource.MAPNIK;
            } else if (checkedId == R.id.mapquest) {
                newSource = MapActivity.TileSource.MAPQUEST;
            } else if (checkedId == R.id.cloudmade) {
                newSource = MapActivity.TileSource.CLOUDMADE;
            } else if (checkedId == R.id.cyclemap) {
                newSource = MapActivity.TileSource.CYCLEMAP;
            } else if (checkedId == R.id.bingaerial) {
                newSource = MapActivity.TileSource.BING_AERIAL;
            } else if (checkedId == R.id.bingaeriallabels) {
                newSource = MapActivity.TileSource.BING_AERIAL_LABELS;
            } else if (checkedId == R.id.bingroad) {
                newSource = MapActivity.TileSource.BING_ROAD;
            } else {
                newSource = MapActivity.TileSource.BING_OSGB;
            }

            // Save the selection
            Editor editor = mPrefs.edit();
            editor.putString("tileSource", newSource.toString());
            editor.apply();

            Log.i(TAG, "Map source changed to: " + newSource.toString());

            // Return result to MapActivity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("tileSource", newSource.toString());
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