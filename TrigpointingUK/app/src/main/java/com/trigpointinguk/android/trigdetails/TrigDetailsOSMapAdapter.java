package com.trigpointinguk.android.trigdetails;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import java.util.Arrays;

import com.trigpointinguk.android.R;
import com.trigpointinguk.android.common.LazyImageLoader;

public class TrigDetailsOSMapAdapter extends BaseAdapter {
    private static final String TAG = "TrigDetailsOSMapAdapter";
    private static final String PLACEHOLDER_URL = "PLACEHOLDER";
    
    private String[] mUrls;
    public LazyImageLoader imageLoader;
	private int mGalleryItemBackground; 
	private Context mContext;
    
    public TrigDetailsOSMapAdapter(Context context, String[] urls) {
        mUrls=urls;
        mContext=context;
        imageLoader=new LazyImageLoader(context);
        
        TypedArray attr = context.obtainStyledAttributes(R.styleable.TrigpointingUK);
        mGalleryItemBackground = attr.getResourceId(R.styleable.TrigpointingUK_android_galleryItemBackground, 0);
    }

    public int getCount() {
        return mUrls.length;
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }
    
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView = new ImageView(mContext);
        
        Log.d(TAG, "Loading map image at position " + position + ": " + mUrls[position]);
        
        // Handle placeholder items
        if (PLACEHOLDER_URL.equals(mUrls[position])) {
            imageView.setImageResource(R.drawable.imageloading);
            Log.d(TAG, "Showing placeholder at position " + position);
        }
        // Check if it's a file path or URL
        else if (mUrls[position].startsWith("/") || mUrls[position].startsWith("file://")) {
            // Load from local file directly (bypass LazyImageLoader for local files)
            String filePath = mUrls[position].startsWith("file://") ? 
                mUrls[position].substring(7) : mUrls[position];
            
            try {
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(filePath);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                    Log.d("TrigDetailsOSMapAdapter", "Successfully loaded local file: " + filePath);
                } else {
                    Log.w("TrigDetailsOSMapAdapter", "Failed to decode local file: " + filePath);
                    // Set a placeholder or default image
                    imageView.setImageResource(R.drawable.imageloading);
                }
            } catch (Exception e) {
                Log.e("TrigDetailsOSMapAdapter", "Error loading local file: " + filePath, e);
                imageView.setImageResource(R.drawable.imageloading);
            }
        } else {
            // Load from URL using LazyImageLoader (legacy support)
            imageLoader.DisplayImage(mUrls[position], imageView);
        }

        imageView.setLayoutParams(new Gallery.LayoutParams(300, 300));
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        imageView.setBackgroundResource(mGalleryItemBackground);

        return imageView;
    }
    
    /**
     * Update a specific position with a new URL (for progressive loading)
     */
    public void updateImageAtPosition(int position, String imagePath) {
        if (position >= 0 && position < mUrls.length) {
            mUrls[position] = imagePath;
            Log.d(TAG, "Updated position " + position + " with: " + imagePath);
            // Notify adapter of change - this will trigger getView() for this position
            notifyDataSetChanged();
        }
    }
    
    /**
     * Get the number of placeholder items still pending
     */
    public int getPendingCount() {
        int count = 0;
        for (String url : mUrls) {
            if (PLACEHOLDER_URL.equals(url)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Create an adapter with all placeholder items initially
     */
    public static TrigDetailsOSMapAdapter createWithPlaceholders(Context context, int expectedCount) {
        String[] placeholders = new String[expectedCount];
        Arrays.fill(placeholders, PLACEHOLDER_URL);
        return new TrigDetailsOSMapAdapter(context, placeholders);
    }
    
    /**
     * Get the URL at a specific position
     */
    public String getUrlAtPosition(int position) {
        if (position >= 0 && position < mUrls.length) {
            return mUrls[position];
        }
        return null;
    }
}