package uk.trigpointing.android.logging;

import java.io.File;
import java.util.Arrays;

import android.annotation.SuppressLint;
import uk.trigpointing.android.common.BaseActivity;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import uk.trigpointing.android.DbHelper;
import uk.trigpointing.android.R;
import uk.trigpointing.android.types.PhotoSubject;

public class LogPhotoActivity extends BaseActivity {
	
	private static final String TAG			= "LogPhotoActivity";
    
	private Long				mPhotoId;
	
	private String 				mIconURL;
	private String 				mPhotoURL;
	private Long				mTrigId;
    private ImageView			mThumb;
    private EditText			mName;
    private EditText			mDescr;
    private Spinner				mSubject;
    private CheckBox			mIsPublic;
    
    private DbHelper 			mDb;

	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.logphoto);

		// Enable back button in action bar
		if (getSupportActionBar() != null) {
		    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		Bundle extras = getIntent().getExtras();
		mPhotoId = extras.getLong(DbHelper.PHOTO_ID);
		
		mThumb		= findViewById(R.id.photoThumb);
	   	mName		= findViewById(R.id.photoName);
	   	mDescr		= findViewById(R.id.photoDescr);
	   	mSubject	= findViewById(R.id.photoSubject);
	   	mIsPublic	= findViewById(R.id.photoIsPublic);
	   	
				
		// Setup condition spinner
		ArrayAdapter<PhotoSubject> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, PhotoSubject.values());
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSubject.setAdapter(adapter);

		// Setup button to save photo
		Button savePhotoBtn = findViewById(R.id.photoSave);
		savePhotoBtn.setOnClickListener(arg0 -> {
            Log.i(TAG, "Save Photo");
            setResult(RESULT_OK);
            finish();
        });

		// Setup button to remove photo
		Button removePhotoBtn = findViewById(R.id.photoRemove);
		removePhotoBtn.setOnClickListener(arg0 -> {
            Log.i(TAG, "Remove Photo");
            // delete the photo record from the database
            mDb.deletePhoto(mPhotoId);
            // delete the files from the cache
            new File (mPhotoURL).delete();
            new File (mIconURL).delete();
            setResult(RESULT_CANCELED);
            finish();
        });

		
		// Connect to database
		mDb = new DbHelper(this);
		mDb.open();

	}
	
	@Override
	protected void onPause() {
		Log.i(TAG, "onPause");
		super.onPause();
		savePhoto();
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume");
		super.onResume();
		if (mPhotoId != null) {
			populateFields();
		}
	}

	
	
	
	@Override
	protected void onDestroy() {
		Log.i(TAG, "onDestroy");
		mDb.close();
		super.onDestroy();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			// Handle back button in action bar
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void savePhoto() {
    	Log.i(TAG, "savePhoto");
    	    	
    	// Save the changes to the database
    	mDb.updatePhoto(mPhotoId,
   						mTrigId,
   						mName.getText().toString(),
   						mDescr.getText().toString(),
   						mIconURL,
   						mPhotoURL,
   						(PhotoSubject) mSubject.getSelectedItem(),
   						mIsPublic.isChecked()?1:0);	
	}

	@SuppressLint("Range")
    private void populateFields () {
    	Log.i(TAG, "populateFields");
    	
    	Cursor c = mDb.fetchPhoto(mPhotoId);
    	if (c == null) {return;}

    	// set hidden variables
    	mIconURL 				= c.getString(c.getColumnIndex(DbHelper.PHOTO_ICON));
    	mPhotoURL 				= c.getString(c.getColumnIndex(DbHelper.PHOTO_PHOTO));
    	mTrigId 				= c.getLong(c.getColumnIndex(DbHelper.PHOTO_TRIG));

    	// set thumbnail image
    	mThumb.setImageBitmap(BitmapFactory.decodeFile(mIconURL));
    	
    	// set Text fields    	
    	mName.setText 			(c.getString(c.getColumnIndex(DbHelper.PHOTO_NAME)));
    	mDescr.setText			(c.getString(c.getColumnIndex(DbHelper.PHOTO_DESCR)));
    	
    	// set Flags
    	mIsPublic.setChecked	(c.getInt(c.getColumnIndex(DbHelper.PHOTO_ISPUBLIC)) > 0);
    	    	
    	// set Subject
    	PhotoSubject subj = PhotoSubject.fromCode(c.getString(c.getColumnIndex(DbHelper.PHOTO_SUBJECT)));
    	mSubject.setSelection (Arrays.asList(PhotoSubject.values()).indexOf(subj));

    	c.close();

    	
	}


}
