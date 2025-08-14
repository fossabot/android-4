package com.trigpointinguk.android.trigdetails;

import android.app.LocalActivityManager;
import android.content.Intent;
import android.net.Uri;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;
import android.view.MenuItem;
import android.view.Menu;
import androidx.appcompat.app.AppCompatActivity;

import com.trigpointinguk.android.R;
import com.trigpointinguk.android.common.ThemeUtils;
import com.trigpointinguk.android.logging.LogTrigActivity;
import com.trigpointinguk.android.DbHelper;
import androidx.browser.customtabs.CustomTabsIntent;
import android.widget.Toast;
import android.database.Cursor;

public class TrigDetailsActivity extends AppCompatActivity {

	private static final String TAG="TrigDetailsActivity";
    private LocalActivityManager mLocalActivityManager;

	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.trigdetails);
	    
	    if (getSupportActionBar() != null) {
	        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	    }
	    
	    ThemeUtils.setupContentPositioning(this);

        Bundle extras = getIntent().getExtras();
        long ensuredTrigId = getIntent().getLongExtra(DbHelper.TRIG_ID, -1);
        if (extras == null) {
            extras = new Bundle();
        }
        if (ensuredTrigId > 0 && !extras.containsKey(DbHelper.TRIG_ID)) {
            extras.putLong(DbHelper.TRIG_ID, ensuredTrigId);
        }
        Resources res = getResources();
        TabHost tabHost = findViewById(android.R.id.tabhost);
        mLocalActivityManager = new LocalActivityManager(this, false);
        mLocalActivityManager.dispatchCreate(savedInstanceState);
        tabHost.setup(mLocalActivityManager);

        TabHost.TabSpec spec;
        Intent intent;

        intent = new Intent().setClass(this, TrigDetailsInfoTab.class);
        intent.putExtras(extras);
        spec = tabHost.newTabSpec("info").setIndicator("",
                        res.getDrawable(android.R.drawable.ic_menu_info_details))
                        .setContent(intent);
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, TrigDetailsLoglistTab.class);
        intent.putExtras(extras);
        spec = tabHost.newTabSpec("logs").setIndicator("",
                          res.getDrawable(android.R.drawable.ic_menu_agenda))
                      .setContent(intent);
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, TrigDetailsAlbumTab.class);
        intent.putExtras(extras);
        spec = tabHost.newTabSpec("album").setIndicator("",
                          res.getDrawable(android.R.drawable.ic_menu_gallery))
                      .setContent(intent);
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, TrigDetailsOSMapTab.class);
        intent.putExtras(extras);
        spec = tabHost.newTabSpec("map").setIndicator("",
                          res.getDrawable(android.R.drawable.ic_menu_mapmode))
                      .setContent(intent);
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, LogTrigActivity.class);
        intent.putExtras(extras);
        spec = tabHost.newTabSpec("mylog").setIndicator("",
                          res.getDrawable(android.R.drawable.ic_menu_edit))
                      .setContent(intent);
        tabHost.addTab(spec);

        tabHost.setCurrentTab(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mLocalActivityManager != null) {
            mLocalActivityManager.dispatchResume();
        }
    }

    @Override
    protected void onPause() {
        if (mLocalActivityManager != null) {
            mLocalActivityManager.dispatchPause(isFinishing());
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.trigdetails_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        long trigId = getIntent().getLongExtra(DbHelper.TRIG_ID, -1);
        double[] coords = loadTrigLatLon(trigId);
        double lat = coords[0];
        double lon = coords[1];

        int id = item.getItemId();
        if (id == R.id.action_open_web) {
            if (trigId > 0) {
                String url = "https://trigpointing.uk/trig/" + trigId;
                try {
                    CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                    CustomTabsIntent customTabsIntent = builder.build();
                    customTabsIntent.launchUrl(this, Uri.parse(url));
                } catch (Exception e) {
                    Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(web);
                }
            } else {
                Toast.makeText(this, "Unknown trigpoint id", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (id == R.id.action_navigate) {
            String nav = String.format("google.navigation:q=%f,%f", lat, lon);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(nav));
            startActivity(Intent.createChooser(intent, "Navigate to trigpoint"));
            return true;
        } else if (id == R.id.action_radar) {
            Intent intent = new Intent(this, com.trigpointinguk.android.radar.RadarActivity.class);
            intent.putExtra(DbHelper.TRIG_ID, trigId);
            intent.putExtra(DbHelper.TRIG_LAT, lat);
            intent.putExtra(DbHelper.TRIG_LON, lon);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private double[] loadTrigLatLon(long trigId) {
        double lat = 0d, lon = 0d;
        if (trigId <= 0) return new double[]{lat, lon};
        DbHelper db = null;
        Cursor c = null;
        try {
            db = new DbHelper(this);
            db.open();
            c = db.fetchTrigInfo(trigId);
            if (c != null && c.moveToFirst()) {
                lat = c.getDouble(c.getColumnIndex(DbHelper.TRIG_LAT));
                lon = c.getDouble(c.getColumnIndex(DbHelper.TRIG_LON));
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return new double[]{lat, lon};
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}