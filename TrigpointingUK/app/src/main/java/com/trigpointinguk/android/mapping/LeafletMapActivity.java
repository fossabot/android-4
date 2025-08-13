package com.trigpointinguk.android.mapping;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.database.Cursor;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.json.JSONArray;
import org.json.JSONObject;
import com.trigpointinguk.android.mapping.BoundingBox;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.trigpointinguk.android.DbHelper;
import com.trigpointinguk.android.R;
import com.trigpointinguk.android.common.FileCache;
import com.trigpointinguk.android.common.ThemeUtils;
import com.trigpointinguk.android.DownloadTrigsActivity;
import com.trigpointinguk.android.filter.Filter;

public class LeafletMapActivity extends AppCompatActivity {
    private static final String TAG = "LeafletMapActivity";
    private WebView webView;
    private static final int REQ_LOCATION = 2001;
    private DbHelper dbHelper;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.leaflet_map);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Ensure proper content positioning to prevent action bar overlap
        ThemeUtils.setupContentPositioning(this);

        // Initialize database helper
        try {
            dbHelper = new DbHelper(this);
            dbHelper.open();
        } catch (Exception e) {
            Log.e(TAG, "Error opening database", e);
            Toast.makeText(this, "Error opening database", Toast.LENGTH_SHORT).show();
        }

        webView = findViewById(R.id.leafletWebView);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(false);
        
        // Enable Service Worker support for file:// URLs
        ws.setAllowFileAccessFromFileURLs(true);
        ws.setAllowUniversalAccessFromFileURLs(true);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        // Enable aggressive caching for offline support
        ws.setCacheMode(WebSettings.LOAD_DEFAULT); // Cache when possible
        ws.setDatabaseEnabled(true);
        ws.setGeolocationEnabled(true);
        
        // Add JavaScript interface for saving preferences
        webView.addJavascriptInterface(new LeafletPreferencesInterface(), "AndroidPrefs");
        
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                // Auto-grant geolocation permission for this WebView session
                callback.invoke(origin, true, false);
            }
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String osKey = prefs.getString("os_api_key", "");
        String leafletMapStyle = prefs.getString("leaflet_map_style", "OpenStreetMap");
        
        String url = buildLeafletUrl(osKey, leafletMapStyle);
        try {
            if (ensureLocationPermission()) {
                webView.loadUrl(url);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading Leaflet page", e);
            Toast.makeText(this, "Error loading Leaflet page", Toast.LENGTH_LONG).show();
        }
    }

    private String buildLeafletUrl(String osKey, String leafletMapStyle) {
        try {
            StringBuilder url = new StringBuilder("file:///android_asset/leaflet/index.html");
            boolean hasParams = false;
            
            if (!osKey.isEmpty()) {
                url.append("?os_key=").append(java.net.URLEncoder.encode(osKey, "UTF-8"));
                hasParams = true;
            }
            
            if (!leafletMapStyle.isEmpty()) {
                url.append(hasParams ? "&" : "?");
                url.append("initial_style=").append(java.net.URLEncoder.encode(leafletMapStyle, "UTF-8"));
            }
            
            return url.toString();
        } catch (java.io.UnsupportedEncodingException e) {
            Log.e(TAG, "Error encoding URL parameters", e);
            return "file:///android_asset/leaflet/index.html";
        }
    }

    private boolean ensureLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                String osKey = prefs.getString("os_api_key", "");
                String leafletMapStyle = prefs.getString("leaflet_map_style", "OpenStreetMap");
                String url = buildLeafletUrl(osKey, leafletMapStyle);
                webView.loadUrl(url);
            } else {
                Toast.makeText(this, "Location permission denied; My location will be disabled.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.leaflet_map_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.menu_map_controls) {
            showMapControlsBottomSheet();
            return true;
        } else if (item.getItemId() == R.id.menu_cache_status) {
            webView.evaluateJavascript(
                "getCacheStatus().then(status => {" +
                "  if (status) {" +
                "    if (status.error) {" +
                "      AndroidPrefs.showCacheDialog(status.error, '');" +
                "    } else {" +
                "      let message = '';" +
                "      if (status.mode) message += 'Mode: ' + status.mode + '\\n';" +
                "      message += 'Tiles: ' + status.tileCount + '\\n';" +
                "      message += 'Size: ' + Math.round(status.totalSize/1024/1024) + ' MB\\n';" +
                "      if (status.usagePercent !== undefined) message += 'Usage: ' + status.usagePercent + '%\\n';" +
                "      if (status.cacheAge) message += 'Age: ' + status.cacheAge + '\\n';" +
                "      let note = status.note || '';" +
                "      AndroidPrefs.showCacheDialog(message, note);" +
                "    }" +
                "  } else {" +
                "    AndroidPrefs.showCacheDialog('Cache status not available', 'Check console for details');" +
                "  }" +
                "});", 
                null
            );
            return true;
        } else if (item.getItemId() == R.id.menu_clear_cache) {
            // Clear both WebView tile cache and static map images cache
            clearAllCaches();
            return true;
        } else if (item.getItemId() == R.id.menu_download_tiles) {
            // For now, use a placeholder URL - this would be your website URL
            String osmTilesUrl = "https://example.com/osm-tiles.zip";
            webView.evaluateJavascript(
                "downloadBulkTiles('" + osmTilesUrl + "')" +
                ".then(result => {" +
                "  AndroidPrefs.showDialog('Bulk Download', 'Completed: ' + result.message);" +
                "})" +
                ".catch(error => {" +
                "  AndroidPrefs.showDialog('Bulk Download', 'Failed: ' + error.message);" +
                "});", 
                null
            );
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showMapControlsBottomSheet() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.leaflet_map_controls_bottom_sheet, null);
        
        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        ViewPager2 viewPager = view.findViewById(R.id.viewPager);
        
        MapControlsTabAdapter adapter = new MapControlsTabAdapter(this);
        viewPager.setAdapter(adapter);
        
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Map Style");
                    break;
                case 1:
                    tab.setText("Markers");
                    break;
                case 2:
                    tab.setText("Trigpoint types");
                    break;
                case 3:
                    tab.setText("Filter found");
                    break;
            }
        }).attach();
        
        bottomSheet.setContentView(view);
        bottomSheet.show();
    }

    // Callback methods for tab fragments
    public void updateMapStyle(String style) {
        webView.evaluateJavascript("if (typeof switchToLayer === 'function') switchToLayer('" + style + "');", null);
        Log.d(TAG, "Updated map style to: " + style);
    }

    public void updateMarkerColor(String color) {
        webView.evaluateJavascript("if (typeof updateMarkerColors === 'function') updateMarkerColors('" + color + "');", null);
        Log.d(TAG, "Updated marker colour to: " + color);
    }

    public void updateTrigpointType(String type) {
        webView.evaluateJavascript("if (typeof updateTrigpointType === 'function') updateTrigpointType('" + type + "');", null);
        Log.d(TAG, "Updated trigpoint type to: " + type);
    }
    
    public void updateFilterFound(String found) {
        webView.evaluateJavascript("if (typeof updateFilterFound === 'function') updateFilterFound('" + found + "');", null);
        Log.d(TAG, "Updated filter found to: " + found);
    }

    private String queryTrigpoints(double south, double west, double north, double east, String trigpointType, String filterFound, String colorScheme) {
        if (dbHelper == null) {
            Log.e(TAG, "Database helper not initialised");
            return "[]";
        }

        try {
            // Convert leaflet filter names to Filter constants
            setupFilterPreferences(trigpointType, filterFound);
            
            // Create bounding box for query
            BoundingBox bounds = new BoundingBox(north, east, south, west);
            
            // Query database using bounding box
            Cursor cursor = dbHelper.fetchTrigMapList(bounds);
            
            if (cursor == null) {
                return "[]";
            }

            JSONArray trigpoints = new JSONArray();
            
            while (cursor.moveToNext()) {
                JSONObject trig = new JSONObject();
                
                trig.put("id", cursor.getLong(cursor.getColumnIndex(DbHelper.TRIG_ID)));
                trig.put("name", cursor.getString(cursor.getColumnIndex(DbHelper.TRIG_NAME)));
                trig.put("lat", cursor.getDouble(cursor.getColumnIndex(DbHelper.TRIG_LAT)));
                trig.put("lon", cursor.getDouble(cursor.getColumnIndex(DbHelper.TRIG_LON)));
                trig.put("type", cursor.getString(cursor.getColumnIndex(DbHelper.TRIG_TYPE)));
                trig.put("condition", cursor.getString(cursor.getColumnIndex(DbHelper.TRIG_CONDITION)));
                trig.put("logged", cursor.getString(cursor.getColumnIndex(DbHelper.TRIG_LOGGED)));
                
                // Check if flagged/marked
                String marked = cursor.getString(cursor.getColumnIndex(DbHelper.JOIN_MARKED));
                trig.put("flagged", marked != null);
                
                trigpoints.put(trig);
            }
            
            cursor.close();
            
            Log.d(TAG, "Returning " + trigpoints.length() + " trigpoints");
            return trigpoints.toString();
            
        } catch (Exception e) {
            Log.e(TAG, "Error querying trigpoints", e);
            return "[]";
        }
    }

    private void setupFilterPreferences(String trigpointType, String filterFound) {
        // Convert JavaScript filter names to Filter preference values
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Set trigpoint type filter based on trigpointType parameter
        switch (trigpointType) {
            case "all":
                editor.putInt(Filter.FILTERTYPE, 6); // TYPESALL
                break;
            case "pillars":
                editor.putInt(Filter.FILTERTYPE, 0); // TYPESPILLAR
                break;
            case "fbm":
                editor.putInt(Filter.FILTERTYPE, 2); // TYPESFBM
                break;
            case "passive":
                editor.putInt(Filter.FILTERTYPE, 3); // TYPESPASSIVE
                break;
            case "intersected":
                editor.putInt(Filter.FILTERTYPE, 4); // TYPESINTERSECTED
                break;
            default:
                editor.putInt(Filter.FILTERTYPE, 6); // TYPESALL
        }
        
        // Set filter found status based on filterFound parameter
        switch (filterFound) {
            case "all":
                editor.putInt(Filter.FILTERRADIO, 0); // All
                break;
            case "logged":
                editor.putInt(Filter.FILTERRADIO, 1); // Logged
                break;
            case "notlogged":
                editor.putInt(Filter.FILTERRADIO, 2); // Not Logged
                break;
            case "marked":
                editor.putInt(Filter.FILTERRADIO, 3); // Marked
                break;
            case "unsynced":
                editor.putInt(Filter.FILTERRADIO, 4); // Unsynced
                break;
            default:
                editor.putInt(Filter.FILTERRADIO, 0); // All
        }
        
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }

    public class LeafletPreferencesInterface {
        @JavascriptInterface
        public void saveMapStyle(String mapStyle) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LeafletMapActivity.this);
            prefs.edit().putString("leaflet_map_style", mapStyle).apply();
            Log.d(TAG, "Saved map style preference: " + mapStyle);
        }
        
        @JavascriptInterface
        public void getTrigpointData(double south, double west, double north, double east, String trigpointType, String filterFound, String colorScheme) {
            Log.d(TAG, String.format("getTrigpointData: bounds=(%.6f,%.6f,%.6f,%.6f) type=%s found=%s colour=%s", south, west, north, east, trigpointType, filterFound, colorScheme));
            
            // Run database query on background thread
            new Thread(() -> {
                try {
                    String trigpointsJson = queryTrigpoints(south, west, north, east, trigpointType, filterFound, colorScheme);
                    
                    // Return results to JavaScript on UI thread
                    runOnUiThread(() -> {
                        webView.evaluateJavascript("displayTrigpointMarkers('" + trigpointsJson.replace("'", "\\'") + "');", null);
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error querying trigpoints", e);
                }
            }).start();
        }
        
        @JavascriptInterface
        public void openTrigDetails(long trigId) {
            Log.d(TAG, "Opening trig details for ID: " + trigId);
            Intent i = new Intent(LeafletMapActivity.this, com.trigpointinguk.android.trigdetails.TrigDetailsActivity.class);
            i.putExtra(com.trigpointinguk.android.DbHelper.TRIG_ID, trigId);
            startActivity(i);
        }
        
        @JavascriptInterface
        public void showCacheDialog(String message, String note) {
            runOnUiThread(() -> {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(LeafletMapActivity.this);
                builder.setTitle("Cache Status");
                
                String fullMessage = message;
                if (note != null && !note.trim().isEmpty()) {
                    fullMessage += "\n\n" + note;
                }
                
                builder.setMessage(fullMessage);
                builder.setPositiveButton("OK", null);
                builder.show();
            });
        }
        
        @JavascriptInterface
        public void showDialog(String title, String message) {
            runOnUiThread(() -> {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(LeafletMapActivity.this);
                builder.setTitle(title);
                builder.setMessage(message);
                builder.setPositiveButton("OK", null);
                builder.show();
            });
        }
        
        @JavascriptInterface
        public String getIconStyle() {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LeafletMapActivity.this);
            return prefs.getString("map_icon_style", "medium");
        }
        

    }
    
    private void clearAllCaches() {
        // Clear WebView tile cache via JavaScript
        webView.evaluateJavascript(
            "clearTileCache().then(result => {" +
            "  console.log('WebView tile cache cleared:', result);" +
            "});", 
            null
        );
        
        // Clear static map images cache and webview tiles cache in background thread
        new Thread(() -> {
            try {
                FileCache mapImagesCache = new FileCache(this, "map_images");
                int mapImageFiles = mapImagesCache.clear();
                Log.d(TAG, "Cleared " + mapImageFiles + " static map image files");
                
                FileCache webViewTilesCache = new FileCache(this, "webview_tiles");
                int webViewTileFiles = webViewTilesCache.clear();
                Log.d(TAG, "Cleared " + webViewTileFiles + " webview tile files");
                
                int totalFiles = mapImageFiles + webViewTileFiles;
                
                runOnUiThread(() -> {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(LeafletMapActivity.this);
                    builder.setTitle("Clear Cache");
                    builder.setMessage("Cache cleared successfully.\n" +
                                     "WebView tiles, " + mapImageFiles + " static map images, and " + 
                                     webViewTileFiles + " bulk download tiles removed.\n\n" +
                                     "Starting trigpoint data download...");
                    builder.setPositiveButton("OK", (dialog, which) -> {
                        // Trigger trigpoint download after user acknowledges
                        triggerTrigpointDownload();
                    });
                    builder.show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error clearing static map images cache", e);
                runOnUiThread(() -> {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(LeafletMapActivity.this);
                    builder.setTitle("Clear Cache");
                    builder.setMessage("Cache partially cleared. Some files may not have been removed.");
                    builder.setPositiveButton("OK", null);
                    builder.show();
                });
            }
        }).start();
    }
    
    private void triggerTrigpointDownload() {
        Log.d(TAG, "triggerTrigpointDownload: Starting DownloadTrigsActivity");
        
        // Launch DownloadTrigsActivity which will download trigpoint data and sync user data
        Intent intent = new Intent(this, DownloadTrigsActivity.class);
        startActivity(intent);
    }
}


