package com.trigpointinguk.android.trigdetails;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

import com.trigpointinguk.android.R;
import com.trigpointinguk.android.common.LazyImageLoader;

public class TrigDetailsOSMapAdapter extends BaseAdapter {
    
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
        
        // Add logging to debug map loading
        Log.d("TrigDetailsOSMapAdapter", "Loading map image at position " + position + ": " + mUrls[position]);
        
        // Check if it's a file path or URL
        if (mUrls[position].startsWith("/") || mUrls[position].startsWith("file://")) {
            // Load from local file
            String filePath = mUrls[position].startsWith("file://") ? 
                mUrls[position].substring(7) : mUrls[position];
            imageLoader.DisplayImage("file://" + filePath, imageView);
        } else {
            // Load from URL (legacy support)
            imageLoader.DisplayImage(mUrls[position], imageView);
        }

        imageView.setLayoutParams(new Gallery.LayoutParams(300, 300));
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        imageView.setBackgroundResource(mGalleryItemBackground);

        return imageView;
    }
}