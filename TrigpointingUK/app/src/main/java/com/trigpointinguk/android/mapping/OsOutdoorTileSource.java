package com.trigpointinguk.android.mapping;

import android.util.Log;

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.util.MapTileIndex;

/**
 * Ordnance Survey Outdoor tiles (Web Mercator 3857) from OS Data Hub.
 * Requires an API key. Coverage: Great Britain only.
 */
public class OsOutdoorTileSource extends OnlineTileSourceBase {
    private static final String TAG = "OsOutdoorTileSource";
    private final String apiKey;

    public OsOutdoorTileSource(String apiKey) {
        super("OS_Outdoor_3857",
                6, // min zoom where tiles look reasonable
                19, // max zoom
                256,
                ".png",
                new String[]{"https://api.os.uk/maps/raster/v1/zxy/Outdoor_3857/"});
        this.apiKey = apiKey;
    }

    @Override
    public String getTileURLString(final long pMapTileIndex) {
        final int z = MapTileIndex.getZoom(pMapTileIndex);
        final int x = MapTileIndex.getX(pMapTileIndex);
        final int y = MapTileIndex.getY(pMapTileIndex);
        String url = getBaseUrl() + z + "/" + x + "/" + y + mImageFilenameEnding + "?key=" + apiKey;
        try {
            String maskedKey = apiKey.length() > 6 ? apiKey.substring(0, 3) + "…" + apiKey.substring(apiKey.length() - 3) : "***";
            Log.d(TAG, "Requesting OS Outdoor tile: z=" + z + " x=" + x + " y=" + y + " key=" + maskedKey);
        } catch (Exception ignore) {}
        return url;
    }
}


