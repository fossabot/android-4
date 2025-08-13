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
	
	// Map configurations: {name, baseUrl, needsApiKey, minZoom, maxZoom}
	private static final MapConfig[] MAP_CONFIGS = {
		new MapConfig("OSM", "https://tile.openstreetmap.org/{z}/{x}/{y}.png", false, 10, 16),
		new MapConfig("OS_Outdoor", "https://api.os.uk/maps/raster/v1/zxy/Outdoor_3857/{z}/{x}/{y}.png", true, 12, 18),
		new MapConfig("OS_Leisure", "https://api.os.uk/maps/raster/v1/zxy/Leisure_27700/{z}/{x}/{y}.png", true, 12, 18)
	};
	
	private static class MapConfig {
		final String name;
		final String baseUrl;
		final boolean needsApiKey;
		final int minZoom;
		final int maxZoom;
		
		MapConfig(String name, String baseUrl, boolean needsApiKey, int minZoom, int maxZoom) {
			this.name = name;
			this.baseUrl = baseUrl;
			this.needsApiKey = needsApiKey;
			this.minZoom = minZoom;
			this.maxZoom = maxZoom;
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
				int centerX = lonToTileX(lon, zoom);
				int centerY = latToTileY(lat, zoom);
				
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
				double pixelX = lonToPixelX(lon, zoom) - (centerX * TILE_SIZE);
				double pixelY = latToPixelY(lat, zoom) - (centerY * TILE_SIZE);
				
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
	
	private void setupGallery() {
		Gallery gallery = (Gallery) findViewById(R.id.trigosgallery);
		gallery.setAdapter(new TrigDetailsOSMapAdapter(this, mImagePaths));
		
		gallery.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				Intent i = new Intent(TrigDetailsOSMapTab.this, DisplayBitmapActivity.class);
				i.putExtra("URL", "file://" + mImagePaths[position]);
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