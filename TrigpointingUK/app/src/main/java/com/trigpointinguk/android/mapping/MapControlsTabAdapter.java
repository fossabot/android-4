package com.trigpointinguk.android.mapping;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MapControlsTabAdapter extends FragmentStateAdapter {

    public MapControlsTabAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new MapStyleTabFragment();
            case 1:
                return new MarkerColorTabFragment();
            case 2:
                return new TrigpointTypesTabFragment();
            case 3:
                return new FilterFoundTabFragment();
            default:
                return new MapStyleTabFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
