package com.trigpointinguk.android.mapping;

import java.util.ArrayList;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;

import org.osmdroid.tileprovider.util.CloudmadeUtil;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.OverlayItem.HotspotPlace;
import org.osmdroid.views.overlay.ScaleBarOverlay;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.trigpointinguk.android.DbHelper;
import com.trigpointinguk.android.R;
import com.trigpointinguk.android.filter.FilterActivity;
import com.trigpointinguk.android.mapping.MapIcon.colourScheme;
import com.trigpointinguk.android.trigdetails.TrigDetailsActivity;
import com.trigpointinguk.android.types.Condition;
import com.trigpointinguk.android.types.Trig;





public class MapActivity extends AppCompatActivity implements MapListener {
	private static final int DEFAULT_LON = -1450510;
	private static final int DEFAULT_LAT = 50931280;
	public static final String TAG = "MapActivity";
	
    public enum TileSource 		{NONE, MAPNIK, MAPQUEST, CLOUDMADE, CYCLEMAP, USGS_SAT, USGS_TOPO, ORDNANCE_SURVEY, PUBLIC_TRANSPORT, BING_AERIAL, BING_ROAD, BING_AERIAL_LABELS, BING_OSGB};	

	private MapView            mMapView;
	private MapController      mMapController;
	private MyLocationNewOverlay  mMyLocationOverlay;
	private ScaleBarOverlay    mScaleBarOverlay;  

	private SharedPreferences  mPrefs;
	private DbHelper    	   mDb;
	
	private ItemizedIconOverlay<OverlayItem> mTrigOverlay;
	private BoundingBox      		mBigBB         = new BoundingBox(0.0, 0.0, 0.0, 0.0);
	private MapIcon.colourScheme    mIconColouring = colourScheme.NONE;
	private TileSource         		mTileSource    = TileSource.NONE;
	private boolean            		mTooManyTrigs;
	private com.trigpointinguk.android.mapping.ResourceProxyImpl mResourceProxy;
	private FloatingActionButton mFabLocation;

    // Rough bounding box for USGS National Map coverage (includes Alaska/Hawaii broadly)
    // If the current map center is outside this, USGS Topo will not return tiles.
    private static final double US_MIN_LAT = 14.0;   // approx southernmost
    private static final double US_MAX_LAT = 72.0;   // approx northernmost (Alaska)
    private static final double US_MIN_LON = -172.0; // approx westernmost (Alaska/Aleutians)
    private static final double US_MAX_LON = -52.0;  // approx easternmost
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mapview);
		
		// Enable back button in action bar
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}
		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		try {
			mDb = new DbHelper(this);
			mDb.open();
		} catch (SQLException e) {
			e.printStackTrace();
        	Toast.makeText(this, "Error opening database.  Please try again shortly.", Toast.LENGTH_SHORT).show();
			finish();
		}

		// basic map setup
		mResourceProxy = new ResourceProxyImpl(getApplicationContext());
		mMapView = (MapView) findViewById(R.id.mapview);  
		mMapView.setBuiltInZoomControls(false); // Disable zoom buttons - use pinch zoom instead
		mMapView.setMultiTouchControls(true);
		
		// Debug: Log initial tile source
		try {
			Log.i(TAG, "Initial tile source: " + mMapView.getTileProvider().getTileSource().name());
			Log.i(TAG, "Initial tile provider: " + mMapView.getTileProvider().getClass().getSimpleName());
		} catch (Exception e) {
			Log.e(TAG, "Error getting initial tile source", e);
		}
		
		mMapController = (MapController) mMapView.getController();


		// setup current location overlay
		mMyLocationOverlay = new MyLocationNewOverlay(mMapView);      
		mMapView.getOverlays().add(mMyLocationOverlay);

		// add scalebar
		mScaleBarOverlay = new ScaleBarOverlay(mMapView);                          
		mMapView.getOverlays().add(mScaleBarOverlay);

		// setup trigpoint overlay
		mTrigOverlay = new ItemizedIconOverlay<OverlayItem>(
				new ArrayList<OverlayItem>(),
				this.getResources().getDrawable(MapIcon.defaultIcon),
				new OnItemGestureListener<OverlayItem>() {
					@Override
					public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
						// When icon is tapped, jump to TrigDetails activity
						Intent i = new Intent(MapActivity.this, TrigDetailsActivity.class);
						i.putExtra(DbHelper.TRIG_ID, (long)Long.parseLong(item.getTitle()));
						startActivityForResult(i, index);
						return true; // We 'handled' this event.
					}

					@Override
					public boolean onItemLongPress(final int index,
							final OverlayItem item) {
						Toast.makeText(MapActivity.this, item.getSnippet(), Toast.LENGTH_SHORT).show();
						return false;
					}
				}, this);
		mMapView.getOverlays().add(mTrigOverlay);
		mMapView.setMapListener(this);
		
		// Setup Floating Action Button for location
		mFabLocation = findViewById(R.id.fab_location);
		mFabLocation.setOnClickListener(v -> {
			mMyLocationOverlay.enableFollowLocation();
			Toast.makeText(MapActivity.this, "Centering on your location", Toast.LENGTH_SHORT).show();
		});
		
		loadViewFromPrefs();
	}


    private void setTileProvider(TileSource tileSource) {
        mTileSource = tileSource;
        Log.i(TAG, "setTileProvider: Requested tile source: " + tileSource);
        TileSource effectiveTileSource = tileSource;
		
		try {
			switch (tileSource) {
            case MAPNIK:
				Log.i(TAG, "setTileProvider: Using MAPNIK");
				mMapView.setTileSource(TileSourceFactory.MAPNIK);
				break;
            case MAPQUEST:
				Log.i(TAG, "setTileProvider: MAPQUEST not available in this OSMdroid version, using MAPNIK");
				mMapView.setTileSource(TileSourceFactory.MAPNIK);
                effectiveTileSource = TileSource.MAPNIK;
				break;
            case CLOUDMADE:
				Log.i(TAG, "setTileProvider: CLOUDMADE not available in this OSMdroid version, using MAPNIK");
				mMapView.setTileSource(TileSourceFactory.MAPNIK);
                effectiveTileSource = TileSource.MAPNIK;
				break;
            case CYCLEMAP:
				Log.i(TAG, "setTileProvider: CYCLEMAP not available in this OSMdroid version, using MAPNIK");
				mMapView.setTileSource(TileSourceFactory.MAPNIK);
                effectiveTileSource = TileSource.MAPNIK;
				break;
            case USGS_SAT:
				Log.i(TAG, "setTileProvider: USGS_SAT not available, using MAPNIK");
				mMapView.setTileSource(TileSourceFactory.MAPNIK);
                effectiveTileSource = TileSource.MAPNIK;
				break;
					case USGS_TOPO:
			Log.i(TAG, "setTileProvider: Using USGS_TOPO");
			try {
				// Debug: Check what USGS_TOPO actually is
				Log.i(TAG, "setTileProvider: USGS_TOPO object: " + TileSourceFactory.USGS_TOPO);
				Log.i(TAG, "setTileProvider: USGS_TOPO name: " + TileSourceFactory.USGS_TOPO.name());
				
				// Debug: Check if USGS_TOPO has a base URL
				try {
					Log.i(TAG, "setTileProvider: USGS_TOPO base URL: " + TileSourceFactory.USGS_TOPO.getBaseUrl());
				} catch (Exception e) {
					Log.e(TAG, "setTileProvider: Error getting USGS_TOPO base URL", e);
				}

                    // If outside US coverage, avoid blank map and fall back
                    IGeoPoint center = mMapView.getMapCenter();
                    double lat = center != null ? center.getLatitude() : (DEFAULT_LAT / 1E6);
                    double lon = center != null ? center.getLongitude() : (DEFAULT_LON / 1E6);
                    boolean inUsCoverage = lat >= US_MIN_LAT && lat <= US_MAX_LAT && lon >= US_MIN_LON && lon <= US_MAX_LON;
                    Log.i(TAG, "setTileProvider: Center lat/lon=" + lat + "," + lon + " inUsCoverage=" + inUsCoverage);
                    if (!inUsCoverage) {
                        Toast.makeText(this, "USGS Topo covers the USA only. Falling back to Mapnik.", Toast.LENGTH_LONG).show();
                        mMapView.setTileSource(TileSourceFactory.MAPNIK);
                        effectiveTileSource = TileSource.MAPNIK;
                        break;
                    }
				
				mMapView.setTileSource(TileSourceFactory.USGS_TOPO);
				Log.i(TAG, "setTileProvider: Successfully set USGS_TOPO tile source");
				
				// Debug: Check what was actually set
				Log.i(TAG, "setTileProvider: After setting, tile source is: " + mMapView.getTileProvider().getTileSource().name());
				
				// Debug: Check if the tile provider is actually using USGS
				Log.i(TAG, "setTileProvider: Tile provider class: " + mMapView.getTileProvider().getClass().getSimpleName());
				Log.i(TAG, "setTileProvider: Tile provider tile source: " + mMapView.getTileProvider().getTileSource().name());
				
            } catch (Exception e) {
				Log.e(TAG, "setTileProvider: Error setting USGS_TOPO tile source", e);
				// Fallback to MAPNIK
				mMapView.setTileSource(TileSourceFactory.MAPNIK);
                effectiveTileSource = TileSource.MAPNIK;
				Log.i(TAG, "setTileProvider: Fallback to MAPNIK due to USGS_TOPO error");
			}
			break;
            case PUBLIC_TRANSPORT:
				Log.i(TAG, "setTileProvider: PUBLIC_TRANSPORT not available, using MAPNIK");
				mMapView.setTileSource(TileSourceFactory.MAPNIK);
                effectiveTileSource = TileSource.MAPNIK;
				break;
            case ORDNANCE_SURVEY:
                // OS Leisure GB tiles require API key and UK coverage; fall back if missing
                String osApiKey = PreferenceManager.getDefaultSharedPreferences(this).getString("os_api_key", "");
                if (osApiKey == null || osApiKey.trim().isEmpty()) {
                    Toast.makeText(this, "Set Ordnance Survey API key in Preferences to use OS maps", Toast.LENGTH_LONG).show();
                    mMapView.setTileSource(TileSourceFactory.MAPNIK);
                    effectiveTileSource = TileSource.MAPNIK;
                    break;
                }
                // UK bounding box (rough): lat 49..61, lon -8..2
                IGeoPoint osCenter = mMapView.getMapCenter();
                double osLat = osCenter != null ? osCenter.getLatitude() : (DEFAULT_LAT / 1E6);
                double osLon = osCenter != null ? osCenter.getLongitude() : (DEFAULT_LON / 1E6);
                boolean inUk = osLat >= 49.0 && osLat <= 61.0 && osLon >= -8.0 && osLon <= 2.0;
                Log.i(TAG, "setTileProvider: OS check lat/lon=" + osLat + "," + osLon + " inUk=" + inUk);
                try {
                    if (!inUk) {
                        Toast.makeText(this, "OS maps cover Great Britain only. Falling back to Mapnik.", Toast.LENGTH_LONG).show();
                        mMapView.setTileSource(TileSourceFactory.MAPNIK);
                        Log.i(TAG, "setTileProvider: Fallback to MAPNIK due to outside GB");
                        effectiveTileSource = TileSource.MAPNIK;
                    } else {
                        mMapView.setTileSource(new OsLeisureTileSource(osApiKey));
                        Log.i(TAG, "setTileProvider: Using Ordnance Survey Leisure tile source");
                        effectiveTileSource = TileSource.ORDNANCE_SURVEY;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "setTileProvider: Error setting Ordnance Survey tile source", e);
                    mMapView.setTileSource(TileSourceFactory.MAPNIK);
                    Log.i(TAG, "setTileProvider: Fallback to MAPNIK due to OS error");
                    effectiveTileSource = TileSource.MAPNIK;
                }
                break;
            case BING_AERIAL:
				Log.i(TAG, "setTileProvider: Bing not available, using MAPNIK");
				mMapView.setTileSource(TileSourceFactory.MAPNIK);
                effectiveTileSource = TileSource.MAPNIK;
				break;
            case BING_AERIAL_LABELS:
				Log.i(TAG, "setTileProvider: Bing not available, using MAPNIK");
				mMapView.setTileSource(TileSourceFactory.MAPNIK);
                effectiveTileSource = TileSource.MAPNIK;
				break;
            case BING_ROAD:
				Log.i(TAG, "setTileProvider: Bing not available, using MAPNIK");
				mMapView.setTileSource(TileSourceFactory.MAPNIK);
                effectiveTileSource = TileSource.MAPNIK;
				break;
            case BING_OSGB:
            case NONE:
				Log.i(TAG, "setTileProvider: Using MAPNIK as default");
				mMapView.setTileSource(TileSourceFactory.MAPNIK);
                effectiveTileSource = TileSource.MAPNIK;
				break;
			}
		} catch (Exception e) {
			Log.e(TAG, "setTileProvider: Error setting tile source: " + tileSource, e);
			// Fallback to MAPNIK
			mMapView.setTileSource(TileSourceFactory.MAPNIK);
            effectiveTileSource = TileSource.MAPNIK;
		}
		
		// save choice to prefs
		Editor editor = mPrefs.edit();
        editor.putString("tileSource", effectiveTileSource.toString());
		editor.apply();
		
		// Debug: Log available tile sources
		Log.i(TAG, "Available tile sources in this OSMdroid version:");
		try {
			Log.i(TAG, "MAPNIK: " + (TileSourceFactory.MAPNIK != null ? "Available" : "Not available"));
		} catch (Exception e) {
			Log.e(TAG, "MAPNIK: Error checking availability", e);
		}
		try {
			Log.i(TAG, "USGS_TOPO: " + (TileSourceFactory.USGS_TOPO != null ? "Available" : "Not available"));
		} catch (Exception e) {
			Log.e(TAG, "USGS_TOPO: Error checking availability", e);
		}
		try {
			Log.i(TAG, "MAPQUEST: Not available in this OSMdroid version");
		} catch (Exception e) {
			Log.e(TAG, "MAPQUEST: Error checking availability", e);
		}
		
        // Debug: Log current tile source
		try {
			Log.i(TAG, "Current tile source: " + mMapView.getTileProvider().getTileSource().name());
			Log.i(TAG, "Tile provider class: " + mMapView.getTileProvider().getClass().getSimpleName());
            Log.i(TAG, "Effective tile source saved to prefs: " + effectiveTileSource);
		} catch (Exception e) {
			Log.e(TAG, "Error getting current tile source", e);
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mapmenu, menu);
		return result;
	}    

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			// Handle back button in action bar
			finish();
			return true;
		}
		
		int itemId = item.getItemId();
		
		if (itemId == R.id.colourBy) {
			Intent i = new Intent(MapActivity.this, ColourByActivity.class);
			startActivityForResult(i, R.id.colourBy);
			return true;
		} else if (itemId == R.id.mapSource) {
			Intent i = new Intent(MapActivity.this, MapSourceActivity.class);
			startActivityForResult(i, R.id.mapSource);
			return true;
		} else if (itemId == R.id.filterBy) {
            Intent i = new Intent(MapActivity.this, FilterActivity.class);
            startActivityForResult(i, R.id.filterBy);
            return true;
		} else if (itemId == R.id.downloadmaps) {
			Intent i = new Intent(MapActivity.this, DownloadMapsActivity.class);
			startActivity(i);
			return true;
		} else if (itemId == R.id.compass) {
			// Compass functionality has changed in modern OSMdroid
			// TODO: Implement compass functionality when needed
			return true;
		}

		return super.onOptionsItemSelected(item);
	}


	@Override
	protected void onPause() {
		mMyLocationOverlay.disableMyLocation();
		
		Editor editor = mPrefs.edit();
		IGeoPoint mapCenter = mMapView.getMapCenter();
		editor.putInt("latitude", mapCenter.getLatitudeE6());
		editor.putInt("longitude", mapCenter.getLongitudeE6());
		editor.putInt("zoomLevel", mMapView.getZoomLevel());
						BoundingBox bb = mMapView.getBoundingBox();
		editor.putInt("north", (int)(bb.getLatNorth() * 1E6));
		editor.putInt("south", (int)(bb.getLatSouth() * 1E6));
		editor.putInt("east", (int)(bb.getLonEast() * 1E6));
		editor.putInt("west", (int)(bb.getLonWest() * 1E6));
		editor.putString("iconColouring", mIconColouring.toString());
		editor.putBoolean("compass", false); // Compass functionality not yet implemented
		editor.apply();
		super.onPause();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
		
		if (resultCode == RESULT_OK && data != null) {
			if (requestCode == R.id.colourBy) {
				String colourSchemeStr = data.getStringExtra("colourScheme");
				if (colourSchemeStr != null) {
					mIconColouring = colourScheme.valueOf(colourSchemeStr);
					refreshMap();
				}
			} else if (requestCode == R.id.mapSource) {
				String tileSourceStr = data.getStringExtra("tileSource");
				if (tileSourceStr != null) {
					setTileProvider(TileSource.valueOf(tileSourceStr));
				}
			} else if (requestCode == R.id.filterBy) {
				refreshMap();
			}
		}
	}


	@Override
	protected void onResume() {
		super.onResume();
		loadViewFromPrefs();
		mMyLocationOverlay.enableMyLocation();
	}


	@Override
	protected void onDestroy() {
		mDb.close();
		super.onDestroy();
	}

	private void refreshMap() {
		mTooManyTrigs = true; // fudge to ensure redraw
		populateTrigOverlay(mMapView.getBoundingBox());
	}

	private void loadViewFromPrefs() {
		try {
			// set tilesource from prefs
			String tileSourcePref = mPrefs.getString("tileSource", TileSource.BING_OSGB.toString());
			Log.i(TAG, "loadViewFromPrefs: Loading tile source from preferences: " + tileSourcePref);
			setTileProvider(TileSource.valueOf(tileSourcePref));
			// set colouring from prefs
			mIconColouring = colourScheme.valueOf(mPrefs.getString("iconColouring", colourScheme.BYCONDITION.toString()));
		} catch (IllegalArgumentException e) {
			// invalid preference
			Log.e(TAG, "loadViewFromPrefs: Invalid tile source preference, using default", e);
			setTileProvider(TileSource.BING_OSGB);
			mIconColouring = colourScheme.NONE;
		}
		try {
			// set view from prefs
			mMapController.setZoom(mPrefs.getInt("zoomLevel", 12));
			mMapController.setCenter(new GeoPoint(mPrefs.getInt("latitude", DEFAULT_LAT)
												, mPrefs.getInt("longitude", DEFAULT_LON)));
									BoundingBox bb = new BoundingBox( 
												mPrefs.getInt("north", mPrefs.getInt("latitude",  DEFAULT_LAT)  + 80000) / 1E6,
												mPrefs.getInt("east",  mPrefs.getInt("longitude", DEFAULT_LON)  + 80000) / 1E6,
												mPrefs.getInt("south", mPrefs.getInt("latitude",  DEFAULT_LAT)  - 80000) / 1E6,
												mPrefs.getInt("west",  mPrefs.getInt("longitude", DEFAULT_LON)  - 80000) / 1E6);
			populateTrigOverlay(bb);
		} catch (ClassCastException e) {
			// bad coordinates, do nothing
		}	
		// Compass functionality has changed in modern OSMdroid
		// TODO: Implement compass functionality when needed

	}




	@Override
	public boolean onScroll(ScrollEvent event) {
		populateTrigOverlay(mMapView.getBoundingBox());
		return false;
	}


	@Override
	public boolean onZoom(ZoomEvent event) {
		populateTrigOverlay(mMapView.getBoundingBox());
		return false;
	}

	private void populateTrigOverlay(BoundingBox bb) {
		ArrayList<OverlayItem> newitems = new ArrayList<OverlayItem>();
		
		MapIcon mapIcon = new MapIcon(this);

		// Only repopulate if the new bounding box extends beyond region previously queried
		if (bb.getLatNorth() < mBigBB.getLatNorth() 
				&& bb.getLatSouth() > mBigBB.getLatSouth() 
				&& bb.getLonEast() < mBigBB.getLonEast() 
				&& bb.getLonWest() > mBigBB.getLonWest()
				&& mTooManyTrigs == false) {
			return;
		}
		// Cope with empty regions (when map initially loading)
		if (bb.getLatitudeSpan() == 0 || bb.getLongitudeSpan() == 0) {
			return;
		}
		
		Log.w(TAG, "Reloading trigs");
		mTrigOverlay.removeAllItems();
		
		// query a larger region than currently shown, to reduce number of reloads
		mBigBB = bb.increaseByScale(2);
		

		Cursor c = mDb.fetchTrigMapList(mBigBB);
		if (c != null) {
			Log.i(TAG, "Found " + c.getCount() + " trigs");
	
			if (c.getCount() < Integer.parseInt(mPrefs.getString("mapcount", DbHelper.DEFAULT_MAP_COUNT))) {
				mTooManyTrigs = false;
				c.moveToFirst();
				while (c.isAfterLast() == false) {
					GeoPoint point = new GeoPoint((int)(c.getDouble(c.getColumnIndex(DbHelper.TRIG_LAT))*1000000), (int)(c.getDouble(c.getColumnIndex(DbHelper.TRIG_LON))*1000000));
					OverlayItem oi = new OverlayItem(c.getString(c.getColumnIndex(DbHelper.TRIG_ID)), 
													 c.getString(c.getColumnIndex(DbHelper.TRIG_NAME)), point);
					
					// forward nulls
					String strUnsynced = c.getString(c.getColumnIndex(DbHelper.JOIN_UNSYNCED));
					Condition unsynced = null;
					if (strUnsynced != null) {
						unsynced = Condition.fromCode(strUnsynced);
					}
					
					oi.setMarker(mapIcon.getDrawable(	mIconColouring, 
														Trig.Physical.fromCode(c.getString(c.getColumnIndex(DbHelper.TRIG_TYPE))),
														Condition.fromCode(c.getString(c.getColumnIndex(DbHelper.TRIG_CONDITION))),
														Condition.fromCode(c.getString(c.getColumnIndex(DbHelper.TRIG_LOGGED))),
														unsynced,
														c.getString(c.getColumnIndex(DbHelper.JOIN_MARKED)) != null
													) );
					oi.setMarkerHotspot(HotspotPlace.CENTER);
					newitems.add(oi);
					c.moveToNext();
				}
				mTrigOverlay.addItems(newitems);
			} else {
				mTooManyTrigs = true;
				Log.i(TAG, "Too Many Trigs");
			}
			c.close();
		}
		mMapView.invalidate();
	}
	
}