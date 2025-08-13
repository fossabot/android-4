package com.trigpointinguk.android.logging;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import com.trigpointinguk.android.R;
import com.trigpointinguk.android.common.LazyImageLoader;
import com.trigpointinguk.android.types.TrigPhoto;

public class LogTrigRecyclerAdapter extends RecyclerView.Adapter<LogTrigRecyclerAdapter.ViewHolder> {
    
    private TrigPhoto[] mPhotos;
    private Context mContext;
    private int mGalleryItemBackground;
    private LazyImageLoader imageLoader;
    
    public LogTrigRecyclerAdapter(Context context, TrigPhoto[] photos) {
        mContext = context;
        mPhotos = photos;
        imageLoader = new LazyImageLoader(context);
        
        // Get the gallery item background
        android.content.res.TypedArray attr = mContext.obtainStyledAttributes(R.styleable.TrigpointingUK);
        mGalleryItemBackground = attr.getResourceId(R.styleable.TrigpointingUK_android_galleryItemBackground, 0);
        attr.recycle();
    }
    
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(mContext);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(200, 200));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackgroundResource(mGalleryItemBackground);
        return new ViewHolder(imageView);
    }
    
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TrigPhoto photo = mPhotos[position];
        if (photo != null && photo.getIconURL() != null) {
            imageLoader.DisplayImage(photo.getIconURL(), holder.imageView);
        }
    }
    
    @Override
    public int getItemCount() {
        return mPhotos != null ? mPhotos.length : 0;
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        
        public ViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView;
        }
    }
}
