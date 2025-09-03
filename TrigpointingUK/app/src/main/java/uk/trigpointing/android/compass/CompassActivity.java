package uk.trigpointing.android.compass;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

import uk.trigpointing.android.DbHelper;
import uk.trigpointing.android.R;
import uk.trigpointing.android.common.BaseActivity;
import uk.trigpointing.android.compass.skins.CompassRoseSkinFragment;
import uk.trigpointing.android.compass.skins.GreyPointerCompassFragment;
import uk.trigpointing.android.compass.skins.RedPointerCompassFragment;

public class CompassActivity extends BaseActivity implements CompassDataManager.CompassDataListener {
    
    private static final int REQ_LOCATION = 1001;
    private static final String PREFS_NAME = "compass_prefs";
    private static final String KEY_LAST_SKIN_INDEX = "last_skin_index";
    
    private ViewPager2 viewPager;
    private CompassSkinAdapter adapter;
    private CompassDataManager compassDataManager;
    private LinearLayout pageIndicators;
    private TextView skinNameText;
    
    private double targetLat;
    private double targetLon;
    private List<CompassSkinFragment> skins;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Get target coordinates from intent
        targetLat = getIntent().getDoubleExtra(DbHelper.TRIG_LAT, 0);
        targetLon = getIntent().getDoubleExtra(DbHelper.TRIG_LON, 0);
        
        // Initialize views
        viewPager = findViewById(R.id.compass_viewpager);
        pageIndicators = findViewById(R.id.page_indicators);
        skinNameText = findViewById(R.id.skin_name_text);
        
        // Initialize compass data manager
        compassDataManager = new CompassDataManager(this, this);
        compassDataManager.setTarget(targetLat, targetLon);
        
        // Create compass skins
        setupCompassSkins();
        
        // Setup ViewPager2
        adapter = new CompassSkinAdapter(this, skins);
        viewPager.setAdapter(adapter);
        
        // Setup page indicators
        setupPageIndicators();
        
        // Setup ViewPager2 callbacks
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                int actualSkinIndex = adapter.getActualSkinIndex(position);
                updatePageIndicators(actualSkinIndex);
                updateSkinNameDisplay(actualSkinIndex);
                notifySkinsOfVisibilityChange(actualSkinIndex);
                
                // Save the last used skin index
                saveLastSkinIndex(actualSkinIndex);
            }
        });
        
        // Initialize with last used skin or first skin
        int lastSkinIndex = getLastSkinIndex();
        int initialViewPagerPosition = adapter.getViewPagerPosition(lastSkinIndex);
        viewPager.setCurrentItem(initialViewPagerPosition, false);
        
        if (!skins.isEmpty()) {
            updateSkinNameDisplay(lastSkinIndex);
            skins.get(lastSkinIndex).onSkinVisible();
        }
    }
    
    private void setupCompassSkins() {
        skins = new ArrayList<>();
        skins.add(new GreyPointerCompassFragment());
        skins.add(new RedPointerCompassFragment());
        skins.add(new CompassRoseSkinFragment());
    }
    
    private void setupPageIndicators() {
        pageIndicators.removeAllViews();
        
        for (int i = 0; i < skins.size(); i++) {
            View indicator = createIndicatorDot(i == 0);
            pageIndicators.addView(indicator);
        }
    }
    
    private View createIndicatorDot(boolean isActive) {
        View dot = new View(this);
        int size = (int) (8 * getResources().getDisplayMetrics().density); // 8dp
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMargins(4, 0, 4, 0);
        dot.setLayoutParams(params);
        
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(isActive ? 0xFFFFFFFF : 0x80FFFFFF);
        dot.setBackground(drawable);
        
        return dot;
    }
    
    private void updatePageIndicators(int activePosition) {
        for (int i = 0; i < pageIndicators.getChildCount(); i++) {
            View indicator = pageIndicators.getChildAt(i);
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(i == activePosition ? 0xFFFFFFFF : 0x80FFFFFF);
            indicator.setBackground(drawable);
        }
    }
    
    private void updateSkinNameDisplay(int position) {
        CompassSkinFragment skin = adapter.getSkin(position);
        if (skin != null) {
            skinNameText.setText(skin.getSkinName());
        }
    }
    
    private void notifySkinsOfVisibilityChange(int newPosition) {
        // Notify all skins of visibility changes
        for (int i = 0; i < skins.size(); i++) {
            if (i == newPosition) {
                skins.get(i).onSkinVisible();
            } else {
                skins.get(i).onSkinHidden();
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        compassDataManager.startUpdates();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        compassDataManager.stopUpdates();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    
    @Override
    public void onCompassDataUpdated(CompassData data) {
        // Update the currently visible compass skin with new data
        int currentPosition = viewPager.getCurrentItem();
        CompassSkinFragment currentFragment = adapter.getFragmentInstance(currentPosition);
        if (currentFragment != null) {
            currentFragment.updateCompassData(data);
        }
    }
    
    @Override
    public void onLocationPermissionRequired() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                REQ_LOCATION);
    }
    
    @Override
    public void onLocationPermissionDenied() {
        Toast.makeText(this, "Location permission denied", Toast.LENGTH_LONG).show();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            boolean granted = false;
            if (grantResults != null && grantResults.length > 0) {
                for (int res : grantResults) {
                    if (res == PackageManager.PERMISSION_GRANTED) {
                        granted = true;
                        break;
                    }
                }
            }
            if (granted) {
                compassDataManager.startUpdates();
            } else {
                onLocationPermissionDenied();
            }
        }
    }
    
    /**
     * Save the last used skin index to SharedPreferences
     */
    private void saveLastSkinIndex(int skinIndex) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_LAST_SKIN_INDEX, skinIndex).apply();
    }
    
    /**
     * Get the last used skin index from SharedPreferences
     */
    private int getLastSkinIndex() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int lastIndex = prefs.getInt(KEY_LAST_SKIN_INDEX, 0);
        // Ensure the index is valid
        if (lastIndex < 0 || lastIndex >= skins.size()) {
            return 0;
        }
        return lastIndex;
    }
}
