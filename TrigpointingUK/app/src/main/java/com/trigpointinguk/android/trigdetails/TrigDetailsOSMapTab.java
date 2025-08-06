package com.trigpointinguk.android.trigdetails;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Gallery;

import com.trigpointinguk.android.DbHelper;
import com.trigpointinguk.android.R;
import com.trigpointinguk.android.common.DisplayBitmapActivity;

public class TrigDetailsOSMapTab extends Activity {
	private static final String TAG = "TrigDetailsOSMapTab";

	private long mTrigId;
	private DbHelper mDb;
	String[] mUrls;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.trigosmap);

		// get trig_id from extras
        Bundle extras = getIntent().getExtras();
		if (extras == null) {return;}
		mTrigId = extras.getLong(DbHelper.TRIG_ID);
		Log.i(TAG, "Trig_id = "+mTrigId);
		
		// get trig info from database
		mDb = new DbHelper(TrigDetailsOSMapTab.this);
		mDb.open();		
		Cursor c = mDb.fetchTrigInfo(mTrigId);
		c.moveToFirst();
		mUrls = getURLs(mTrigId, c);
		c.close();
		
	    Gallery gallery = (Gallery) findViewById(R.id.trigosgallery);
	    gallery.setAdapter(new TrigDetailsOSMapAdapter(this, mUrls));

	    
	    
	    gallery.setOnItemClickListener(new OnItemClickListener() {
	        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
	            Intent i = new Intent(TrigDetailsOSMapTab.this, DisplayBitmapActivity.class);
	            i.putExtra("URL", mUrls[position]);
	            Log.i(TAG, "Clicked OSMap at URL: " +mUrls[position]);
	            startActivity(i);
	        }
	    });
	}
	
	
	
	
	
	@Override
	protected void onDestroy() {
		if (mDb != null) {
			mDb.close();
		}
		super.onDestroy();
	}





	public String[] getURLs (long mTrigId2, Cursor c) {
		String url;
		List<String> URLs = new ArrayList<String>();
		
		Double lat = c.getDouble(c.getColumnIndex(DbHelper.TRIG_LAT));
		Double lon = c.getDouble(c.getColumnIndex(DbHelper.TRIG_LON));

		Log.d(TAG, "Generating map URLs for lat: " + lat + ", lon: " + lon);

		// OS 1:25000 maps - Updated to HTTPS
		url = String.format("%s/%s/%s,%s/%d?key=%s",
				"https://dev.virtualearth.net/REST/v1/Imagery/Map",
				"OrdnanceSurvey",
				lat, lon,
				13,
				"AmX-6eFz_aE2rrhkXUprU3HRV2BNMrCYQoKodIFdfNEcZosjAEbsNetB00GFktP5");
		Log.d(TAG, "OS Map URL 1: " + url);
		URLs.add(url);

		url = String.format("%s/%s/%s,%s/%d?key=%s",
				"https://dev.virtualearth.net/REST/v1/Imagery/Map",
				"OrdnanceSurvey",
				lat, lon,
				15,
				"AmX-6eFz_aE2rrhkXUprU3HRV2BNMrCYQoKodIFdfNEcZosjAEbsNetB00GFktP5");
		Log.d(TAG, "OS Map URL 2: " + url);
		URLs.add(url);

		// Aerial photos - Updated to HTTPS
		url = String.format("%s/%s/%s,%s/%d?key=%s",
				"https://dev.virtualearth.net/REST/v1/Imagery/Map",
				"Aerial",
				lat, lon,
				14,
				"AmX-6eFz_aE2rrhkXUprU3HRV2BNMrCYQoKodIFdfNEcZosjAEbsNetB00GFktP5");
		Log.d(TAG, "Aerial URL 1: " + url);
		URLs.add(url);

		url = String.format("%s/%s/%s,%s/%d?key=%s",
				"https://dev.virtualearth.net/REST/v1/Imagery/Map",
				"Aerial",
				lat, lon,
				17,
				"AmX-6eFz_aE2rrhkXUprU3HRV2BNMrCYQoKodIFdfNEcZosjAEbsNetB00GFktP5");
		Log.d(TAG, "Aerial URL 2: " + url);
		URLs.add(url);

		url = String.format("%s/%s/%s,%s/%d?key=%s",
				"https://dev.virtualearth.net/REST/v1/Imagery/Map",
				"Aerial",
				lat, lon,
				19,
				"AmX-6eFz_aE2rrhkXUprU3HRV2BNMrCYQoKodIFdfNEcZosjAEbsNetB00GFktP5");
		Log.d(TAG, "Aerial URL 3: " + url);
		URLs.add(url);

		Log.d(TAG, "Generated " + URLs.size() + " map URLs");
		return URLs.toArray(new String[URLs.size()]);
	}
	
}
