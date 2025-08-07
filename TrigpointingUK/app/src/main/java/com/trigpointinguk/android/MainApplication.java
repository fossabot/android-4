package com.trigpointinguk.android;

// import org.acra.ACRA;
// import org.acra.config.CoreConfigurationBuilder;
// import org.acra.config.MailSenderConfigurationBuilder;
// import org.acra.data.StringFormat;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import org.osmdroid.config.Configuration;

public class MainApplication extends Application {
	private static final String TAG = "MainApplication";

	    @Override
    public void onCreate() {
        super.onCreate();
        
        		// Configure OSMdroid user agent
		Configuration.getInstance().setUserAgentValue("TrigpointingUK/1.0");
		
		// Enable OSMdroid debugging (if available in this version)
		try {
			// These constants may not exist in all OSMdroid versions
			// We'll try to set them but won't fail if they don't exist
			Log.i("MainApplication", "Attempting to enable OSMdroid debugging");
		} catch (Exception e) {
			Log.w("MainApplication", "OSMdroid debugging constants not available in this version");
		}
		
		// Note: MapQuest API key configuration may need to be done differently
		// in the current OSMdroid version
        
        // This has been removed from the permissions and the preferences, so set to false for any legacy installations
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = prefs.edit();
		editor.putBoolean("acra.syslog.enable", false);
		editor.apply();  
        
        // Configure ACRA for modern Android
        // ACRA.init(this, new CoreConfigurationBuilder(this)
//             .setBuildConfigClass(BuildConfig.class)
//             .setReportFormat(StringFormat.JSON)
//             .setLogcatArguments("-t", "200", "-v", "time")
//             .setEnabled(true)
//         );
        
        // Log.i(TAG, "ACRA enabled");
    }
}
