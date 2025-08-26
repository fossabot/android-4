package uk.trigpointing.android.trigdetails;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import uk.trigpointing.android.common.BaseTabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager;
import android.widget.Toast;

import uk.trigpointing.android.DbHelper;
import uk.trigpointing.android.R;
import uk.trigpointing.android.common.DisplayBitmapActivity;

public class TrigDetailsOSMapTab extends BaseTabActivity {
	private static final String TAG = "TrigDetailsOSMapTab";

	private long mTrigId;
	private DbHelper mDb;
	private String[] mImagePaths;
	private TrigDetailsOSMapAdapter mAdapter;
	private final AtomicInteger mNextPosition = new AtomicInteger(0);
	private ExecutorService mExecutor;
	private Handler mMainHandler;
	private double mLat;
	private double mLon;
	
	// Tile configuration
	private static final int TILE_SIZE = 256;
	private static final int GRID_SIZE = 3; // 3x3 grid
	private static final int FINAL_IMAGE_SIZE = TILE_SIZE * 2; // 2x tile size as requested
	
	// Map configurations: {name, baseUrl, needsApiKey, minZoom, maxZoom, is27700, attribution}
	private static final MapConfig[] MAP_CONFIGS = {
		new MapConfig("OSM", "https://tile.openstreetmap.org/{z}/{x}/{y}.png", false, 8, 12, false, "© OpenStreetMap contributors"),
		new MapConfig("OS_Outdoor", "https://api.os.uk/maps/raster/v1/zxy/Outdoor_3857/{z}/{x}/{y}.png", true, 8, 12, false, "Contains OS data © Crown copyright and database rights 2024"),
		new MapConfig("OS_Leisure", "https://api.os.uk/maps/raster/v1/zxy/Leisure_27700/{z}/{x}/{y}.png", true, 5, 9, true, "Contains OS data © Crown copyright and database rights 2024"),
		// Satellite layer matches Leaflet's ESRI World Imagery
		new MapConfig(
			"Satellite",
			"https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
			false,
			0,
			18,
			false,
			"Tiles © Esri — Source: Esri, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping, Aerogrid, IGN, IGP, UPR-EGP, and the GIS User Community"
		)
	};

	// Explicit ordered selection of map/zoom pairs to generate/cache and display
	// Add/remove/reorder to control exactly what appears in the grid
	private static final String[][] MAP_SELECTIONS = new String[][]{
		// name, zoom as string (parsed to int)
		{"OSM", "7"},
		{"OS_Outdoor", "8"},
		{"Satellite", "8"},
		{"OS_Leisure", "5"},
		{"OSM", "10"},
		{"OS_Leisure", "7"},
		{"OS_Outdoor", "15"},
		{"Satellite", "15"},
		{"OSM", "15"},
		{"OS_Outdoor", "18"},
		{"OS_Leisure", "9"},
		{"Satellite", "18"},
		{"OSM", "19"},
		{"OS_Outdoor", "20"}
	};
	
	private static class MapConfig {
		final String name;
		final String baseUrl;
		final boolean needsApiKey;
		final int minZoom;
		final int maxZoom;
		final boolean is27700; // Uses British National Grid projection
		final String attribution;
		
		MapConfig(String name, String baseUrl, boolean needsApiKey, int minZoom, int maxZoom, boolean is27700, String attribution) {
			this.name = name;
			this.baseUrl = baseUrl;
			this.needsApiKey = needsApiKey;
			this.minZoom = minZoom;
			this.maxZoom = maxZoom;
			this.is27700 = is27700;
			this.attribution = attribution;
		}
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.trigosmap);
		// Ensure we have a menu
		invalidateOptionsMenu();

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
		mLat = lat;
		mLon = lon;
		c.close();
		
		Log.d(TAG, "Generating cached map images for lat: " + lat + ", lon: " + lon);
		
		// Generate cached images and get file paths
		generateCachedImages(lat, lon);
	}
	
	private void generateCachedImages(double lat, double lon) {
		mNextPosition.set(0);
		// Calculate total expected images from explicit selections
		int expectedImageCount = MAP_SELECTIONS.length;
		
		Log.d(TAG, "Expecting " + expectedImageCount + " total images");
		
		// Create adapter with placeholders immediately and show gallery
		mAdapter = TrigDetailsOSMapAdapter.createWithPlaceholders(this, expectedImageCount);
		setupGallery();
		mAdapter.notifyDataSetChanged();
		
		// Start generating images progressively based on explicit selections
		for (String[] sel : MAP_SELECTIONS) {
			final String selName = sel[0];
			final int selZoom = Integer.parseInt(sel[1]);
			final MapConfig finalConfig = findMapConfigByName(selName);
			if (finalConfig == null) {
				Log.w(TAG, "Unknown map selection name: " + selName);
				continue;
			}
			generateTileBasedImage(mTrigId, lat, lon, finalConfig, selZoom)
				.thenAccept(imagePath -> {
					if (imagePath != null) {
						int position = mNextPosition.getAndIncrement();
						mMainHandler.post(() -> {
							mAdapter.updateImageAtPosition(position, imagePath);
							int remaining = mAdapter.getPendingCount();
							Log.d(TAG, "Updated position " + position + ", " + remaining + " images remaining");
							if (remaining == 0) {
								Log.d(TAG, "All images loaded!");
								Toast.makeText(this, "All map images loaded", Toast.LENGTH_SHORT).show();
							}
						});
					} else {
						Log.w(TAG, "Failed to generate image for " + finalConfig.name + " zoom " + selZoom);
					}
				})
				.exceptionally(throwable -> {
					Log.e(TAG, "Error generating image for " + finalConfig.name + " zoom " + selZoom, throwable);
					return null;
				});
		}
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
				
				// Draw scale bar just above attribution area
				finalBitmap = drawScaleBar(finalBitmap, config, lat, zoom);
				// Draw attribution text at the bottom
				finalBitmap = drawAttribution(finalBitmap, config.attribution);
				
				// Add blue circle marker at center of image only in Dev Mode
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
				boolean devMode = prefs.getBoolean("dev_mode", false);
				if (devMode) {
					finalBitmap = addCenterMarker(finalBitmap);
				}
				
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
	
	/**
	 * Adds a small blue circle marker at the center of the image to indicate trigpoint location
	 * @param originalBitmap The bitmap to add the marker to
	 * @return A new bitmap with the blue circle marker added
	 */
	private Bitmap addCenterMarker(Bitmap originalBitmap) {
		// Create a mutable copy of the bitmap
		Bitmap markedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
		
		// Create canvas to draw on the bitmap
		Canvas canvas = new Canvas(markedBitmap);
		
		// Calculate circle parameters
		int imageWidth = markedBitmap.getWidth();
		int imageHeight = markedBitmap.getHeight();
		float centerX = imageWidth / 2.0f;
		float centerY = imageHeight / 2.0f;
		float circleRadius = imageWidth * 0.025f; // 5% of image width diameter = 2.5% radius
		
		// Create paint for the blue circle outline only
		Paint circlePaint = new Paint();
		circlePaint.setColor(0xFF0066CC); // Blue color
		circlePaint.setStyle(Paint.Style.STROKE);
		circlePaint.setStrokeWidth(1.5f); // Half the previous thickness (3.0f -> 1.5f)
		circlePaint.setAntiAlias(true);
		
		// Draw only the outline (no fill for transparent interior)
		canvas.drawCircle(centerX, centerY, circleRadius, circlePaint);
		
		Log.d(TAG, String.format("Added center marker: circle at (%.1f, %.1f) radius %.1f", 
			centerX, centerY, circleRadius));
		
		return markedBitmap;
	}

	private Bitmap drawAttribution(Bitmap originalBitmap, String attribution) {
		if (attribution == null || attribution.trim().isEmpty()) return originalBitmap;
		Bitmap markedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
		Canvas canvas = new Canvas(markedBitmap);
		Paint textPaint = new Paint();
		textPaint.setColor(0xCC000000); // semi-transparent black
		textPaint.setAntiAlias(true);
		textPaint.setTextSize(dpToPxF(10f * 4f / 9f));
		textPaint.setTextAlign(Paint.Align.LEFT);
		
		// White background strip for readability
		Paint bgPaint = new Paint();
		bgPaint.setColor(0x80FFFFFF);
		
		float padding = dpToPx(4);
		float textHeight = Math.abs(textPaint.ascent() + textPaint.descent());
		float y = markedBitmap.getHeight() - padding;
		float bgTop = y - textHeight - padding;
		canvas.drawRect(0, bgTop, markedBitmap.getWidth(), markedBitmap.getHeight(), bgPaint);
		canvas.drawText(attribution, padding, y - textPaint.descent(), textPaint);
		return markedBitmap;
	}

	private Bitmap drawScaleBar(Bitmap originalBitmap, MapConfig config, double lat, int zoom) {
		try {
			Bitmap bmp = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
			Canvas canvas = new Canvas(bmp);
			
			// Compute meters-per-pixel
			double metersPerPixel;
			if (config.is27700) {
				int z = zoom;
				if (z < 0) z = 0;
				if (z >= OSGB_RESOLUTIONS.length) z = OSGB_RESOLUTIONS.length - 1;
				metersPerPixel = OSGB_RESOLUTIONS[z];
			} else {
				metersPerPixel = 156543.03392 * Math.cos(Math.toRadians(lat)) / Math.pow(2.0, zoom);
			}
			
			int width = bmp.getWidth();
			int height = bmp.getHeight();
			
			// Determine available horizontal length (aim ~50% of width)
			float targetPx = width * 0.5f;
			double targetMeters = targetPx * metersPerPixel;
			
			// Choose a nice rounded length (1,2,5 * 10^n)
			double niceMeters = chooseNiceScale(targetMeters);
			float barPx = (float)(niceMeters / metersPerPixel);
			
			// Calculate vertical position just above attribution strip height
			Paint attrPaint = new Paint();
			attrPaint.setAntiAlias(true);
			attrPaint.setTextSize(dpToPxF(10f * 4f / 9f));
			float pad = dpToPx(4);
			float textHeight = Math.abs(attrPaint.ascent() + attrPaint.descent());
			float stripHeight = textHeight + pad; // attribution will add another bottom pad
			float gap = dpToPx(4);
			float barY = height - stripHeight - gap;
			
			// Geometry and paints
			float barX = dpToPx(8);
			float stroke = Math.max(1f, dpToPxF(1.5f));
			Paint barPaint = new Paint();
			barPaint.setAntiAlias(true);
			barPaint.setColor(0xFFB3B3B3); // ~70% gray
			barPaint.setStrokeWidth(stroke);
			barPaint.setStyle(Paint.Style.STROKE);
			
			// Background rectangle behind label and bar for legibility
			String label = formatDistance(niceMeters);
			Paint labelPaint = new Paint();
			labelPaint.setAntiAlias(true);
			labelPaint.setColor(0xFF8C8C8C); // ~55% gray (darker than bar) for more punch
			labelPaint.setTextSize(dpToPxF(7f));
			labelPaint.setTextAlign(Paint.Align.CENTER);
			float tick = dpToPxF(6f);
			// Nudge the bar up by a fraction of the tick height to avoid attribution overlap
			barY -= (tick / 3f);
			float labelY = barY - dpToPxF(2f);
			float labelHeight = Math.abs(labelPaint.ascent() + labelPaint.descent());
			Paint bgPaint = new Paint();
			bgPaint.setColor(0x80FFFFFF);
			float bgPad = dpToPxF(4f);
			float bgTop = labelY - labelHeight - bgPad;
			float bgBottom = barY + (dpToPxF(6f) / 2f) + bgPad;
			float bgLeft = barX - bgPad;
			float bgRight = barX + barPx + bgPad;
			canvas.drawRect(bgLeft, bgTop, bgRight, bgBottom, bgPaint);
			
			// Draw scale bar line over background
			// Main bar
			canvas.drawLine(barX, barY, barX + barPx, barY, barPaint);
			// End ticks
			canvas.drawLine(barX, barY - tick/2f, barX, barY + tick/2f, barPaint);
			canvas.drawLine(barX + barPx, barY - tick/2f, barX + barPx, barY + tick/2f, barPaint);
			
			// Label
			canvas.drawText(label, barX + barPx / 2f, labelY, labelPaint);
			
			return bmp;
		} catch (Exception e) {
			Log.w(TAG, "drawScaleBar failed", e);
			return originalBitmap;
		}
	}

	private double chooseNiceScale(double targetMeters) {
		if (targetMeters <= 0) return 1;
		double exponent = Math.floor(Math.log10(targetMeters));
		double base = Math.pow(10, exponent);
		double[] candidates = new double[]{1, 2, 5};
		double best = base;
		for (double c : candidates) {
			double v = c * base;
			if (v <= targetMeters) best = v;
		}
		return best;
	}

	private String formatDistance(double meters) {
		if (meters >= 1000.0) {
			double km = meters / 1000.0;
			if (km >= 10) return ((int)Math.round(km)) + " km";
			return String.format(java.util.Locale.UK, "%.1f km", km);
		} else {
			if (meters >= 100) return ((int)Math.round(meters)) + " m";
			return ((int)Math.round(meters)) + " m";
		}
	}
	
	private void setupGallery() {
		RecyclerView gallery = findViewById(R.id.trigosgallery);
		
		// Use the instance adapter (either newly created or existing)
		if (mAdapter != null) {
			// Use GridLayoutManager for vertical scrolling grid layout
			// Calculate number of columns based on screen width for optimal thumbnail size
			int screenWidth = getResources().getDisplayMetrics().widthPixels;
			int columns = Math.max(2, Math.min(3, screenWidth / 500)); // 2-3 columns based on screen width (higher threshold for 2 columns)
			
			gallery.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, columns));
			// Disable nested scrolling so parent scroll view handles scroll
			gallery.setNestedScrollingEnabled(false);
			gallery.setAdapter(mAdapter);
			
			// Add dp-based grid spacing decoration so columns and rows have visible gaps
			int spacingPx = dpToPx(16); // 16dp spacing between items
			gallery.addItemDecoration(new GridSpacingItemDecoration(columns, spacingPx, false));
			gallery.setClipToPadding(false);
		}
		
		mAdapter.setOnItemClickListener(new TrigDetailsOSMapAdapter.OnItemClickListener() {
			@Override
			public void onItemClick(int position) {
				// Get the current URL from the adapter (may be placeholder or actual image)
				                                if (mAdapter != null && position < mAdapter.getItemCount()) {
					String url = mAdapter.getUrlAtPosition(position);
					
					// Only allow clicks on actual images (not placeholders)
					if (!"PLACEHOLDER".equals(url)) {
						Intent i = new Intent(TrigDetailsOSMapTab.this, DisplayBitmapActivity.class);
						i.putExtra("URL", url);
						Log.i(TAG, "Clicked OSMap at path: " + url);
						startActivity(i);
					} else {
						Log.d(TAG, "Clicked on placeholder at position " + position + " - ignoring");
						Toast.makeText(TrigDetailsOSMapTab.this, "Image still loading...", Toast.LENGTH_SHORT).show();
					}
				}
			}
		});
	}

	private MapConfig findMapConfigByName(String name) {
		for (MapConfig mc : MAP_CONFIGS) {
			if (mc.name.equals(name)) return mc;
		}
		return null;
	}

	public void refreshImagesFromParent() {
		try {
			File cacheDir = new File(getCacheDir(), "map_images");
			File[] files = cacheDir.listFiles();
			if (files != null) {
				for (File f : files) {
					String name = f.getName();
					if (name.startsWith("trig_" + mTrigId + "_")) {
						// noinspection ResultOfMethodCallIgnored
						f.delete();
					}
				}
			}
			generateCachedImages(mLat, mLon);
		} catch (Exception e) {
			Log.w(TAG, "Failed to refresh images from parent", e);
		}
	}

	/**
	 * Converts dp to pixels for consistent spacing on different densities
	 */
	private int dpToPx(int dp) {
		float density = getResources().getDisplayMetrics().density;
		return Math.round(dp * density);
	}

	private float dpToPxF(float dp) {
		float density = getResources().getDisplayMetrics().density;
		return dp * density;
	}

	/**
	 * ItemDecoration that adds even spacing around grid items so columns and rows have gaps.
	 * If includeEdge is true, outer edges will also have spacing, matching inner gaps visually.
	 */
	private static class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
		private final int spanCount;
		private final int spacing;
		private final boolean includeEdge;

		GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
			this.spanCount = spanCount;
			this.spacing = spacing;
			this.includeEdge = includeEdge;
		}

		@Override
		public void getItemOffsets(android.graphics.Rect outRect, android.view.View view,
				RecyclerView parent, RecyclerView.State state) {
			int position = parent.getChildAdapterPosition(view); // item position
			int column = position % spanCount; // item column

			if (includeEdge) {
				outRect.left = spacing - column * spacing / spanCount;
				outRect.right = (column + 1) * spacing / spanCount;
				if (position < spanCount) { // top edge
					outRect.top = spacing;
				}
				outRect.bottom = spacing; // item bottom
			} else {
				outRect.left = column * spacing / spanCount;
				outRect.right = spacing - (column + 1) * spacing / spanCount;
				if (position >= spanCount) {
					outRect.top = spacing; // item top
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.trigosmap_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_refresh_osmaps) {
			File cacheDir = new File(getCacheDir(), "map_images");
			File[] files = cacheDir.listFiles();
			if (files != null) {
				for (File f : files) {
					String name = f.getName();
					if (name.startsWith("trig_" + mTrigId + "_")) {
						// noinspection ResultOfMethodCallIgnored
						f.delete();
					}
				}
			}
			Toast.makeText(this, "Cleared cached OS map images for this trigpoint", Toast.LENGTH_SHORT).show();
			generateCachedImages(mLat, mLon);
			return true;
		}
		return super.onOptionsItemSelected(item);
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