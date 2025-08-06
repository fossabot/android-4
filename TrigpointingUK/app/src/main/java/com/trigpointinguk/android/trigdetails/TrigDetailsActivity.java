package com.trigpointinguk.android.trigdetails;

import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import com.trigpointinguk.android.DbHelper;
import com.trigpointinguk.android.R;

public class TrigDetailsActivity extends FragmentActivity {

	private static final String TAG="TrigDetailsActivity";
    //private SharedPreferences mPrefs;
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.trigdetails);

		Bundle extras = getIntent().getExtras();
        //mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

	    // Setup ViewPager2 and TabLayout
	    ViewPager2 viewPager = findViewById(R.id.viewPager);
	    TabLayout tabLayout = findViewById(R.id.tabLayout);
	    
	    // Create and set the adapter
	    TrigDetailsPagerAdapter pagerAdapter = new TrigDetailsPagerAdapter(this, extras);
	    viewPager.setAdapter(pagerAdapter);
	    
	    // Connect TabLayout with ViewPager2
	    new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
	        Resources res = getResources();
	        switch (position) {
	            case 0:
	                tab.setIcon(res.getDrawable(android.R.drawable.ic_menu_info_details));
	                break;
	            case 1:
	                tab.setIcon(res.getDrawable(android.R.drawable.ic_menu_agenda));
	                break;
	            case 2:
	                tab.setIcon(res.getDrawable(android.R.drawable.ic_menu_gallery));
	                break;
	            case 3:
	                tab.setIcon(res.getDrawable(android.R.drawable.ic_menu_mapmode));
	                break;
	            case 4:
	                tab.setIcon(res.getDrawable(android.R.drawable.ic_menu_edit));
	                break;
	        }
	    }).attach();
	    
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
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// Ensure we finish this activity and return to the previous one
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}