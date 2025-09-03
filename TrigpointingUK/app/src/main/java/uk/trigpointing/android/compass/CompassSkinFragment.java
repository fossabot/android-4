package uk.trigpointing.android.compass;

import androidx.fragment.app.Fragment;

/**
 * Base class for different compass visualization "skins".
 * Each skin receives the same compass data but renders it differently.
 */
public abstract class CompassSkinFragment extends Fragment {
    
    /**
     * Called when new compass data is available.
     * Implementations should update their UI based on this data.
     */
    public abstract void updateCompassData(CompassData data);
    
    /**
     * Returns the display name of this compass skin.
     */
    public abstract String getSkinName();
    
    /**
     * Called when the fragment becomes visible to the user.
     * Override to perform any skin-specific setup.
     */
    public void onSkinVisible() {
        // Default implementation does nothing
    }
    
    /**
     * Called when the fragment is no longer visible to the user.
     * Override to perform any skin-specific cleanup.
     */
    public void onSkinHidden() {
        // Default implementation does nothing
    }
}
