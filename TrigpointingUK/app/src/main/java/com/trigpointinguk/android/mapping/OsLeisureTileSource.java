package com.trigpointinguk.android.mapping;

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.util.MapTileIndex;

/**
 * Ordnance Survey Leisure tiles (Web Mercator 3857) from OS Data Hub.
 * Requires an API key. Coverage: Great Britain only.
 * The Leisure_3857 style provides leisure cartography with appropriate scale rendering.
 */
public class OsLeisureTileSource extends OnlineTileSourceBase {
    private final String apiKey;

    public OsLeisureTileSource(String apiKey) {
        super("OS_Leisure_3857",
                6, // min zoom where tiles look reasonable
                19, // max zoom
                256,
                ".png",
                new String[]{"https://api.os.uk/maps/raster/v1/zxy/Leisure_3857/"});
        this.apiKey = apiKey;
    }

    @Override
    public String getTileURLString(final long pMapTileIndex) {
        final int z = MapTileIndex.getZoom(pMapTileIndex);
        final int x = MapTileIndex.getX(pMapTileIndex);
        final int y = MapTileIndex.getY(pMapTileIndex);
        // URL pattern per OS Data Hub (z/x/y.png?key=...)
        return getBaseUrl() + z + "/" + x + "/" + y + mImageFilenameEnding + "?key=" + apiKey;
    }
}

