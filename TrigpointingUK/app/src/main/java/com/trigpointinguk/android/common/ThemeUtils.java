package com.trigpointinguk.android.common;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
    
    /**
     * Get the status bar height
     */
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
    
    /**
     * Apply proper top margin to a view to account for action bar
     */
    public static void applyActionBarMargin(View view, Context context) {
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            int actionBarHeight = getActionBarHeight(context);
            int statusBarHeight = getStatusBarHeight(context);
            
            // Add margin to account for action bar and status bar
            params.topMargin = actionBarHeight + statusBarHeight + 16; // 16dp additional padding
            view.setLayoutParams(params);
        }
    }
    
    /**
     * Apply proper top padding to a view to account for action bar
     */
    public static void applyActionBarPadding(View view, Context context) {
        int actionBarHeight = getActionBarHeight(context);
        int statusBarHeight = getStatusBarHeight(context);
        
        // Add padding to account for action bar and status bar
        int topPadding = actionBarHeight + statusBarHeight + 16; // 16dp additional padding
        view.setPadding(view.getPaddingLeft(), topPadding, view.getPaddingRight(), view.getPaddingBottom());
    }
    
    /**
     * Ensure an activity has proper content positioning
     */
    public static void setupContentPositioning(AppCompatActivity activity) {
        View contentView = activity.findViewById(android.R.id.content);
        if (contentView instanceof ViewGroup) {
            ViewGroup contentGroup = (ViewGroup) contentView;
            if (contentGroup.getChildCount() > 0) {
                View rootChild = contentGroup.getChildAt(0);

                int actionBarHeight = getActionBarHeight(activity);
                // Add approximately one line height of padding (16dp)
                int extraPaddingPx = Math.round(TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 8,
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
    
    /**
     * Apply consistent top margin to a specific view
     */
    public static void applyConsistentTopMargin(View view, Context context) {
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            int actionBarHeight = getActionBarHeight(context);
            int statusBarHeight = getStatusBarHeight(context);
            int totalTopSpace = actionBarHeight + statusBarHeight + 24; // 24dp additional spacing
            
            params.topMargin = totalTopSpace;
            view.setLayoutParams(params);
        }
    }
    
    /**
     * Get the recommended top margin for consistent spacing
     */
    public static int getRecommendedTopMargin(Context context) {
        int actionBarHeight = getActionBarHeight(context);
        int statusBarHeight = getStatusBarHeight(context);
        return actionBarHeight + statusBarHeight + 24; // 24dp additional spacing
    }
}
