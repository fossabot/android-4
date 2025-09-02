package uk.trigpointing.android.logging;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

import uk.trigpointing.android.R;
import uk.trigpointing.android.types.TrigPhoto;

public class LogTrigGalleryAdapter extends BaseAdapter {
    
    private final TrigPhoto[] mPhotos;
    private final int mGalleryItemBackground;
    private final Context mContext;
    
    public LogTrigGalleryAdapter(Context context, TrigPhoto[] photos) {
        mPhotos=photos;
        mContext=context;
 
        TypedArray attr = context.obtainStyledAttributes(R.styleable.TrigpointingUK);
        mGalleryItemBackground = attr.getResourceId(R.styleable.TrigpointingUK_android_galleryItemBackground, 0);
        attr.recycle();
    }

    public int getCount() {
        return mPhotos.length;
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }
    
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView iv;
        if (null == convertView) {
            iv = new ImageView(mContext);
        } else {
            iv = (ImageView) convertView;
        }

        iv.setImageBitmap(BitmapFactory.decodeFile(mPhotos[position].getIconURL()));
        
        iv.setLayoutParams(new Gallery.LayoutParams(200, 200));
        iv.setScaleType(ImageView.ScaleType.FIT_XY);
        iv.setBackgroundResource(mGalleryItemBackground);

        return iv;
    }
}