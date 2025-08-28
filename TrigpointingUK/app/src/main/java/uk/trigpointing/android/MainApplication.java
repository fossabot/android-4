package uk.trigpointing.android;

// import org.acra.ACRA;
// import org.acra.config.CoreConfigurationBuilder;
// import org.acra.config.MailSenderConfigurationBuilder;
// import org.acra.data.StringFormat;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import androidx.preference.PreferenceManager;
import android.util.Log;



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
		
		// Mark that the app is starting fresh - this will trigger logging status filter reset
		editor.putBoolean("app_fresh_start", true);
		
		// Reset map load flag so first map visit uses user preference
		editor.putBoolean("is_first_map_load", true);
		
		editor.apply();
		
		Log.i(TAG, "Marked app as fresh start for filter reset and map preference loading");  
        
        // Crashlytics removed during package migration; re-add if needed later

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
