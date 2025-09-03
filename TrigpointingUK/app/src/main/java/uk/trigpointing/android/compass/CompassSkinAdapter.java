package uk.trigpointing.android.compass;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.trigpointing.android.compass.skins.CompassRoseSkinFragment;
import uk.trigpointing.android.compass.skins.GreyPointerCompassFragment;
import uk.trigpointing.android.compass.skins.RedPointerCompassFragment;

/**
 * Adapter for ViewPager2 to manage different compass skin fragments with wrap-around functionality
 */
public class CompassSkinAdapter extends FragmentStateAdapter {
    private final List<CompassSkinFragment> skins;
    private static final int WRAP_AROUND_MULTIPLIER = 1000; // Large number to enable wrap-around
    private final Map<Integer, CompassSkinFragment> fragmentInstances = new HashMap<>();
    
    public CompassSkinAdapter(@NonNull FragmentActivity fragmentActivity, List<CompassSkinFragment> skins) {
        super(fragmentActivity);
        this.skins = skins;
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Wrap position to actual skin index
        if (skins.isEmpty()) {
            throw new IllegalStateException("No compass skins available");
        }
        int actualPosition = position % skins.size();
        
        // Create a new instance based on the skin type to avoid "Fragment already added" errors
        CompassSkinFragment templateSkin = skins.get(actualPosition);
        CompassSkinFragment newFragment;
        
        if (templateSkin instanceof GreyPointerCompassFragment) {
            newFragment = new GreyPointerCompassFragment();
        } else if (templateSkin instanceof RedPointerCompassFragment) {
            newFragment = new RedPointerCompassFragment();
        } else if (templateSkin instanceof CompassRoseSkinFragment) {
            newFragment = new CompassRoseSkinFragment();
        } else {
            throw new IllegalStateException("Unknown compass skin type: " + templateSkin.getClass().getSimpleName());
        }
        
        // Store the fragment instance for later reference
        fragmentInstances.put(position, newFragment);
        return newFragment;
    }
    
    @Override
    public int getItemCount() {
        return skins.size() * WRAP_AROUND_MULTIPLIER;
    }
    
    public CompassSkinFragment getSkin(int position) {
        if (skins.isEmpty()) return null;
        int actualPosition = position % skins.size();
        if (actualPosition >= 0 && actualPosition < skins.size()) {
            return skins.get(actualPosition);
        }
        return null;
    }
    
    /**
     * Get the actual skin index from a ViewPager position
     */
    public int getActualSkinIndex(int position) {
        if (skins.isEmpty()) return 0;
        return position % skins.size();
    }
    
    /**
     * Get a ViewPager position for a given skin index, starting from the middle of the wrap-around range
     */
    public int getViewPagerPosition(int skinIndex) {
        if (skins.isEmpty()) return 0;
        int middlePosition = (getItemCount() / 2) - (getItemCount() / 2) % skins.size();
        return middlePosition + skinIndex;
    }
    
    /**
     * Get the actual fragment instance at the given position
     */
    public CompassSkinFragment getFragmentInstance(int position) {
        return fragmentInstances.get(position);
    }
}
