package com.trigpointinguk.android.common;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatActivity;

public class ThemeUtils {
    
    /**
     * Get the action bar height for proper content positioning
     */
    public static int getActionBarHeight(Context context) {
        int actionBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        }
        return actionBarHeight;
    }

    public static void setupContentPositioning(AppCompatActivity activity) {
        // When using windowActionBar=true in the theme (which we are),
        // the Android framework automatically handles content positioning
        // below the action bar. We should not add any manual padding
        // as this causes device-specific layout issues.
        
        // The theme already has:
        // - windowActionBar=true (shows the action bar)
        // - windowNoTitle=false (ensures title is shown)
        // - fitsSystemWindows=false (we handle our own insets)
        
        // Therefore, we don't need to manually adjust padding.
        // The system will automatically position content below the action bar.
        
        // Note: If specific activities need custom positioning, they should
        // handle it individually rather than applying a global padding adjustment.
    }
}
