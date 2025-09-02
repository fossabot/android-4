package uk.trigpointing.android.trigdetails;

import java.util.ArrayList;
import java.util.Locale;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import uk.trigpointing.android.DbHelper;
import uk.trigpointing.android.R;
import uk.trigpointing.android.common.BaseTabActivity;
import uk.trigpointing.android.common.StringLoader;
import uk.trigpointing.android.types.Condition;
import uk.trigpointing.android.types.TrigLog;

public class TrigDetailsLoglistTab extends BaseTabActivity {
    private static final String TAG="TrigDetailsLoglistTab";
    
    private long                         mTrigId;
    private StringLoader                 mStrLoader;
    private ArrayList<TrigLog>             mTrigLogs;
    private TrigDetailsLoglistAdapter     mTrigLogsAdapter;
    private TextView                     mEmptyView;
    private ListView                     mListView;


    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.triglogs);
        
        // get trig_id from extras
        Bundle extras = getIntent().getExtras();
        if (extras == null) {return;}
        mTrigId = extras.getLong(DbHelper.TRIG_ID);
        Log.i(TAG, "Trig_id = "+mTrigId);

        // Find ListView and set up adapter
        mListView = findViewById(android.R.id.list);
        mTrigLogs = new ArrayList<TrigLog>();
        mTrigLogsAdapter = new TrigDetailsLoglistAdapter(TrigDetailsLoglistTab.this, R.layout.triglogrow, mTrigLogs);
        mListView.setAdapter(mTrigLogsAdapter);

        // find view for empty list notification
        mEmptyView = findViewById(android.R.id.empty);
        mListView.setEmptyView(mEmptyView);
        
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
            String url = String.format(Locale.getDefault(), "https://trigpointing.uk/trigs/down-android-triglogs.php?t=%d", mTrigId);
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
                                csv[3],                            //username 
                                csv[0],                         //date
                                Condition.fromCode(csv[2]),     //condition
                                csv[1]);                        //text
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

    // Allow parent activity to trigger a refresh for the current trigpoint
    public void refreshLogsFromParent() {
        Log.i(TAG, "refreshLogsFromParent");
        populateLogs(true);
    }

    
}