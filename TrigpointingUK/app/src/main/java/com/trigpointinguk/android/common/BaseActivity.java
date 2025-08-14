package com.trigpointinguk.android.common;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.appbar.AppBarLayout;

/**
 * Base activity that properly handles content positioning below the action bar.
 * All activities should extend this instead of AppCompatActivity directly.
 */
public abstract class BaseActivity extends AppCompatActivity {
    
    @Override
    public void setContentView(int layoutResID) {
        // Inflate the original layout
        View originalView = getLayoutInflater().inflate(layoutResID, null);
        
        // Create a CoordinatorLayout as the root
        CoordinatorLayout coordinatorLayout = new CoordinatorLayout(this);
        coordinatorLayout.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        coordinatorLayout.setFitsSystemWindows(true);
        
        // Create a FrameLayout to hold the content below the action bar
        FrameLayout contentFrame = new FrameLayout(this);
        CoordinatorLayout.LayoutParams contentParams = new CoordinatorLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        
        // This is the key: set the behavior to position below the app bar
        contentParams.setBehavior(new AppBarLayout.ScrollingViewBehavior());
        contentFrame.setLayoutParams(contentParams);
        
        // Add the original view to the content frame
        contentFrame.addView(originalView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        
        // Add the content frame to the coordinator layout
        coordinatorLayout.addView(contentFrame);
        
        // Set the coordinator layout as the content view
        super.setContentView(coordinatorLayout);
    }
    
    @Override
    public void setContentView(View view) {
        // Handle direct view setting
        CoordinatorLayout coordinatorLayout = new CoordinatorLayout(this);
        coordinatorLayout.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        coordinatorLayout.setFitsSystemWindows(true);
        
        FrameLayout contentFrame = new FrameLayout(this);
        CoordinatorLayout.LayoutParams contentParams = new CoordinatorLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        contentParams.setBehavior(new AppBarLayout.ScrollingViewBehavior());
        contentFrame.setLayoutParams(contentParams);
        
        contentFrame.addView(view, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        
        coordinatorLayout.addView(contentFrame);
        super.setContentView(coordinatorLayout);
    }
}
