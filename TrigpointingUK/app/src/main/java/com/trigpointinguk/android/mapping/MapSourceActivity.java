package com.trigpointinguk.android.mapping;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RadioGroup;

import com.trigpointinguk.android.R;

public class MapSourceActivity extends AppCompatActivity {
    private static final String TAG = "MapSourceActivity";
    private SharedPreferences mPrefs;
    private RadioGroup mMapSourceRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_source_activity);

        		// Enable back button in action bar
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
            case ORDNANCE_SURVEY:
                mMapSourceRadioGroup.check(R.id.ordnance_survey);
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
            case USGS_SAT:
                mMapSourceRadioGroup.check(R.id.usgs_sat);
                break;
            case USGS_TOPO:
                mMapSourceRadioGroup.check(R.id.usgs_topo);
                break;
            case PUBLIC_TRANSPORT:
                mMapSourceRadioGroup.check(R.id.public_transport);
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
                mMapSourceRadioGroup.check(R.id.mapnik);
                break;
        }

        // Set up radio group listener
        mMapSourceRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            MapActivity.TileSource newSource;
            if (checkedId == R.id.mapnik) {
                newSource = MapActivity.TileSource.MAPNIK;
            } else if (checkedId == R.id.ordnance_survey) {
                newSource = MapActivity.TileSource.ORDNANCE_SURVEY;
            } else if (checkedId == R.id.usgs_topo) {
                newSource = MapActivity.TileSource.USGS_TOPO;
            } else if (checkedId == R.id.mapquest || checkedId == R.id.cloudmade || 
                       checkedId == R.id.cyclemap || checkedId == R.id.usgs_sat || 
                       checkedId == R.id.public_transport) {
                // These sources are disabled, don't allow selection
                Log.i(TAG, "Disabled map source selected, ignoring");
                return;
            } else if (checkedId == R.id.usgs_topo) {
                newSource = MapActivity.TileSource.USGS_TOPO;
            } else if (checkedId == R.id.bingaerial || checkedId == R.id.bingaeriallabels || 
                       checkedId == R.id.bingroad || checkedId == R.id.bingosgb) {
                // Bing options are disabled, don't allow selection
                Log.i(TAG, "Bing map source selected but disabled, ignoring");
                return;
            } else {
                newSource = MapActivity.TileSource.MAPNIK;
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