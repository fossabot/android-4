package uk.trigpointing.android.trigdetails;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import uk.trigpointing.android.R;
import uk.trigpointing.android.common.LazyImageLoader;
import uk.trigpointing.android.types.TrigPhoto;

public class TrigDetailsAlbumGridAdapter extends RecyclerView.Adapter<TrigDetailsAlbumGridAdapter.ViewHolder> {

	public interface OnItemClickListener {
		void onItemClick(int position);
	}

	private final Context mContext;
	private final ArrayList<TrigPhoto> mPhotos;
	private final LazyImageLoader mImageLoader;
	private OnItemClickListener mClickListener;
	private final int mCellImageSizePx;

	public TrigDetailsAlbumGridAdapter(Context context, ArrayList<TrigPhoto> photos) {
		mContext = context;
		mPhotos = photos;
		mImageLoader = new LazyImageLoader(context);
		// Pre-compute target image size to keep images square and large enough
		int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
		int columns = Math.max(2, Math.min(3, screenWidth / 500));
		int totalSidePaddingPx = dpToPx(32); // 16dp padding on both sides
		int interItemSpacingPx = dpToPx(16); // spacing between items
		int totalInterItemWidth = interItemSpacingPx * (columns - 1);
		int availableWidth = screenWidth - totalSidePaddingPx - totalInterItemWidth;
		mCellImageSizePx = availableWidth / columns;
	}

	public void setOnItemClickListener(OnItemClickListener listener) {
		mClickListener = listener;
	}

	public void clearImageCaches() {
		try { mImageLoader.clearCaches(); } catch (Exception ignored) {}
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(mContext).inflate(R.layout.trigalbum_item, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		TrigPhoto tp = mPhotos.get(position);
		holder.name.setText(tp.getName());
		holder.dateUser.setText(tp.getDate() + "   " + tp.getUsername());
		if (tp.getDescr().equals(tp.getName())) {
			holder.descr.setText("");
		} else {
			holder.descr.setText(tp.getDescr());
		}

		// Ensure image view is a square of computed size
		ViewGroup.LayoutParams lp = holder.image.getLayoutParams();
		lp.width = mCellImageSizePx;
		lp.height = mCellImageSizePx;
		holder.image.setLayoutParams(lp);
		holder.image.setScaleType(ImageView.ScaleType.CENTER_CROP);

		mImageLoader.DisplayImage(tp.getIconURL(), holder.image);

		holder.itemView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mClickListener != null) {
					mClickListener.onItemClick(holder.getAdapterPosition());
				}
			}
		});
	}

	@Override
	public int getItemCount() {
		return mPhotos.size();
	}

	static class ViewHolder extends RecyclerView.ViewHolder {
		final TextView name;
		final ImageView image;
		final TextView dateUser;
		final TextView descr;

		ViewHolder(View itemView) {
			super(itemView);
			name = itemView.findViewById(R.id.photoName);
			image = itemView.findViewById(R.id.photoIcon);
			dateUser = itemView.findViewById(R.id.photoDateUser);
			descr = itemView.findViewById(R.id.photoDescr);
		}
	}

	private int dpToPx(int dp) {
		float density = mContext.getResources().getDisplayMetrics().density;
		return Math.round(dp * density);
	}
}


