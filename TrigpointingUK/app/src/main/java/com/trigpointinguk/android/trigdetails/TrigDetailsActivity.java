package com.trigpointinguk.android.trigdetails;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.TabHost;
import android.view.KeyEvent;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;

import com.trigpointinguk.android.DbHelper;
import com.trigpointinguk.android.R;
import com.trigpointinguk.android.logging.LogTrigActivity;

public class TrigDetailsActivity extends AppCompatActivity {

	private static final String TAG="TrigDetailsActivity";
    //private SharedPreferences mPrefs;
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.trigdetails);
	    
	    // Enable back button in action bar
	    if (getSupportActionBar() != null) {
	        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	    }

		Bundle extras = getIntent().getExtras();
        //mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

	    
	    Resources res = getResources();
	    TabHost tabHost = findViewById(android.R.id.tabhost);
	    tabHost.setup(); // Initialize the TabHost
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
	    
	    // Change title
	    // get trig_id from extras
		if (extras == null) {return;}
		Long  trigId = extras.getLong(DbHelper.TRIG_ID);
		Log.i(TAG, "Trig_id = "+trigId);

		// get trig info from database
		DbHelper mDb = new DbHelper(this);
		try {
			mDb.open();		
			Cursor c = mDb.fetchTrigInfo(trigId);
			c.moveToFirst();
				
			String title = String.format("TrigpointingUK - %s" 
					, c.getString(c.getColumnIndex(DbHelper.TRIG_NAME))
			);
			this.setTitle(title);
			c.close();
	        mDb.close();
		} catch (Exception e) {
		} finally {
			mDb.close();
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			// Handle back button in action bar
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// Ensure we finish this activity and return to the previous one
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}