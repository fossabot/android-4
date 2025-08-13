package com.trigpointinguk.android.trigdetails;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.trigpointinguk.android.DbHelper;
import com.trigpointinguk.android.R;
import com.trigpointinguk.android.common.ThemeUtils;
import com.trigpointinguk.android.logging.LogTrigActivity;

public class TrigDetailsActivity extends AppCompatActivity {

	private static final String TAG="TrigDetailsActivity";
    private Long mTrigId;
    private ImageView mTabInfo, mTabLogs, mTabAlbum, mTabMap, mTabMyLog;
    private FrameLayout mTabContent;
    private FragmentManager mFragmentManager;
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.trigdetails);
	    
	    // Enable back button in action bar
	    if (getSupportActionBar() != null) {
	        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	    }
	    
	    // Ensure proper content positioning to prevent action bar overlap
	    ThemeUtils.setupContentPositioning(this);

		Bundle extras = getIntent().getExtras();
		if (extras == null) {return;}
		
		mTrigId = extras.getLong(DbHelper.TRIG_ID);
		Log.i(TAG, "Trig_id = "+mTrigId);
		
		// Initialize views
		initializeViews();
		setupTabListeners();
		
		// Set initial tab
		showInfoTab();
		
		// Set title
		setTitleFromDatabase();
	}
	
	private void initializeViews() {
		mTabInfo = findViewById(R.id.tab_info);
		mTabLogs = findViewById(R.id.tab_logs);
		mTabAlbum = findViewById(R.id.tab_album);
		mTabMap = findViewById(R.id.tab_map);
		mTabMyLog = findViewById(R.id.tab_mylog);
		mTabContent = findViewById(R.id.tab_content);
		mFragmentManager = getSupportFragmentManager();
	}
	
	private void setupTabListeners() {
		mTabInfo.setOnClickListener(v -> showInfoTab());
		mTabLogs.setOnClickListener(v -> showLogsTab());
		mTabAlbum.setOnClickListener(v -> showAlbumTab());
		mTabMap.setOnClickListener(v -> showMapTab());
		mTabMyLog.setOnClickListener(v -> showMyLogTab());
	}
	
	private void showInfoTab() {
		Intent intent = new Intent(this, TrigDetailsInfoTab.class);
		intent.putExtra(DbHelper.TRIG_ID, mTrigId);
		startActivity(intent);
	}
	
	private void showLogsTab() {
		Intent intent = new Intent(this, TrigDetailsLoglistTab.class);
		intent.putExtra(DbHelper.TRIG_ID, mTrigId);
		startActivity(intent);
	}
	
	private void showAlbumTab() {
		Intent intent = new Intent(this, TrigDetailsAlbumTab.class);
		intent.putExtra(DbHelper.TRIG_ID, mTrigId);
		startActivity(intent);
	}
	
	private void showMapTab() {
		Intent intent = new Intent(this, TrigDetailsOSMapTab.class);
		intent.putExtra(DbHelper.TRIG_ID, mTrigId);
		startActivity(intent);
	}
	
	private void showMyLogTab() {
		Intent intent = new Intent(this, LogTrigActivity.class);
		intent.putExtra(DbHelper.TRIG_ID, mTrigId);
		startActivity(intent);
	}
	
	private void setTitleFromDatabase() {
		DbHelper mDb = new DbHelper(this);
		try {
			mDb.open();		
			Cursor c = mDb.fetchTrigInfo(mTrigId);
			c.moveToFirst();
				
			String title = String.format("TrigpointingUK - %s" 
					, c.getString(c.getColumnIndex(DbHelper.TRIG_NAME))
			);
			this.setTitle(title);
			c.close();
	        mDb.close();
		} catch (Exception e) {
			Log.e(TAG, "Error setting title", e);
		} finally {
			mDb.close();
		}
	}
}