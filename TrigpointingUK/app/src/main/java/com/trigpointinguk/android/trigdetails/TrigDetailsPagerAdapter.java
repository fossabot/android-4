package com.trigpointinguk.android.trigdetails;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.trigpointinguk.android.logging.LogTrigActivity;

public class TrigDetailsPagerAdapter extends FragmentStateAdapter {
    
    private final Bundle extras;
    private final Context context;
    
    public TrigDetailsPagerAdapter(@NonNull FragmentActivity fragmentActivity, Bundle extras) {
        super(fragmentActivity);
        this.extras = extras;
        this.context = fragmentActivity;
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        
        switch (position) {
            case 0:
                fragment = new TrigDetailsInfoTab();
                break;
            case 1:
                fragment = new TrigDetailsLoglistTab();
                break;
            case 2:
                fragment = new TrigDetailsAlbumTab();
                break;
            case 3:
                fragment = new TrigDetailsOSMapTab();
                break;
            case 4:
                fragment = new LogTrigActivity();
                break;
            default:
                fragment = new TrigDetailsInfoTab();
                break;
        }
        
        if (extras != null) {
            fragment.setArguments(extras);
        }
        
        return fragment;
    }
    
    @Override
    public int getItemCount() {
        return 5; // info, logs, album, map, mylog
    }
} 