package com.trigpointinguk.android.mapping;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.layers.OpenStreetMapLayer;
import com.esri.arcgisruntime.layers.WebTiledLayer;
import com.esri.arcgisruntime.io.RequestConfiguration;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.arcgisservices.TileInfo;
import com.esri.arcgisruntime.arcgisservices.LevelOfDetail;
import com.trigpointinguk.android.R;

public class ArcGisMapActivity extends AppCompatActivity {
    private static final String TAG = "ArcGisMapActivity";
    private MapView mapView3857;
    private ArcGISMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.arcgis_map);
        // ArcGIS Runtime logging APIs vary by version; skipping SDK-level log routing to avoid
        // compatibility issues. We log layer load statuses and draw status below instead.

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mapView3857 = findViewById(R.id.arcgisMapView);

        // Default to OSM Mapnik via WebTiledLayer with subdomains
        map = new ArcGISMap();
        mapView3857.setMap(map);
        setBasemapOSM();
        mapView3857.setViewpointAsync(new Viewpoint(54.0, -2.0, 1_500_000));

        mapView3857.addDrawStatusChangedListener(status -> {
            Log.i(TAG, "Draw status=" + status.getDrawStatus());
        });
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
            setBasemapOSM();
            return true;
        }
        if (id == R.id.source_os_outdoor) {
            setBasemapOSOutdoor();
            return true;
        }
        if (id == R.id.source_os_leisure) {
            Toast.makeText(this, "Leisure (27700) requires a 27700 map; not yet implemented here.", Toast.LENGTH_LONG).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setBasemapOSM() {
        String template = "https://{subDomain}.tile.openstreetmap.org/{level}/{col}/{row}.png";
        java.util.List<String> subs = java.util.Arrays.asList("a", "b", "c");
        WebTiledLayer osm = new WebTiledLayer(template, subs);
        RequestConfiguration rc = new RequestConfiguration();
        rc.getHeaders().put("User-Agent", "TrigpointingUK/1.0");
        osm.setRequestConfiguration(rc);
        osm.addDoneLoadingListener(() -> {
            Log.i(TAG, "OSM WebTiled status=" + osm.getLoadStatus());
            if (osm.getLoadError() != null) {
                Log.e(TAG, "OSM WebTiled load error", osm.getLoadError());
                Toast.makeText(this, "OSM error: " + osm.getLoadError().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        osm.loadAsync();
        map.setBasemap(new Basemap(osm));
    }

    private void setBasemapOSOutdoor() {
        String osKey = PreferenceManager.getDefaultSharedPreferences(this).getString("os_api_key", "");
        if (osKey == null || osKey.trim().isEmpty()) {
            Toast.makeText(this, "Set Ordnance Survey API key in Preferences", Toast.LENGTH_LONG).show();
            return;
        }
        String templateUrl = "https://api.os.uk/maps/raster/v1/zxy/Outdoor_3857/{level}/{col}/{row}.png?key=" + osKey;
        // Provide explicit Web Mercator tile schema so ArcGIS knows how to fetch tiles
        TileInfo tileInfo = createWebMercatorOsmTileInfo();
        Envelope world = new Envelope(
                -20037508.3427892, -20037508.3427892,
                 20037508.3427892,  20037508.3427892,
                SpatialReference.create(3857)
        );
        WebTiledLayer osOutdoor = new WebTiledLayer(templateUrl, tileInfo, world);
        RequestConfiguration rc = new RequestConfiguration();
        rc.getHeaders().put("User-Agent", "TrigpointingUK/1.0");
        osOutdoor.setRequestConfiguration(rc);
        osOutdoor.addDoneLoadingListener(() -> {
            Log.i(TAG, "OS Outdoor status=" + osOutdoor.getLoadStatus());
            if (osOutdoor.getLoadError() != null) {
                Log.e(TAG, "OS Outdoor load error", osOutdoor.getLoadError());
                Toast.makeText(this, "OS Outdoor error: " + osOutdoor.getLoadError().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        osOutdoor.loadAsync();
        map.setBasemap(new Basemap(osOutdoor));
        mapView3857.setViewpointAsync(new Viewpoint(54.0, -2.0, 1_500_000));
    }

    private TileInfo createWebMercatorOsmTileInfo() {
        SpatialReference sr = SpatialReference.create(3857);
        Point origin = new Point(-20037508.3427892, 20037508.3427892, sr);
        java.util.List<LevelOfDetail> lods = new java.util.ArrayList<>();
        // Standard OSM/Google Web Mercator scales/resolutions
        double[] scales = new double[]{
                591657527.591555, 295828763.795777, 147914381.897889, 73957190.948944,
                36978595.474472, 18489297.737236, 9244648.868618, 4622324.434309,
                2311162.217155, 1155581.108577, 577790.554289, 288895.277144,
                144447.638572, 72223.819286, 36111.909643, 18055.954822,
                9027.977411, 4513.988705, 2256.994353
        };
        double[] resolutions = new double[]{
                156543.033928, 78271.516964, 39135.758482, 19567.879241,
                9783.9396205, 4891.96981025, 2445.98490513, 1222.99245256,
                611.496226281, 305.748113141, 152.87405657, 76.4370282852,
                38.2185141426, 19.1092570713, 9.55462853565, 4.77731426782,
                2.38865713391, 1.19432856696, 0.597164283478
        };
        for (int i = 0; i < scales.length; i++) {
            lods.add(new LevelOfDetail(i, resolutions[i], scales[i]));
        }
        return new TileInfo(96, TileInfo.ImageFormat.PNG, lods, origin, sr, 256, 256);
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


