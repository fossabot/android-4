package uk.trigpointing.android.trigdetails;

import java.util.ArrayList;
import java.util.Locale;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;
import android.widget.TextView;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import uk.trigpointing.android.DbHelper;
import uk.trigpointing.android.R;
import uk.trigpointing.android.common.BaseTabActivity;
import uk.trigpointing.android.common.DisplayBitmapActivity;
import uk.trigpointing.android.common.StringLoader;
import uk.trigpointing.android.types.TrigPhoto;

public class TrigDetailsAlbumTab extends BaseTabActivity {
	private long mTrigId;
	private static final String TAG="TrigDetailsAlbumTab";
	private StringLoader 			mStrLoader;
    private ArrayList<TrigPhoto> 	mTrigPhotos;
    private TrigDetailsAlbumGridAdapter mGridAdapter;
	private TextView 				mEmptyView;

    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trigalbum);
        
        // get trig_id from extras
        Bundle extras = getIntent().getExtras();
		if (extras == null) {return;}
		mTrigId = extras.getLong(DbHelper.TRIG_ID);
		Log.i(TAG, "Trig_id = "+mTrigId);

        // Set up grid RecyclerView similar to OS Map tab
		RecyclerView recycler = findViewById(R.id.trigalbum_recycler);
		mEmptyView = findViewById(android.R.id.empty);
		mTrigPhotos = new ArrayList<TrigPhoto>();
		mGridAdapter = new TrigDetailsAlbumGridAdapter(this, mTrigPhotos);
		
		int screenWidth = getResources().getDisplayMetrics().widthPixels;
		int columns = Math.max(2, Math.min(3, screenWidth / 500));
		recycler.setLayoutManager(new GridLayoutManager(this, columns));
		recycler.setNestedScrollingEnabled(false);
		recycler.setAdapter(mGridAdapter);
		int spacingPx = dpToPx(16);
		recycler.addItemDecoration(new GridSpacingItemDecoration(columns, spacingPx, false));

		mGridAdapter.setOnItemClickListener(new TrigDetailsAlbumGridAdapter.OnItemClickListener() {
			@Override
			public void onItemClick(int position) {
				String url = mTrigPhotos.get(position).getPhotoURL();
				Intent i = new Intent(TrigDetailsAlbumTab.this, DisplayBitmapActivity.class);
				i.putExtra("URL", url);
				Log.i(TAG, "Clicked photo at URL: " + url);
				startActivity(i);
			}
		});

		// get list of photos
        populatePhotos(false);	    
    }
    
    
    protected void onListItemClick(android.widget.ListView l, View v, int position, long id) {}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.trigdetailsmenu, menu);
		return result;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		
		if (itemId == R.id.refresh) {
			Log.i(TAG, "refresh");
			refreshAlbumFromParent();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

    
    
    
	private void populatePhotos(boolean refresh) {
		// Show loading message on main thread
		mEmptyView.setText(R.string.downloadingPhotos);
		// create string loader class
		mStrLoader = new StringLoader(TrigDetailsAlbumTab.this);
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Handler mainHandler = new Handler(Looper.getMainLooper());
		
		CompletableFuture.<ArrayList<TrigPhoto>>supplyAsync(() -> {
			ArrayList<TrigPhoto> results = new ArrayList<TrigPhoto>();
			String url = String.format(Locale.getDefault(), "https://trigpointing.uk/trigs/down-android-trigphotos.php?t=%d", mTrigId);
			String list = mStrLoader.getString(url, refresh);
			if (list == null || list.trim().length()==0) {
				Log.i(TAG, "No photos for "+mTrigId);            
				return results;
			}

			TrigPhoto tp;

			String[] lines = list.split("\n");
			Log.i(TAG, "Photos found : "+lines.length);
			
			for (String line : lines) {
				if (!(line.trim().equals(""))) { 
					String[] csv = line.split("\t");
					try {
						tp = new TrigPhoto(
								csv[2],		//name 
								csv[3], 	//descr
								csv[1],		//photo
								csv[0],		//icon
								csv[5],		//user
								csv[4]);	//date
						Log.d(TAG, "Photo URL: " + csv[1] + ", Icon URL: " + csv[0]);
						results.add(tp);
					} catch (Exception e) {
						Log.e(TAG, "Error parsing photo line: " + line, e);
					}
				}
			}
			return results;
		}, executor)
		.thenAcceptAsync(results -> {
			int count = results != null ? results.size() : 0;
			if (count == 0) {
				mEmptyView.setVisibility(View.VISIBLE);
				mEmptyView.setText(R.string.noPhotos);
			} else {
				mEmptyView.setVisibility(View.GONE);
			}
			// Mutate adapter list only on main thread to avoid RecyclerView inconsistencies
			mTrigPhotos.clear();
			if (results != null) { mTrigPhotos.addAll(results); }
			mGridAdapter.notifyDataSetChanged();
		}, mainHandler::post);
	}

	public void refreshAlbumFromParent() {
		try {
			if (mGridAdapter != null) {
				mGridAdapter.clearImageCaches();
			}
		} catch (Exception e) {
			Log.w(TAG, "Failed to clear image cache", e);
		}

		// Recreate adapter to clear any in-memory caches
		RecyclerView recycler = findViewById(R.id.trigalbum_recycler);
		if (recycler != null) {
			mGridAdapter = new TrigDetailsAlbumGridAdapter(this, mTrigPhotos);
			recycler.setAdapter(mGridAdapter);
			mGridAdapter.setOnItemClickListener(new TrigDetailsAlbumGridAdapter.OnItemClickListener() {
				@Override
				public void onItemClick(int position) {
					String url = mTrigPhotos.get(position).getPhotoURL();
					Intent i = new Intent(TrigDetailsAlbumTab.this, DisplayBitmapActivity.class);
					i.putExtra("URL", url);
					Log.i(TAG, "Clicked photo at URL: " + url);
					startActivity(i);
				}
			});
		}

		// Force reload of photo list and thumbnails
		populatePhotos(true);
	}



	private int dpToPx(int dp) {
		float density = getResources().getDisplayMetrics().density;
		return Math.round(dp * density);
	}

	// Even grid spacing decoration similar to OS Map tab
	private static class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
		private final int spanCount;
		private final int spacing;
		private final boolean includeEdge;

		GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
			this.spanCount = spanCount;
			this.spacing = spacing;
			this.includeEdge = includeEdge;
		}

		@Override
		public void getItemOffsets(android.graphics.Rect outRect, android.view.View view,
				RecyclerView parent, RecyclerView.State state) {
			int position = parent.getChildAdapterPosition(view);
			int column = position % spanCount;
			if (includeEdge) {
				outRect.left = spacing - column * spacing / spanCount;
				outRect.right = (column + 1) * spacing / spanCount;
				if (position < spanCount) {
					outRect.top = spacing;
				}
				outRect.bottom = spacing;
			} else {
				outRect.left = column * spacing / spanCount;
				outRect.right = spacing - (column + 1) * spacing / spanCount;
				if (position >= spanCount) {
					outRect.top = spacing;
				}
			}
		}
	}

    
}