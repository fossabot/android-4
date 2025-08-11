package com.trigpointinguk.android.mapping;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.ArcGISMap;
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
        ArcGISMap map = new ArcGISMap();
        // Set OpenStreetMap as default basemap (no Esri watermark)
        map.setBasemap(new Basemap(new OpenStreetMapLayer()));
        mapView3857.setMap(map);

        // Center roughly on GB
        mapView3857.setViewpointCenterAsync(new Point(-2.0, 54.0, SpatialReferences.getWgs84()), 1_000_000);

        // No provider switch yet; added in options menu
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.arcgis_map_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        if (id == R.id.source_osm) {
            mapView3857.getMap().setBasemap(new Basemap(new OpenStreetMapLayer()));
            return true;
        }
        if (id == R.id.source_os_outdoor) {
            String osKey = PreferenceManager.getDefaultSharedPreferences(this).getString("os_api_key", "");
            if (osKey == null || osKey.trim().isEmpty()) {
                Toast.makeText(this, "Set Ordnance Survey API key in Preferences", Toast.LENGTH_LONG).show();
                return true;
            }
            try {
                String templateUrl = "https://api.os.uk/maps/raster/v1/zxy/Outdoor_3857/{level}/{col}/{row}.png?key=" + osKey;
                WebTiledLayer osOutdoor = new WebTiledLayer(templateUrl);
                mapView3857.getMap().setBasemap(new Basemap(osOutdoor));
            } catch (Exception e) {
                Log.e(TAG, "Error creating OS Outdoor layer", e);
            }
            return true;
        }
        if (id == R.id.source_os_leisure) {
            Toast.makeText(this, "Leisure (27700) requires a 27700 map; not yet implemented here.", Toast.LENGTH_LONG).show();
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


