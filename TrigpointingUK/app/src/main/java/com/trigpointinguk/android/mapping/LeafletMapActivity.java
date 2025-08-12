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
import org.osmdroid.util.BoundingBox;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.trigpointinguk.android.DbHelper;
import com.trigpointinguk.android.R;
import com.trigpointinguk.android.filter.Filter;
import com.trigpointinguk.android.types.Condition;
import com.trigpointinguk.android.types.Trig;

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
                    tab.setText("Filter");
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
        Log.d(TAG, "Updated marker color to: " + color);
    }

    public void updateFilter(String filter) {
        webView.evaluateJavascript("if (typeof updateFilter === 'function') updateFilter('" + filter + "');", null);
        Log.d(TAG, "Updated filter to: " + filter);
    }

    private String queryTrigpoints(double south, double west, double north, double east, String filter, String colorScheme) {
        if (dbHelper == null) {
            Log.e(TAG, "Database helper not initialized");
            return "[]";
        }

        try {
            // Convert leaflet filter names to Filter constants
            setupFilterPreferences(filter);
            
            // Create bounding box for query
            BoundingBox bounds = new BoundingBox(north, east, south, west);
            
            // Query database using existing OSMdroid method
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

    private void setupFilterPreferences(String filter) {
        // Convert JavaScript filter names to Filter preference values
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Set filter type based on filter parameter
        switch (filter) {
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
        public void getTrigpointData(double south, double west, double north, double east, String filter, String colorScheme) {
            Log.d(TAG, String.format("getTrigpointData: bounds=(%.6f,%.6f,%.6f,%.6f) filter=%s color=%s", south, west, north, east, filter, colorScheme));
            
            // Run database query on background thread
            new Thread(() -> {
                try {
                    String trigpointsJson = queryTrigpoints(south, west, north, east, filter, colorScheme);
                    
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
    }
}


