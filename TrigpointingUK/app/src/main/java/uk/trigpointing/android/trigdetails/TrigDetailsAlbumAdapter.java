package uk.trigpointing.android.trigdetails;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import uk.trigpointing.android.R;
import uk.trigpointing.android.common.LazyImageLoader;
import uk.trigpointing.android.types.TrigPhoto;

public class TrigDetailsAlbumAdapter extends ArrayAdapter<TrigPhoto> {
	private final ArrayList<TrigPhoto> mPhotos;
	private final Context 			 mContext;
    public  LazyImageLoader      imageLoader;
	
	public TrigDetailsAlbumAdapter(Context context, int rowResourceId, ArrayList<TrigPhoto> photos) {
		super(context, rowResourceId, photos);
    	mContext = context;
        mPhotos=photos;
        imageLoader=new LazyImageLoader(mContext);
	}

    public int getCount() {
        return mPhotos.size();
    }
    
	@Override
	public TrigPhoto getItem(int position) {
		return mPhotos.get(position);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row;
		 
		if (null == convertView) {
			row = LayoutInflater.from(mContext).inflate(R.layout.trigalbumrow, parent, false);
		} else {
			row = convertView;
		}
		 		
		TextView  tn = row.findViewById(R.id.photoName);
		TextView  td = row.findViewById(R.id.photoDescr);
		TextView  tu = row.findViewById(R.id.photoDateUser);
		ImageView ta = row.findViewById(R.id.photoIcon);

		TrigPhoto tp = getItem(position);
		tn.setText(tp.getName());
		if (tp.getDescr().equals(tp.getName())) {
			td.setText("");
		} else {
			td.setText(tp.getDescr());
		}
				
		tu.setText(mContext.getString(R.string.date_user_format, tp.getDate(), tp.getUsername()));
		imageLoader.DisplayImage(tp.getIconURL(), ta);
		return row;
	}

	
}
