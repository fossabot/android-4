package com.trigpointinguk.android.trigdetails;

import java.util.ArrayList;

import android.app.ListActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.trigpointinguk.android.DbHelper;
import com.trigpointinguk.android.R;
import com.trigpointinguk.android.common.StringLoader;
import com.trigpointinguk.android.types.Condition;
import com.trigpointinguk.android.types.TrigLog;

public class TrigDetailsLoglistTab extends ListActivity {
	private static final String TAG="TrigDetailsLoglistTab";
	
	private long 						mTrigId;
	private StringLoader 				mStrLoader;
    private ArrayList<TrigLog> 			mTrigLogs;
    private TrigDetailsLoglistAdapter 	mTrigLogsAdapter;
	private TextView 					mEmptyView; 


	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.triglogs);
        
        // get trig_id from extras
        Bundle extras = getIntent().getExtras();
		if (extras == null) {return;}
		mTrigId = extras.getLong(DbHelper.TRIG_ID);
		Log.i(TAG, "Trig_id = "+mTrigId);

        // attach the array adapter
		mTrigLogs = new ArrayList<TrigLog>();
		mTrigLogsAdapter = new TrigDetailsLoglistAdapter(TrigDetailsLoglistTab.this, R.layout.triglogrow, mTrigLogs);
		setListAdapter(mTrigLogsAdapter);

		// find view for empty list notification
		mEmptyView = (TextView) findViewById(android.R.id.empty);
		
		// get list of logs
        populateLogs(false);
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
			populateLogs(true);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}




	private void populateLogs(boolean refresh) {
		// Show loading message on main thread
		mEmptyView.setText(R.string.downloadingLogs);
		// create string loader class
		mStrLoader = new StringLoader(TrigDetailsLoglistTab.this);
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Handler mainHandler = new Handler(Looper.getMainLooper());
		
		CompletableFuture.supplyAsync(() -> {
			int count=0;
			mTrigLogs.clear();
			
			// get triglogs from T:UK, refresh if requested
			String url = String.format("https://trigpointing.uk/trigs/down-android-triglogs.php?t=%d", mTrigId);
			String list = mStrLoader.getString(url, refresh);
			if (list == null || list.trim().length()==0) {
				Log.i(TAG, "No logs for "+mTrigId);        	
				return count;
			}
			//Log.d(TAG,list);        	

			TrigLog tl;

			String[] lines = list.split("\n");
			//System.out.println(java.util.Arrays.toString(lines));
			Log.i(TAG, "Logs found : "+lines.length);
			
			for (String line : lines) {
				if (!(line.trim().equals(""))) { 
					String[] csv = line.split("\t");
					//System.out.println(java.util.Arrays.toString(csv));
					try {
						tl= new TrigLog(
								csv[3],							//username 
								csv[0], 						//date
								Condition.fromCode(csv[2]), 	//condition
								csv[1]);						//text
						mTrigLogs.add(tl);
						count++;
					} catch (Exception e) {
						System.out.println(e);
					}
				}
			}
			return count;
		}, executor)
		.thenAcceptAsync(count -> {
			mEmptyView.setText(R.string.noLogs);
			mTrigLogsAdapter.notifyDataSetChanged();
		}, mainHandler::post);
	}

    
}