package uk.trigpointing.android.compass;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

/**
 * Adapter for ViewPager2 to manage different compass skin fragments
 */
public class CompassSkinAdapter extends FragmentStateAdapter {
    private final List<CompassSkinFragment> skins;
    
    public CompassSkinAdapter(@NonNull FragmentActivity fragmentActivity, List<CompassSkinFragment> skins) {
        super(fragmentActivity);
        this.skins = skins;
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return skins.get(position);
    }
    
    @Override
    public int getItemCount() {
        return skins.size();
    }
    
    public CompassSkinFragment getSkin(int position) {
        if (position >= 0 && position < skins.size()) {
            return skins.get(position);
        }
        return null;
    }
}
