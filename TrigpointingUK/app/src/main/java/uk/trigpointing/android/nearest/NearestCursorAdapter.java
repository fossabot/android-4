package uk.trigpointing.android.nearest;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import androidx.preference.PreferenceManager;
import android.util.Log;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

import uk.trigpointing.android.DbHelper;
import uk.trigpointing.android.R;
import uk.trigpointing.android.types.Condition;
import uk.trigpointing.android.types.LatLon;
import uk.trigpointing.android.types.LatLon.UNITS;
import uk.trigpointing.android.types.Trig;

public class NearestCursorAdapter extends SimpleCursorAdapter {

    private final LayoutInflater mInflater;
    private int mNameIndex;
    private int mLatIndex;
    private int mLonIndex;
    private int mConditionIndex;
    private int mLoggedIndex;
    private int mTypeIndex;
    private int mUnsyncedIndex;
    private int mMarkedIndex;
    private Location mCurrentLocation;
    private final LatLon.UNITS mUnits;
    //private static final String TAG = "NearestCursorAdapter";
    private double mHeading = 0;
    private double mOrientationOffset = 0;
    private boolean mUsingCompass = false;
    private Context mContext;
    
    
    public NearestCursorAdapter(Context context, int layout, Cursor c,    String[] from, int[] to, Location currentLocation) {
        this(context, layout, c, from, to);
        this.mCurrentLocation = currentLocation;
        this.mContext = context;
    }

    public NearestCursorAdapter(Context context, int layout, Cursor c,
            String[] from, int[] to) {
        super(context, layout, c, from, to, 0);
        mInflater = LayoutInflater.from(context);
        
        // should we list km or miles?
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getString("units", "metric").equals("metric")) {
            mUnits = UNITS.KM;
        } else {
            mUnits = UNITS.MILES;
        }
        
        if (c != null) {
            mNameIndex = c.getColumnIndexOrThrow(DbHelper.TRIG_NAME);
            mConditionIndex = c.getColumnIndexOrThrow(DbHelper.TRIG_CONDITION);
            mTypeIndex = c.getColumnIndexOrThrow(DbHelper.TRIG_TYPE);
            mLoggedIndex = c.getColumnIndexOrThrow(DbHelper.TRIG_LOGGED);
            mLatIndex = c.getColumnIndexOrThrow(DbHelper.TRIG_LAT);    
            mLonIndex = c.getColumnIndexOrThrow(DbHelper.TRIG_LON);
            mUnsyncedIndex = c.getColumnIndexOrThrow(DbHelper.JOIN_UNSYNCED);
            mMarkedIndex = c.getColumnIndexOrThrow(DbHelper.JOIN_MARKED);
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView  tn = view.findViewById(R.id.trigName);
        TextView  td = view.findViewById(R.id.trigDistance);
        ImageView ta = view.findViewById(R.id.trigArrow);
        ImageView tc = view.findViewById(R.id.trigCondition);
        ImageView tt = view.findViewById(R.id.trigType);
        ImageView tl = view.findViewById(R.id.trigLogged);
        
        tn.setText(cursor.getString(mNameIndex));
        tc.setImageResource(Condition.fromCode(cursor.getString(mConditionIndex)).icon());
        
        // deal with marked trigpoints
        Boolean marked = (cursor.getString(mMarkedIndex) != null);
        if (marked) {
            tn.setTypeface(null, Typeface.BOLD);
            tn.setTextColor(ContextCompat.getColor(mContext, R.color.nearestMarkedColour));
        } else {
            tn.setTypeface(null, Typeface.NORMAL);
            tn.setTextColor(ContextCompat.getColor(mContext, R.color.nearestUnmarkedColour));
        }
        tt.setImageResource(Trig.Physical.fromCode(cursor.getString(mTypeIndex)).icon(marked));
        
        // Use either synced condition from T:UK, or highlighted unsynced condition from logs
        Boolean unsynced = (cursor.getString(mUnsyncedIndex) != null);
        if (unsynced) {
            tl.setImageResource(Condition.fromCode(cursor.getString(mUnsyncedIndex)).icon(unsynced));
        } else {
            tl.setImageResource(Condition.fromCode(cursor.getString(mLoggedIndex)).icon(unsynced));            
        }
        
        if (mCurrentLocation != null) {
            LatLon l = new LatLon(cursor.getDouble(mLatIndex), cursor.getDouble(mLonIndex));            
            double distance = l.distanceTo(mCurrentLocation, mUnits);
            double bearing = l.bearingFrom(mCurrentLocation);
            double adjustedBearing = bearing - mHeading;
            int arrowResource = getArrow(adjustedBearing);
            
            td.setText(String.format(Locale.getDefault(), "%3.1f", distance));
            
            // Debug: Check if ImageView is null
            if (ta == null) {
                Log.e("NearestCursorAdapter", "trigArrow ImageView is null!");
            } else {
                ta.setImageResource(arrowResource);
                // Apply rotation to the arrow based on bearing
                float rotation = getArrowRotation(adjustedBearing);
                ta.setRotation(rotation);
                Log.d("NearestCursorAdapter", "Set arrow resource: " + arrowResource + " with rotation: " + rotation + "° for trig: " + cursor.getString(mNameIndex));
            }
            
            // Debug logging for first few items
            String trigName = cursor.getString(mNameIndex);
            if (trigName != null && trigName.length() > 0) {
                Log.d("NearestCursorAdapter", String.format("Trig: %s, Bearing: %.1f°, Adjusted: %.1f°, Arrow: %d", 
                    trigName, bearing, adjustedBearing, arrowResource));
            }
        } else {
            td.setText("");
            if (ta != null) {
                ta.setImageResource(R.drawable.arrow_x);
                Log.d("NearestCursorAdapter", "Set arrow_x resource for null location");
            }
        }
    }


    // Cache for the base arrow drawable to avoid repeated loading
    private Drawable mBaseArrowDrawable;
    
    /**
     * Get a rotated arrow drawable based on the bearing
     * @param bearing The bearing in degrees (0-360)
     * @return A rotated drawable resource ID (for compatibility with existing code)
     */
    public int getArrow(double bearing) {
        // Only apply orientation offset when compass sensor is active
        // When compass is disabled, North should always point to top of screen
        double effectiveOrientationOffset = mUsingCompass ? mOrientationOffset : 0.0;
        double totalRotation = bearing + effectiveOrientationOffset;
        
        Log.d("NearestCursorAdapter", String.format("getArrow: bearing=%.1f°, orientationOffset=%.1f°, effectiveOffset=%.1f°, totalRotation=%.1f°", 
            bearing, mOrientationOffset, effectiveOrientationOffset, totalRotation));
        
        // For now, return the base arrow resource
        // The actual rotation will be handled in bindView where we can set the ImageView rotation
        return R.drawable.arrow_00_n;
    }
    
    /**
     * Get the rotation angle for the arrow based on bearing
     * @param bearing The bearing in degrees (0-360)
     * @return The rotation angle in degrees
     */
    public float getArrowRotation(double bearing) {
        // Only apply orientation offset when compass sensor is active
        // When compass is disabled, North should always point to top of screen
        double effectiveOrientationOffset = mUsingCompass ? mOrientationOffset : 0.0;
        return (float)(bearing + effectiveOrientationOffset);
    }    
    
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.trigrow, parent, false);
    }

    public Cursor swapCursor(Cursor c, Location loc) {
        if (c != null) {
            mNameIndex = c.getColumnIndexOrThrow(DbHelper.TRIG_NAME);
            mConditionIndex = c.getColumnIndexOrThrow(DbHelper.TRIG_CONDITION);
            mTypeIndex = c.getColumnIndexOrThrow(DbHelper.TRIG_TYPE);
            mLoggedIndex = c.getColumnIndexOrThrow(DbHelper.TRIG_LOGGED);
            mLatIndex = c.getColumnIndexOrThrow(DbHelper.TRIG_LAT);    
            mLonIndex = c.getColumnIndexOrThrow(DbHelper.TRIG_LON);
            mUnsyncedIndex = c.getColumnIndexOrThrow(DbHelper.JOIN_UNSYNCED);
            mMarkedIndex = c.getColumnIndexOrThrow(DbHelper.JOIN_MARKED);
        }
        mCurrentLocation = loc;
        return super.swapCursor(c);
    }

    public void setHeading (double heading) {
        mHeading = heading;        
    }
    
    public void setUsingCompass(boolean usingCompass) {
        mUsingCompass = usingCompass;
    }
    public void setOrientation (int orientation) {
        switch (orientation) {
        case 0:  // normal
            mOrientationOffset = 0.0;
            break;
        case 1:  // CCW screen rotation
            mOrientationOffset = -90.0;
            break;
        case 3:  // CW screen rotation
            mOrientationOffset = +90.0;
            break;
        }
    }

}
