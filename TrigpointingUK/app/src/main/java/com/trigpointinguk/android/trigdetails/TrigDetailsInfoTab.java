package com.trigpointinguk.android.trigdetails;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.trigpointinguk.android.DbHelper;
import com.trigpointinguk.android.R;
import com.trigpointinguk.android.mapping.MapActivity;
import com.trigpointinguk.android.types.Condition;
import com.trigpointinguk.android.types.LatLon;
import com.trigpointinguk.android.types.Trig;

public class TrigDetailsInfoTab extends Fragment {
	private static final String TAG="TrigDetailsInfoTab";
	private long     mTrigId;
	private DbHelper mDb;
	private Uri 	 mTUKUrl;
	private Uri 	 mNavUrl;
	private double   mLatitude;
	private double   mLongitude;
	private String   mWaypoint;
	private CheckBox mMark;
    private SharedPreferences mPrefs;
    private View mView;

	
	private static final int RADAR = 1;
	
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.triginfo, container, false);
	}
	
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		mView = view;
		
		// get trig_id from arguments
		Bundle args = getArguments();
		if (args == null) {return;}
		mTrigId = args.getLong(DbHelper.TRIG_ID);
		Log.i("TrigInfo", "Trig_id = "+mTrigId);

		// get application preferences
		mPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

		mMark = (CheckBox) view.findViewById(R.id.mark);
		mMark.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				try {
					mDb.setMarkedTrig(mTrigId, isChecked);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
    }


	@Override
	public void onResume() {
		super.onResume();
		populateFields();
	}

	
		private void populateFields() {
		// get trig info from database
		mDb = new DbHelper(requireContext());
		mDb.open();		
		Cursor c = mDb.fetchTrigInfo(mTrigId);
		c.moveToFirst();

		mLatitude  = c.getDouble(c.getColumnIndex(DbHelper.TRIG_LAT));
		mLongitude = c.getDouble(c.getColumnIndex(DbHelper.TRIG_LON));
		
		mTUKUrl   = Uri.parse( "https://trigpointing.uk/trigs/trig-details.php?t="+c.getLong(c.getColumnIndex(DbHelper.TRIG_ID)) );
		mNavUrl   = Uri.parse( String.format("google.navigation:ll=%3.5f,%3.5f",mLatitude, mLongitude)); 
		mWaypoint = String.format("TP%04d", c.getLong(c.getColumnIndex(DbHelper.TRIG_ID)));
		
		TextView tv;
		ImageView iv;
		
		tv = (TextView)  mView.findViewById(R.id.triginfo_name);
		tv.setText(c.getString(c.getColumnIndex(DbHelper.TRIG_NAME)));

		tv = (TextView)  mView.findViewById(R.id.triginfo_waypoint);
		tv.setText(mWaypoint);
		
		iv = (ImageView) mView.findViewById(R.id.triginfo_condition_icon);
		iv.setImageResource(Condition.fromCode(c.getString(c.getColumnIndex(DbHelper.TRIG_CONDITION))).icon());

		tv = (TextView) mView.findViewById(R.id.triginfo_condition);
		tv.setText(Condition.fromCode(c.getString(c.getColumnIndex(DbHelper.TRIG_CONDITION))).toString());

		LatLon ll = new LatLon(c.getDouble(c.getColumnIndex(DbHelper.TRIG_LAT)), c.getDouble(c.getColumnIndex(DbHelper.TRIG_LON)));

		tv = (TextView)  mView.findViewById(R.id.triginfo_gridref);
		tv.setText(ll.getOSGB10());
		
		tv = (TextView)  mView.findViewById(R.id.triginfo_wgs84);
		tv.setText(ll.getWGS());
		
		tv = (TextView) mView.findViewById(R.id.triginfo_current);
		tv.setText(Trig.Current.fromCode(c.getString(c.getColumnIndex(DbHelper.TRIG_CURRENT))).toString());

		tv = (TextView) mView.findViewById(R.id.triginfo_historic);
		tv.setText(Trig.Historic.fromCode(c.getString(c.getColumnIndex(DbHelper.TRIG_HISTORIC))).toString());

		tv = (TextView) mView.findViewById(R.id.triginfo_type);
		tv.setText(Trig.Physical.fromCode(c.getString(c.getColumnIndex(DbHelper.TRIG_TYPE))).toString());

		tv = (TextView) mView.findViewById(R.id.triginfo_fb);
		tv.setText(c.getString(c.getColumnIndex(DbHelper.TRIG_FB)));

		c.close();

		mMark.setChecked(mDb.isMarkedTrig(mTrigId));
		
		
	}
	
	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		inflater.inflate(R.menu.trigdetailsinfomenu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}


	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		int itemId = item.getItemId();
		
		if (itemId == R.id.directions) {
			Log.i(TAG, "directions");
			try {
				startActivity( new Intent( Intent.ACTION_VIEW, mNavUrl ) );
			} catch (ActivityNotFoundException e) {
				Toast.makeText(requireContext(), "Unable to launch navigator", Toast.LENGTH_LONG).show();
			} 
			return true;
		} else if (itemId == R.id.browser) {
			Log.i(TAG, "browser");
			try {
				startActivity( new Intent( Intent.ACTION_VIEW, mTUKUrl ) );
			} catch (ActivityNotFoundException e) {
				Toast.makeText(requireContext(), "Unable to launch browser", Toast.LENGTH_LONG).show();
			}
			return true;
		} else if (itemId == R.id.radar) {
			Log.i(TAG, "radar");
			try {
				Intent i = new Intent("com.google.android.radar.SHOW_RADAR") ;
				i.putExtra("latitude",  (float) mLatitude);
				i.putExtra("longitude", (float) mLongitude);
				i.putExtra("name", mWaypoint);
				startActivity(i);
			} catch (ActivityNotFoundException e) {
				showDialog(RADAR);
			} 
			return true;	     
		} else if (itemId == R.id.map) {
			Log.i(TAG, "map");
			// remove existing map bounding box preferences
			Editor editor = mPrefs.edit();
			editor.remove("north");
			editor.remove("south");
			editor.remove("east");
			editor.remove("west");
			editor.putInt("zoomLevel", 12);
			editor.putInt("latitude", (int)(mLatitude * 1E6));
			editor.putInt("longitude", (int)(mLongitude * 1E6));
			editor.apply();
			Intent i = new Intent(TrigDetailsInfoTab.this, MapActivity.class);
			startActivityForResult(i, R.id.map);
			return true;	     
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		populateFields();
		
	}

	protected Dialog onCreateDialog(int id) {
	    Dialog dialog;
	    switch(id) {
	    case RADAR:
			dialog = new Dialog(this);
			dialog.setContentView(R.layout.radardialog);
			dialog.setTitle(R.string.radartitle);
			Button yes = (Button) dialog.findViewById(R.id.yes);
			yes.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dismissDialog(RADAR);
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse("market://details?id=com.eclipsim.gpsstatus2"));
					startActivity(intent);
				}
			});
			Button no = (Button) dialog.findViewById(R.id.no);
			no.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dismissDialog(RADAR);
				}
			});
	        break;
	    default:
	        dialog = null;
	    }
	    return dialog;
	}
	
	
	@Override
	protected void onDestroy() {
		if (mDb != null) {
			mDb.close();
		}
		super.onDestroy();
	}


	
}