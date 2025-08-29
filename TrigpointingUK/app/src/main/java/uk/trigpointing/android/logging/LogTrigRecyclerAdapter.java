package uk.trigpointing.android.logging;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.graphics.BitmapFactory;
import java.io.File;
import androidx.recyclerview.widget.RecyclerView;
import uk.trigpointing.android.R;
import uk.trigpointing.android.common.LazyImageLoader;
import uk.trigpointing.android.types.TrigPhoto;

public class LogTrigRecyclerAdapter extends RecyclerView.Adapter<LogTrigRecyclerAdapter.ViewHolder> {
    
    private final TrigPhoto[] mPhotos;
    private final Context mContext;
    private final int mGalleryItemBackground;
    private final LazyImageLoader imageLoader;
    
    public LogTrigRecyclerAdapter(Context context, TrigPhoto[] photos) {
        mContext = context;
        mPhotos = photos;
        imageLoader = new LazyImageLoader(context);
        
        // No background/borders for thumbnails
        mGalleryItemBackground = 0;
    }
    
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(mContext);
        int tileHeightPx = (int) (120 * mContext.getResources().getDisplayMetrics().density); // ~120dp tall
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, tileHeightPx);
        int marginPx = (int) (4 * mContext.getResources().getDisplayMetrics().density); // ~4dp margin
        lp.setMargins(marginPx, marginPx, marginPx, marginPx);
        imageView.setLayoutParams(lp);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setAdjustViewBounds(true);
        if (mGalleryItemBackground != 0) {
            imageView.setBackgroundResource(mGalleryItemBackground);
        }
        return new ViewHolder(imageView);
    }
    
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TrigPhoto photo = mPhotos[position];
        if (photo == null) {return;}
        String iconUrl = photo.getIconURL();
        if (iconUrl == null || iconUrl.trim().isEmpty()) {
            holder.imageView.setImageResource(R.drawable.imageloading);
            return;
        }

        // If this is a local file path, decode directly. Otherwise, use LazyImageLoader.
        File potentialLocalFile = new File(iconUrl);
        if (potentialLocalFile.exists()) {
            holder.imageView.setImageBitmap(BitmapFactory.decodeFile(iconUrl));
            return;
        }

        if (iconUrl.startsWith("file:")) {
            holder.imageView.setImageBitmap(BitmapFactory.decodeFile(iconUrl.replaceFirst("^file:", "")));
            return;
        }

        imageLoader.DisplayImage(iconUrl, holder.imageView);
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
