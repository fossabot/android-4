package uk.trigpointing.android.common;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.appbar.AppBarLayout;

/**
 * Base activity for activities that are embedded as tabs within another activity.
 * This is similar to BaseActivity but without action bar handling since tab activities
 * are embedded within a parent activity that already has an action bar.
 * 
 * This ensures content positioning works correctly without creating duplicate action bars.
 */
public abstract class BaseTabActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Explicitly hide the action bar for tab activities
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }
    
    @Override
    public void setContentView(int layoutResID) {
        // For tab activities, we don't need the CoordinatorLayout wrapper
        // since they're embedded within a parent activity that already handles
        // the action bar spacing. Just use the layout directly.
        super.setContentView(layoutResID);
    }
    
    @Override
    public void setContentView(View view) {
        // For tab activities, use the view directly without wrapping
        super.setContentView(view);
    }
}
