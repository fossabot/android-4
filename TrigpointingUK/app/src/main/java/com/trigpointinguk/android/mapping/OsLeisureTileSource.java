package com.trigpointinguk.android.mapping;

import android.util.Log;

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.util.MapTileIndex;

/**
 * Ordnance Survey Leisure tiles (OSGB 27700) from OS Data Hub.
 * Requires an API key. Coverage: Great Britain only.
 * Note: This layer uses British National Grid (EPSG:27700) tiling. It may not align with Web Mercator.
 */
public class OsLeisureTileSource extends OnlineTileSourceBase {
    private static final String TAG = "OsLeisureTileSource";
    private final String apiKey;

    public OsLeisureTileSource(String apiKey) {
        super("OS_Leisure_27700",
                6, // min zoom where tiles look reasonable
                19, // max zoom
                256,
                ".png",
                new String[]{"https://api.os.uk/maps/raster/v1/zxy/Leisure_27700/"});
        this.apiKey = apiKey;
    }

    @Override
    public String getTileURLString(final long pMapTileIndex) {
        final int z = MapTileIndex.getZoom(pMapTileIndex);
        final int x = MapTileIndex.getX(pMapTileIndex);
        final int y = MapTileIndex.getY(pMapTileIndex);
        // URL pattern per OS Data Hub (z/x/y.png?key=...)
        // Key as query parameter per docs example
        String url = getBaseUrl() + z + "/" + x + "/" + y + mImageFilenameEnding + "?key=" + apiKey;
        try {
            // Avoid logging full key
            String maskedKey = apiKey.length() > 6 ? apiKey.substring(0, 3) + "…" + apiKey.substring(apiKey.length()-3) : "***";
            Log.d(TAG, "Requesting OS tile: z=" + z + " x=" + x + " y=" + y + " key=" + maskedKey);
        } catch (Exception ignore) {}
        return url;
    }
}

