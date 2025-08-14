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

import com.google.firebase.crashlytics.FirebaseCrashlytics;


public class MainApplication extends Application {
	private static final String TAG = "MainApplication";

	    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.i(TAG, "Application starting - using Leaflet for all mapping");
        
        // This has been removed from the permissions and the preferences, so set to false for any legacy installations
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = prefs.edit();
		editor.putBoolean("acra.syslog.enable", false);
		editor.apply();  
        
        // Initialize Crashlytics user identity from preferences if available
        try {
            String username = prefs.getString("username", "");
            if (username != null && !username.trim().isEmpty()) {
                FirebaseCrashlytics.getInstance().setUserId(username);
                Log.i(TAG, "Crashlytics user set to '" + username + "'");
            }
        } catch (Exception ignored) {}

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
