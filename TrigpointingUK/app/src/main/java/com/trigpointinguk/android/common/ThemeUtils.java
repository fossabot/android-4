package com.trigpointinguk.android.common;

import android.content.Context;
import android.util.TypedValue;
import androidx.appcompat.app.AppCompatActivity;

public class ThemeUtils {
    
    /**
     * Get the action bar height for proper content positioning
     * This method is kept for reference but should not be needed with proper theme configuration
     */
    public static int getActionBarHeight(Context context) {
        int actionBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        }
        return actionBarHeight;
    }

    /**
     * Sets up proper content positioning for activities.
     * 
     * With the modernized Material Components theme and windowActionBarOverlay=false,
     * the Android framework now automatically positions content below the action bar.
     * This method is kept for compatibility but no longer needs to do anything.
     * 
     * The theme configuration handles:
     * - Positioning content below the action bar (windowActionBarOverlay=false)
     * - Proper status bar handling (windowDrawsSystemBarBackgrounds=true)
     * - Consistent behavior across all devices
     */
    public static void setupContentPositioning(AppCompatActivity activity) {
        // No manual adjustment needed - the Material Components theme with
        // windowActionBarOverlay=false ensures content is properly positioned
        // below the action bar on all devices.
    }
}
