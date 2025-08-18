package uk.trigpointing.android.logging;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.zip.GZIPInputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONException;
import org.json.JSONObject;

import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.view.ViewGroup;
import android.util.TypedValue;

import uk.trigpointing.android.DbHelper;
import uk.trigpointing.android.R;
import uk.trigpointing.android.common.CountingMultipartEntity.ProgressListener;
import uk.trigpointing.android.common.ProgressRequestBody;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import uk.trigpointing.android.types.Condition;



public class SyncTask implements ProgressListener {
	public static final String TAG ="SyncTask";
	private Context 			mCtx;
	private SharedPreferences 	mPrefs;
    private AlertDialog 		progressDialog;
    private ProgressBar 		progressBar;
    private TextView   		progressText;
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private final Handler mainHandler = new Handler(Looper.getMainLooper());
	
	private void updateProgress(int type, int... values) {
        mainHandler.post(() -> {
            String message;
            if (progressDialog == null) {
                showDialog("");
            }
            switch (type) {
            case MAX:
                if (progressBar != null) {
                    progressBar.setIndeterminate(false);
                    progressBar.setMax(values[0]);
                    progressBar.setProgress(0);
                }
                break;
            case PROGRESS:
                if (progressBar != null) {
                    progressBar.setProgress(values[0]);
                }
                break;
            case MESSAGE:
                message = mCtx.getResources().getString(values[0]);
                if (progressText != null) {
                    progressText.setText(message);
                }
                break;
            case MESSAGECOUNT:
                message = "Uploading photo " + values[0] + " of " + values[1];
                if (progressText != null) {
                    progressText.setText(message);
                }
                break;
            case BLANKPROGRESS:
                if (progressBar != null) {
                    progressBar.setMax(1);
                }
                break;
            }
        });
	}

	private	DbHelper 			mDb = null;
	private SyncListener		mSyncListener;
	
    private static boolean 		mLock = false;
    private int 				mAppVersion;
    private int 				mMax;		// Maximum count of things being synced, for progress bar
    private String				mUsername;
    private String				mPassword;
    private String				mErrorMessage;
    
    private int					mActiveByteCount; // byte count from currently transferring photo
    private int					mPreviousByteCount; // byte count from previously transferred photos
    
    private static final int 	MAX 			= 1;
    private static final int 	PROGRESS 		= 2;
    private static final int 	MESSAGE			= 3;
    private static final int 	BLANKPROGRESS	= 4;
    private static final int 	MESSAGECOUNT	= 5;
    
    private static final String PREFS_LOGCOUNT  ="logCount";
    
    public static final int 	SUCCESS 	= 0;
    public static final int 	NOROWS 		= 1;
    public static final int 	ERROR 		= 2;
    public static final int 	CANCELLED 	= 3;
    
    
    
	public SyncTask(Context pCtx, SyncListener listener) {
		this.mCtx = pCtx;
		this.mSyncListener = listener;
		try {
			mAppVersion = mCtx.getPackageManager().getPackageInfo(mCtx.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			Log.e(TAG,"Couldn't get versionCode!");
			mAppVersion = 99999;
		}
	}
	
	public void detach() {
        mCtx = null;
        mSyncListener = null;
        if (progressDialog != null) { progressDialog.dismiss(); }
	}
	
	public void attach(Context pCtx, SyncListener listener) {
		this.mCtx = pCtx;
		this.mSyncListener = listener;
        showDialog("Continuing sync");
	}
	
	public void execute(Long... trigId) {
		// Pre-execution logic (equivalent to onPreExecute)
		if (mCtx == null) {
			Toast.makeText(mCtx, "Sync failed!", Toast.LENGTH_LONG).show();
			return;
		}
		
		// Check that we have a username and password, so that we can sync existing logs
		mPrefs = PreferenceManager.getDefaultSharedPreferences(mCtx);
		if (mPrefs.getString("username", "").equals("")) {
			Toast.makeText(mCtx, R.string.toastAddUsername, Toast.LENGTH_LONG).show();
			return;
		} 
		if (mPrefs.getString("plaintextpassword", "").equals("")) {
			Toast.makeText(mCtx, R.string.toastAddPassword, Toast.LENGTH_LONG).show();
			return;
		} 
		showDialog("Connecting to T:UK");
		mErrorMessage = "";
		
		CompletableFuture.supplyAsync(() -> {
			Log.d(TAG, "doInBackground");
			
			// Make sure only one SyncTask runs at a time
			if (mLock) {
				Log.i(TAG, "SyncTask already running");
				return ERROR;
			}
			mLock = true;

			// Open database connection
			mDb = new DbHelper(mCtx);
			mDb.open();
			
			try {
				// Get details from Prefs
				mUsername = mPrefs.getString("username", "");
				mPassword = mPrefs.getString("plaintextpassword", "");
				if (mUsername.equals("")) {return ERROR;}
				if (mPassword.equals("")) {return ERROR;}

				if (ERROR == sendLogsToTUK(trigId)) {
					return ERROR;
				}
				if (ERROR == sendPhotosToTUK(trigId)) {
					return ERROR;
				}
				mDb.close();
				mDb.open();
				if (trigId.length == 0) {
					if (ERROR == readLogsFromTUK()) {
						return ERROR;
					}
				}
			} finally {
				mDb.close();
				mLock = false;		
			}

			return SUCCESS;
        }, executor)
        .thenAcceptAsync(result -> {
			Log.d(TAG, "onPostExecute " + result);
			// Post-execution logic (equivalent to onPostExecute)
			if (result == SUCCESS) {
				Toast.makeText(mCtx, "Synced with TrigpointingUK " + mErrorMessage, Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(mCtx, "Error syncing with TrigpointingUK - " + mErrorMessage, Toast.LENGTH_LONG).show();					
			}
			try {
                if (progressDialog != null) {progressDialog.dismiss();}
            } catch (Exception e) {
				Log.e(TAG, "Exception dismissing dialog - " + e.getMessage());
			}
			if (mSyncListener != null) {
				mSyncListener.onSynced(result);
			}
        }, r -> mainHandler.post(r));
	}
	

	
	
	
	
	Integer sendLogsToTUK(Long... trigId) {
		Log.d(TAG, "sendLogsToTUK");
		Long trig_id = null;
		
		if (trigId != null && trigId.length >= 1) {
			trig_id = trigId[0];
		}
		
		updateProgress(MESSAGE, R.string.syncToTUK);
		Cursor c = mDb.fetchLogs(trig_id);
		if (c==null) {
			return NOROWS;
		}
		
		updateProgress(MAX, c.getCount());
		updateProgress(PROGRESS, 0);
		
		int i=0;
		do {
			if (SUCCESS != sendLogToTUK(c)) {
				return ERROR;
			}
			updateProgress(PROGRESS, ++i);
		} while (c.moveToNext());
		
		c.close();
		return SUCCESS;
	}
	
	
	Integer sendPhotosToTUK(Long... trigId) {
		Log.d(TAG, "sendPhotosToTUK");
		Long trig_id = null;
		mPreviousByteCount = 0;
		mActiveByteCount = 0;
		
		if (trigId != null && trigId.length >= 1) {
			trig_id = trigId[0];
		}

		updateProgress(MESSAGE, R.string.syncPhotosToTUK);
		Cursor c = mDb.fetchPhotos(trig_id);
		if (c==null) {
			return NOROWS;
		}
		
		// whiz through the cursor, totalling file sizes 
		int totalBytes=0;
		do {
			totalBytes += new File(c.getString(c.getColumnIndex(DbHelper.PHOTO_PHOTO))).length();
		} while (c.moveToNext());
		// reset cursor
		c.moveToFirst();
		
		updateProgress(MAX, totalBytes);
		updateProgress(PROGRESS, 0);
		
		int i = 1;
		do {
			updateProgress(MESSAGECOUNT, i++, c.getCount());
			if (SUCCESS != sendPhotoToTUK(c)) {
				return ERROR;
			}
			mPreviousByteCount += mActiveByteCount;
			mActiveByteCount = 0;
		} while (c.moveToNext());
		
		c.close();
		return SUCCESS;
	}
	
	
	
	Integer sendLogToTUK(Cursor c) {
		Log.i(TAG, "sendLogToTUK");
		
		long trigId = c.getInt(c.getColumnIndex(DbHelper.LOG_ID));
		
        // Build form body
        FormBody formBody = new FormBody.Builder(StandardCharsets.UTF_8)
                .add("username", mUsername)
                .add("password", mPassword)
                .add("id", c.getString(c.getColumnIndex(DbHelper.LOG_ID)))
                .add("year", c.getString(c.getColumnIndex(DbHelper.LOG_YEAR)))
                .add("month", c.getString(c.getColumnIndex(DbHelper.LOG_MONTH)))
                .add("day", c.getString(c.getColumnIndex(DbHelper.LOG_DAY)))
                .add("sendtime", c.getString(c.getColumnIndex(DbHelper.LOG_SENDTIME)))
                .add("hour", c.getString(c.getColumnIndex(DbHelper.LOG_HOUR)))
                .add("minutes", c.getString(c.getColumnIndex(DbHelper.LOG_MINUTES)))
                .add("comment", c.getString(c.getColumnIndex(DbHelper.LOG_COMMENT)))
                .add("gridref", c.getString(c.getColumnIndex(DbHelper.LOG_GRIDREF)))
                .add("fb", c.getString(c.getColumnIndex(DbHelper.LOG_FB)))
                .add("adminflag", c.getString(c.getColumnIndex(DbHelper.LOG_FLAGADMINS)))
                .add("userflag", c.getString(c.getColumnIndex(DbHelper.LOG_FLAGUSERS)))
                .add("score", c.getString(c.getColumnIndex(DbHelper.LOG_SCORE)))
                .add("condition", c.getString(c.getColumnIndex(DbHelper.LOG_CONDITION)))
                .add("sendemail", String.valueOf(mPrefs.getBoolean("sendLogEmails", false)))
                .add("appversion", String.valueOf(mAppVersion))
                .build();
		try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://trigpointing.uk/trigs/android-sync-log.php")
                    .post(formBody)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "RC error - " + response.code());
                    return ERROR;
                }
                String reply = response.body() != null ? response.body().string() : null;
	        Log.d(TAG, "Reply from T:UK - " + reply);
	        if (reply == null || reply == "") {
	        	Log.e(TAG, "No response received from T:UK");
	        	return ERROR;
	        }

	        // Parse the JSON response
	        try {
				JSONObject jo = new JSONObject(reply);
				int status = jo.getInt("status");
				mErrorMessage = jo.getString("msg");
				Log.i(TAG, "Status=" + status + ", msg=" + mErrorMessage);
				
				if (status != 0) {
					return ERROR;
				}
				int logId = jo.getInt("log_id");
				Log.i(TAG, "Successfully inserted log into T:UK - " + logId);
				// remove log from database
				mDb.deleteLog(trigId);
				// update photos for this trig with log id from T:UK
				mDb.updatePhotos(trigId, logId);
				// update local logged condition
				mDb.updateTrigLog(trigId, Condition.fromCode(c.getString(c.getColumnIndex(DbHelper.LOG_CONDITION))));
			} catch (JSONException e1) {
				e1.printStackTrace();
				return ERROR;
            }
        }

        } catch (IOException e) {
            return ERROR;
		} catch (NumberFormatException e) {
			Log.e(TAG, "Unable to convert log_id received from T:UK into integer");
			return ERROR;
		}
		return SUCCESS;
	}
	
	
	
	Integer sendPhotoToTUK(Cursor c) {
		Log.i(TAG, "sendPhotoToTUK");
		
		Long	photoId 	= c.getLong  (c.getColumnIndex(DbHelper.PHOTO_ID));
    	String 	photoPath 	= c.getString(c.getColumnIndex(DbHelper.PHOTO_PHOTO));
    	String 	thumbPath 	= c.getString(c.getColumnIndex(DbHelper.PHOTO_ICON));

		try {
            OkHttpClient client = new OkHttpClient();

            MediaType JPEG = MediaType.parse("image/jpeg");
            File photoFile = new File(photoPath);
            RequestBody photoBody = new ProgressRequestBody(photoFile, JPEG, this);

            MultipartBody.Builder mb = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("username", mUsername)
                    .addFormDataPart("password", mPassword)
                    .addFormDataPart("photoid", photoId.toString())
                    .addFormDataPart("tlog_id", c.getString(c.getColumnIndex(DbHelper.PHOTO_TUKLOGID)))
                    .addFormDataPart("trig", c.getString(c.getColumnIndex(DbHelper.PHOTO_TRIG)))
                    .addFormDataPart("name", c.getString(c.getColumnIndex(DbHelper.PHOTO_NAME)))
                    .addFormDataPart("descr", c.getString(c.getColumnIndex(DbHelper.PHOTO_DESCR)))
                    .addFormDataPart("subject", c.getString(c.getColumnIndex(DbHelper.PHOTO_SUBJECT)))
                    .addFormDataPart("ispublic", c.getString(c.getColumnIndex(DbHelper.PHOTO_ISPUBLIC)))
                    .addFormDataPart("appversion", String.valueOf(mAppVersion))
                    .addFormDataPart("photo", photoFile.getName(), photoBody);

            Request request = new Request.Builder()
                    .url("https://trigpointing.uk/trigs/android-sync-photo.php")
                    .post(mb.build())
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "RC error - " + response.code());
                    return ERROR;
                }
                String reply = response.body() != null ? response.body().string() : null;
	        Log.i(TAG, "Reply from T:UK - " + reply);
	        if (reply == null || reply == "") {
	        	Log.e(TAG, "No response received from T:UK");
	        	return ERROR;
	        }

	        // Parse the JSON response
	        try {
				JSONObject jo = new JSONObject(reply);
				int status = jo.getInt("status");
				mErrorMessage = jo.getString("msg");
				Log.i(TAG, "Status=" + status + ", msg=" + mErrorMessage);
				if (status != 0) {
					return ERROR;
				}
				int tukPhotoId = jo.getInt("photo_id");
				Log.i(TAG, "Successfully uploaded photo to T:UK - " + tukPhotoId);
				// remove log from database
				mDb.deletePhoto(photoId);
				// remove files from cachedir
				new File (photoPath).delete();
				new File (thumbPath).delete();
	        } catch (JSONException e1) {
				e1.printStackTrace();
				return ERROR;
            }
        }

        } catch (IOException e) {
            return ERROR;
		} catch (NumberFormatException e) {
			Log.e(TAG, "Unable to convert tukPhotoId received from T:UK into integer");
			return ERROR;
		}
		return SUCCESS;
	}
	
	@Override
	public void transferred(long num) {
		mActiveByteCount = (int) num;
					updateProgress(PROGRESS, mPreviousByteCount + mActiveByteCount);
		Log.d(TAG, "Transferred bytes: " + num);
	}	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	Integer readLogsFromTUK() {
		Log.d(TAG, "readLogsFromTUK");

        String strLine;                
		int i=0;
		
		
		try {
			updateProgress(BLANKPROGRESS);
			updateProgress(MESSAGE, R.string.syncLogsFromTUK);
			updateProgress(MAX, mPrefs.getInt(PREFS_LOGCOUNT, 1));
			URL url = new URL("https://trigpointing.uk/trigs/down-android-mylogs.php?username="+URLEncoder.encode(mUsername)+"&appversion="+mAppVersion);
			Log.d(TAG, "Getting " + url);
            URLConnection ucon = url.openConnection();
            InputStream is = ucon.getInputStream();
            GZIPInputStream zis = new GZIPInputStream(new BufferedInputStream(is));
            BufferedReader br = new BufferedReader(new InputStreamReader(zis));

			mDb.mDb.beginTransaction();
            
			// blank out any existing logs;
			mDb.deleteAllTrigLogs();
			
			// first record contains log count
			if ((strLine=br.readLine()) != null) {
				mMax = Integer.parseInt(strLine);
				Log.i(TAG, "Log count from TUK = " + mMax);
				updateProgress(MAX, mMax);
			}
			// read log records records
			while ((strLine = br.readLine()) != null && !strLine.trim().equals(""))   {
            	//Log.i(TAG,strLine);
				String[] csv=strLine.split("\t");
				Condition logged		= Condition.fromCode(csv[0]);
				int id					= Integer.valueOf(csv[1]);
				mDb.updateTrigLog(id, logged);
				i++;
				// Cancellation check removed - use CompletableFuture.cancel() if needed
				updateProgress(PROGRESS, i);
            }
			mDb.mDb.setTransactionSuccessful();
        } catch (Exception e) {
        	Log.d(TAG, "Error: " + e);
        	i=-1;
        	mErrorMessage = e.getMessage();
        	return ERROR;
        } finally {
        	if (mDb != null && mDb.mDb.inTransaction()) {
        		mDb.mDb.endTransaction();
        	}
        }

		// store the log count to pre-populate the progress bar next time
		mPrefs.edit().putInt(PREFS_LOGCOUNT,i).apply();
        
		return SUCCESS;		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

    
    
    protected void showDialog(String message) {
        // Build a simple dialog with a horizontal ProgressBar and a message
        LinearLayout container = new LinearLayout(mCtx);
        container.setOrientation(LinearLayout.VERTICAL);
        int paddingPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24,
                mCtx.getResources().getDisplayMetrics());
        container.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

        progressText = new TextView(mCtx);
        progressText.setText(message);
        container.addView(progressText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        progressBar = new ProgressBar(mCtx, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(false);
        container.addView(progressBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        progressDialog = new AlertDialog.Builder(mCtx)
                .setView(container)
                .setCancelable(true)
                .create();
        progressDialog.show();
    }
    

    
    





}