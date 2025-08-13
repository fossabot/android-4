package com.trigpointinguk.android.trigdetails;

import android.app.LocalActivityManager;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;

import com.trigpointinguk.android.R;
import com.trigpointinguk.android.common.ThemeUtils;
import com.trigpointinguk.android.logging.LogTrigActivity;

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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}