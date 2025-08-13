package com.trigpointinguk.android;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import com.trigpointinguk.android.common.ThemeUtils;

public class PreferencesActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "TUKPrefsFile";
    public static final String TAG = "PreferenceActivity";
    public static final String PREFERENCETYPE = "PreferenceType"; 
    public static final int MAINPREFERENCES = 1; 
    public static final int FILTERPREFERENCES = 2; 
    private int mPreferenceType = MAINPREFERENCES;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preferences_activity);
		
		// Enable back button in action bar
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}
		
		// Ensure proper content positioning to prevent action bar overlap
		ThemeUtils.setupContentPositioning(this);
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mPreferenceType = extras.getInt(PREFERENCETYPE);
		}
		
		// Load the appropriate preference fragment
		if (savedInstanceState == null) {
			getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.preferences_container, new PreferencesFragment())
				.commit();
		}
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "onPause");
		super.onPause();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		String plaintext = prefs.getString("password", "");
		if (!plaintext.equals("")) {
			Editor editor = prefs.edit();
			editor.putString("plaintextpassword", plaintext);
			editor.remove("password");
			editor.apply();
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
	
	/**
	 * Preference Fragment to handle the preferences
	 */
	public static class PreferencesFragment extends PreferenceFragmentCompat {
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.preferences, rootKey);
		}
	}
	
	
}
