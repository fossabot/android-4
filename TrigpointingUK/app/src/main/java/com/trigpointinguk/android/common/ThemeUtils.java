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
        View contentView = activity.findViewById(android.R.id.content);
        if (contentView instanceof ViewGroup contentGroup) {
            if (contentGroup.getChildCount() > 0) {
                View rootChild = contentGroup.getChildAt(0);

                int actionBarHeight = getActionBarHeight(activity);
                // Nudge content downward a bit more than before (about one line = 16dp)
                int extraPaddingPx = Math.round(TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 24,
                        activity.getResources().getDisplayMetrics()));

                int desiredTopPadding = actionBarHeight + extraPaddingPx;

                // Only increase if current padding is less than desired
                if (rootChild.getPaddingTop() < desiredTopPadding) {
                    rootChild.setPadding(
                            rootChild.getPaddingLeft(),
                            desiredTopPadding,
                            rootChild.getPaddingRight(),
                            rootChild.getPaddingBottom()
                    );
                }
            }
        }
    }
}
