package com.trigpointinguk.android.trigdetails;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Gallery;
import android.widget.Toast;

import com.trigpointinguk.android.DbHelper;
import com.trigpointinguk.android.R;
import com.trigpointinguk.android.common.DisplayBitmapActivity;

public class TrigDetailsOSMapTab extends Activity {
	private static final String TAG = "TrigDetailsOSMapTab";

	private long mTrigId;
	private DbHelper mDb;
	private String[] mImagePaths;
	private ExecutorService mExecutor;
	private Handler mMainHandler;
	
	// Tile configuration
	private static final int TILE_SIZE = 256;
	private static final int GRID_SIZE = 3; // 3x3 grid
	private static final int FINAL_IMAGE_SIZE = TILE_SIZE * 2; // 2x tile size as requested
	
	// Map configurations: {name, baseUrl, needsApiKey, minZoom, maxZoom, is27700}
	private static final MapConfig[] MAP_CONFIGS = {
		new MapConfig("OSM", "https://tile.openstreetmap.org/{z}/{x}/{y}.png", false, 8, 12, false),
		new MapConfig("OS_Outdoor", "https://api.os.uk/maps/raster/v1/zxy/Outdoor_3857/{z}/{x}/{y}.png", true, 8, 12, false),
		new MapConfig("OS_Leisure", "https://api.os.uk/maps/raster/v1/zxy/Leisure_27700/{z}/{x}/{y}.png", true, 5, 9, true)
	};
	
	private static class MapConfig {
		final String name;
		final String baseUrl;
		final boolean needsApiKey;
		final int minZoom;
		final int maxZoom;
		final boolean is27700; // Uses British National Grid projection
		
		MapConfig(String name, String baseUrl, boolean needsApiKey, int minZoom, int maxZoom, boolean is27700) {
			this.name = name;
			this.baseUrl = baseUrl;
			this.needsApiKey = needsApiKey;
			this.minZoom = minZoom;
			this.maxZoom = maxZoom;
			this.is27700 = is27700;
		}
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.trigosmap);

		// Initialise threading
		mExecutor = Executors.newSingleThreadExecutor();
		mMainHandler = new Handler(Looper.getMainLooper());

		// get trig_id from extras
        Bundle extras = getIntent().getExtras();
		if (extras == null) {return;}
		mTrigId = extras.getLong(DbHelper.TRIG_ID);
		Log.i(TAG, "Trig_id = "+mTrigId);
		
		// get trig info from database
		mDb = new DbHelper(TrigDetailsOSMapTab.this);
		mDb.open();		
		Cursor c = mDb.fetchTrigInfo(mTrigId);
		c.moveToFirst();
		
		// Get coordinates
		double lat = c.getDouble(c.getColumnIndex(DbHelper.TRIG_LAT));
		double lon = c.getDouble(c.getColumnIndex(DbHelper.TRIG_LON));
		c.close();
		
		Log.d(TAG, "Generating cached map images for lat: " + lat + ", lon: " + lon);
		
		// Generate cached images and get file paths
		generateCachedImages(lat, lon);
	}
	
	private void generateCachedImages(double lat, double lon) {
		List<String> imagePaths = new ArrayList<>();
		List<CompletableFuture<String>> tasks = new ArrayList<>();
		
		// Generate images for each map configuration at different zoom levels
		for (MapConfig config : MAP_CONFIGS) {
			for (int zoom = config.minZoom; zoom <= config.maxZoom; zoom += 2) {
				tasks.add(generateTileBasedImage(mTrigId, lat, lon, config, zoom)
					.thenApply(path -> {
						if (path != null) {
							synchronized (imagePaths) {
								imagePaths.add(path);
							}
						}
						return path;
					}));
			}
		}
		
		CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
			.thenRun(() -> {
				mMainHandler.post(() -> {
					mImagePaths = imagePaths.toArray(new String[0]);
					setupGallery();
				});
			})
			.exceptionally(throwable -> {
				Log.e(TAG, "Error generating cached images", throwable);
				mMainHandler.post(() -> {
					Toast.makeText(this, "Error generating map images", Toast.LENGTH_SHORT).show();
				});
				return null;
			});
	}
	
	private CompletableFuture<String> generateTileBasedImage(long trigId, double lat, double lon, 
															MapConfig config, int zoom) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				// Check if cached file already exists
				String fileName = String.format("trig_%d_%s_z%d.png", trigId, config.name, zoom);
				File cacheDir = new File(getCacheDir(), "map_images");
				if (!cacheDir.exists()) {
					cacheDir.mkdirs();
				}
				File cachedFile = new File(cacheDir, fileName);
				
				if (cachedFile.exists()) {
					Log.d(TAG, "Using cached image: " + fileName);
					return cachedFile.getAbsolutePath();
				}
				
				// Convert lat/lon to tile coordinates
				int centerX, centerY;
				if (config.is27700) {
					// For EPSG:27700 (British National Grid), use different conversion
					centerX = lonToTileX27700(lat, lon, zoom);
					centerY = latToTileY27700(lat, lon, zoom);
					Log.d(TAG, String.format("EPSG:27700 coords for %s zoom %d: lat=%.6f,lon=%.6f -> tile=%d,%d", 
						config.name, zoom, lat, lon, centerX, centerY));
				} else {
					// For Web Mercator (EPSG:3857)
					centerX = lonToTileX(lon, zoom);
					centerY = latToTileY(lat, zoom);
					Log.d(TAG, String.format("Web Mercator coords for %s zoom %d: lat=%.6f,lon=%.6f -> tile=%d,%d", 
						config.name, zoom, lat, lon, centerX, centerY));
				}
				
				// Create 3x3 grid of tiles
				Bitmap compositeBitmap = Bitmap.createBitmap(
					TILE_SIZE * GRID_SIZE, TILE_SIZE * GRID_SIZE, Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(compositeBitmap);
				
				// Download and draw each tile
				for (int dx = -1; dx <= 1; dx++) {
					for (int dy = -1; dy <= 1; dy++) {
						int tileX = centerX + dx;
						int tileY = centerY + dy;
						
						Bitmap tileBitmap = downloadTile(config, zoom, tileX, tileY);
						if (tileBitmap != null) {
							int drawX = (dx + 1) * TILE_SIZE;
							int drawY = (dy + 1) * TILE_SIZE;
							canvas.drawBitmap(tileBitmap, drawX, drawY, null);
							tileBitmap.recycle();
						}
					}
				}
				
				// Calculate the exact pixel position of the trigpoint within the center tile
				double pixelX, pixelY;
				if (config.is27700) {
					pixelX = lonToPixelX27700(lat, lon, zoom) - (centerX * TILE_SIZE);
					pixelY = latToPixelY27700(lat, lon, zoom) - (centerY * TILE_SIZE);
				} else {
					pixelX = lonToPixelX(lon, zoom) - (centerX * TILE_SIZE);
					pixelY = latToPixelY(lat, zoom) - (centerY * TILE_SIZE);
				}
				
				// Adjust for the center tile position in our 3x3 grid
				int centerPixelX = TILE_SIZE + (int)pixelX;
				int centerPixelY = TILE_SIZE + (int)pixelY;
				
				// Crop to final size (2x tile size) centered on trigpoint
				int cropLeft = centerPixelX - (FINAL_IMAGE_SIZE / 2);
				int cropTop = centerPixelY - (FINAL_IMAGE_SIZE / 2);
				
				// Ensure crop area is within bounds
				cropLeft = Math.max(0, Math.min(cropLeft, compositeBitmap.getWidth() - FINAL_IMAGE_SIZE));
				cropTop = Math.max(0, Math.min(cropTop, compositeBitmap.getHeight() - FINAL_IMAGE_SIZE));
				
				Bitmap finalBitmap = Bitmap.createBitmap(compositeBitmap, 
					cropLeft, cropTop, FINAL_IMAGE_SIZE, FINAL_IMAGE_SIZE);
				compositeBitmap.recycle();
				
				// Save to cache
				try (FileOutputStream out = new FileOutputStream(cachedFile)) {
					finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
					finalBitmap.recycle();
					Log.d(TAG, "Cached new image: " + fileName);
					return cachedFile.getAbsolutePath();
				}
				
			} catch (Exception e) {
				Log.e(TAG, "Error generating image for " + config.name + " zoom " + zoom, e);
				return null;
			}
		}, mExecutor);
	}
	
	private Bitmap downloadTile(MapConfig config, int z, int x, int y) {
		try {
			String urlString = config.baseUrl
				.replace("{z}", String.valueOf(z))
				.replace("{x}", String.valueOf(x))
				.replace("{y}", String.valueOf(y));
			
			if (config.needsApiKey) {
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
				String apiKey = prefs.getString("os_api_key", "");
				if (apiKey.isEmpty()) {
					Log.w(TAG, "No OS API key configured for " + config.name);
					return null;
				}
				urlString += "?key=" + apiKey;
			}
			
			URL url = new URL(urlString);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(10000);
			connection.setReadTimeout(10000);
			connection.setRequestProperty("User-Agent", "TrigpointingUK-Android");
			
			try (InputStream is = connection.getInputStream()) {
				return BitmapFactory.decodeStream(is);
			}
			
		} catch (Exception e) {
			Log.w(TAG, "Failed to download tile " + z + "/" + x + "/" + y + " from " + config.name, e);
			return null;
		}
	}
	
	// Web Mercator projection helper methods
	private int lonToTileX(double lon, int zoom) {
		return (int) Math.floor((lon + 180.0) / 360.0 * Math.pow(2.0, zoom));
	}
	
	private int latToTileY(double lat, int zoom) {
		double latRad = Math.toRadians(lat);
		return (int) Math.floor((1.0 - asinh(Math.tan(latRad)) / Math.PI) / 2.0 * Math.pow(2.0, zoom));
	}
	
	private double lonToPixelX(double lon, int zoom) {
		return (lon + 180.0) / 360.0 * Math.pow(2.0, zoom) * TILE_SIZE;
	}
	
	private double latToPixelY(double lat, int zoom) {
		double latRad = Math.toRadians(lat);
		return (1.0 - asinh(Math.tan(latRad)) / Math.PI) / 2.0 * Math.pow(2.0, zoom) * TILE_SIZE;
	}
	
	// Manual implementation of asinh for older Java versions
	private double asinh(double x) {
		return Math.log(x + Math.sqrt(x * x + 1.0));
	}
	
	// EPSG:27700 (British National Grid) coordinate conversion methods
	// Based on Leaflet configuration: resolutions, origin, and bounds
	private static final double[] OSGB_RESOLUTIONS = {896, 448, 224, 112, 56, 28, 14, 7, 3.5, 1.75, 0.875, 0.4375, 0.21875};
	private static final double OSGB_ORIGIN_X = -238375.0;
	private static final double OSGB_ORIGIN_Y = 1376256.0;
	private static final double OSGB_BOUNDS_MIN_X = -238375.0;
	private static final double OSGB_BOUNDS_MIN_Y = 0.0;
	private static final double OSGB_BOUNDS_MAX_X = 900000.0;
	private static final double OSGB_BOUNDS_MAX_Y = 1376256.0;
	
	private int lonToTileX27700(double lat, double lon, int zoom) {
		// Convert WGS84 lon/lat to OSGB36 easting/northing (simplified transformation)
		double[] osgb = wgs84ToOsgb36(lat, lon);
		double easting = osgb[0];
		
		// Use OSGB tile coordinate system
		if (zoom >= OSGB_RESOLUTIONS.length) zoom = OSGB_RESOLUTIONS.length - 1;
		double resolution = OSGB_RESOLUTIONS[zoom];
		
		int tileX = (int) Math.floor((easting - OSGB_ORIGIN_X) / (resolution * TILE_SIZE));
		return tileX;
	}
	
	private int latToTileY27700(double lat, double lon, int zoom) {
		// Convert WGS84 lon/lat to OSGB36 easting/northing
		double[] osgb = wgs84ToOsgb36(lat, lon);
		double northing = osgb[1];
		
		// Use OSGB tile coordinate system  
		if (zoom >= OSGB_RESOLUTIONS.length) zoom = OSGB_RESOLUTIONS.length - 1;
		double resolution = OSGB_RESOLUTIONS[zoom];
		
		int tileY = (int) Math.floor((OSGB_ORIGIN_Y - northing) / (resolution * TILE_SIZE));
		return tileY;
	}
	
	private double lonToPixelX27700(double lat, double lon, int zoom) {
		double[] osgb = wgs84ToOsgb36(lat, lon);
		double easting = osgb[0];
		
		if (zoom >= OSGB_RESOLUTIONS.length) zoom = OSGB_RESOLUTIONS.length - 1;
		double resolution = OSGB_RESOLUTIONS[zoom];
		
		return (easting - OSGB_ORIGIN_X) / resolution;
	}
	
	private double latToPixelY27700(double lat, double lon, int zoom) {
		double[] osgb = wgs84ToOsgb36(lat, lon);
		double northing = osgb[1];
		
		if (zoom >= OSGB_RESOLUTIONS.length) zoom = OSGB_RESOLUTIONS.length - 1;
		double resolution = OSGB_RESOLUTIONS[zoom];
		
		return (OSGB_ORIGIN_Y - northing) / resolution;
	}
	
	// Simplified WGS84 to OSGB36 transformation (approximate for UK)
	// Returns [easting, northing] in metres
	private double[] wgs84ToOsgb36(double lat, double lon) {
		// This is a simplified transformation suitable for the UK area
		// For production use, you'd want a proper coordinate transformation library
		
		// Approximate transformation parameters for UK
		double a = 6377563.396;      // OSGB36 semi-major axis
		double b = 6356256.909;      // OSGB36 semi-minor axis
		double f0 = 0.9996012717;    // Scale factor on central meridian
		double lat0 = Math.toRadians(49);     // Latitude of true origin
		double lon0 = Math.toRadians(-2);     // Longitude of true origin
		double N0 = -100000;         // Northing of true origin
		double E0 = 400000;          // Easting of true origin
		
		double latRad = Math.toRadians(lat);
		double lonRad = Math.toRadians(lon);
		
		double e2 = 1 - (b * b) / (a * a);
		double n = (a - b) / (a + b);
		double n2 = n * n;
		double n3 = n * n * n;
		
		double cosLat = Math.cos(latRad);
		double sinLat = Math.sin(latRad);
		double tanLat = Math.tan(latRad);
		
		double nu = a * f0 / Math.sqrt(1 - e2 * sinLat * sinLat);
		double rho = a * f0 * (1 - e2) / Math.pow(1 - e2 * sinLat * sinLat, 1.5);
		double eta2 = nu / rho - 1;
		
		double dLon = lonRad - lon0;
		double dLon2 = dLon * dLon;
		double dLon3 = dLon2 * dLon;
		double dLon4 = dLon3 * dLon;
		double dLon5 = dLon4 * dLon;
		double dLon6 = dLon5 * dLon;
		
		double M = b * f0 * ((1 + n + (5/4) * n2 + (5/4) * n3) * (latRad - lat0)
			- (3 * n + 3 * n2 + (21/8) * n3) * Math.sin(latRad - lat0) * Math.cos(latRad + lat0)
			+ ((15/8) * n2 + (15/8) * n3) * Math.sin(2 * (latRad - lat0)) * Math.cos(2 * (latRad + lat0))
			- (35/24) * n3 * Math.sin(3 * (latRad - lat0)) * Math.cos(3 * (latRad + lat0)));
		
		double I = M + N0;
		double II = (nu / 2) * sinLat * cosLat;
		double III = (nu / 24) * sinLat * Math.pow(cosLat, 3) * (5 - tanLat * tanLat + 9 * eta2);
		double IIIA = (nu / 720) * sinLat * Math.pow(cosLat, 5) * (61 - 58 * tanLat * tanLat + Math.pow(tanLat, 4));
		
		double IV = nu * cosLat;
		double V = (nu / 6) * Math.pow(cosLat, 3) * (nu / rho - tanLat * tanLat);
		double VI = (nu / 120) * Math.pow(cosLat, 5) * (5 - 18 * tanLat * tanLat + Math.pow(tanLat, 4) + 14 * eta2 - 58 * tanLat * tanLat * eta2);
		
		double northing = I + II * dLon2 + III * dLon4 + IIIA * dLon6;
		double easting = E0 + IV * dLon + V * dLon3 + VI * dLon5;
		
		return new double[]{easting, northing};
	}
	
	private void setupGallery() {
		Gallery gallery = (Gallery) findViewById(R.id.trigosgallery);
		gallery.setAdapter(new TrigDetailsOSMapAdapter(this, mImagePaths));
		
		gallery.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				Intent i = new Intent(TrigDetailsOSMapTab.this, DisplayBitmapActivity.class);
				// Pass the file path directly, DisplayBitmapActivity should handle local files
				i.putExtra("URL", mImagePaths[position]);
				Log.i(TAG, "Clicked OSMap at path: " + mImagePaths[position]);
				startActivity(i);
			}
		});
	}

	@Override
	protected void onDestroy() {
		if (mExecutor != null) {
			mExecutor.shutdown();
		}
		if (mDb != null) {
			mDb.close();
		}
		super.onDestroy();
	}
}