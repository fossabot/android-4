package com.trigpointinguk.android.mapping;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.layers.OpenStreetMapLayer;
import com.esri.arcgisruntime.layers.WebTiledLayer;
import com.trigpointinguk.android.R;

public class ArcGisMapActivity extends AppCompatActivity {
    private static final String TAG = "ArcGisMapActivity";
    private MapView mapView3857;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.arcgis_map);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mapView3857 = findViewById(R.id.arcgisMapView);

        // Default to OSM Mapnik
        ArcGISMap map = new ArcGISMap(BasemapStyle.ARCGIS_IMAGERY_STANDARD);
        // Replace basemap with OpenStreetMap
        map.setBasemap(new Basemap(new OpenStreetMapLayer()));
        mapView3857.setMap(map);

        // Center roughly on GB
        mapView3857.setViewpointCenterAsync(new Point(-2.0, 54.0, SpatialReferences.getWgs84()), 1_000_000);

        // If OS key present, add OS Outdoor as a WebTiledLayer option (user can navigate to it via menu later)
        String osKey = PreferenceManager.getDefaultSharedPreferences(this).getString("os_api_key", "");
        if (osKey == null || osKey.trim().isEmpty()) {
            Toast.makeText(this, "OS API key not set. Only OSM is available.", Toast.LENGTH_SHORT).show();
        } else {
            try {
                String templateUrl = "https://api.os.uk/maps/raster/v1/zxy/Outdoor_3857/{level}/{col}/{row}.png?key=" + osKey;
                WebTiledLayer osOutdoor = new WebTiledLayer(templateUrl);
                // Swap basemap to OS Outdoor immediately for demo
                map.setBasemap(new Basemap(osOutdoor));
            } catch (Exception e) {
                Log.e(TAG, "Error creating OS Outdoor layer", e);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        if (mapView3857 != null) mapView3857.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView3857 != null) mapView3857.resume();
    }

    @Override
    protected void onDestroy() {
        if (mapView3857 != null) mapView3857.dispose();
        super.onDestroy();
    }
}


