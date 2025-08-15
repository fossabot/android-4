package uk.trigpointing.android.trigdetails;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
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
    private TrigDetailsAlbumAdapter mTrigAlbumAdapter;
	private TextView 				mEmptyView;
	private ListView 				mListView;

    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trigalbum);
        
        // get trig_id from extras
        Bundle extras = getIntent().getExtras();
		if (extras == null) {return;}
		mTrigId = extras.getLong(DbHelper.TRIG_ID);
		Log.i(TAG, "Trig_id = "+mTrigId);

        // Find ListView and set up adapter
		mListView = findViewById(android.R.id.list);
		mTrigPhotos = new ArrayList<TrigPhoto>();
		mTrigAlbumAdapter = new TrigDetailsAlbumAdapter(TrigDetailsAlbumTab.this, R.layout.trigalbumrow, mTrigPhotos);
		mListView.setAdapter(mTrigAlbumAdapter);

		// find view for empty list notification
		mEmptyView = findViewById(android.R.id.empty);
		mListView.setEmptyView(mEmptyView);
		
		// Set up click listener
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				onListItemClick(mListView, view, position, id);
			}
		});

		// get list of photos
        populatePhotos(false);	    
    }
    
    
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String url = mTrigPhotos.get(position).getPhotoURL();
        Intent i = new Intent(this, DisplayBitmapActivity.class);
        i.putExtra("URL", url);
        Log.i(TAG, "Clicked photo at URL: " +url);
        startActivity(i);
    }
	
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
			populatePhotos(true);
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
		
		CompletableFuture.supplyAsync(() -> {
			int count=0;
			mTrigPhotos.clear();
			
			String url = String.format("https://trigpointing.uk/trigs/down-android-trigphotos.php?t=%d", mTrigId);
			String list = mStrLoader.getString(url, refresh);
			if (list == null || list.trim().length()==0) {
				Log.i(TAG, "No photos for "+mTrigId);        	
				return count;
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
						mTrigPhotos.add(tp);
						count++;
					} catch (Exception e) {
						Log.e(TAG, "Error parsing photo line: " + line, e);
					}
				}
			}
			return count;
		}, executor)
		.thenAcceptAsync(count -> {
			mEmptyView.setText(R.string.noPhotos);
			mTrigAlbumAdapter.notifyDataSetChanged();
		}, mainHandler::post);
	}

    
}