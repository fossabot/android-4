package com.trigpointinguk.android.mapping;

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;

/**
 * MapQuest tile source using the provided API key
 */
public class MapQuestTileSource extends OnlineTileSourceBase {
    
    private static final String MAPQUEST_API_KEY = "2S9I5XjGUjdeGI9IH7Vqx21DYLSzU5pK";
    
    public MapQuestTileSource() {
        super("MapQuest", 1, 18, 256, ".png", 
              new String[]{"https://www.mapquestapi.com/tile/v1/map/"});
    }
    
    @Override
    public String getTileURLString(final long pMapTileIndex) {
        final int zoom = (int) (pMapTileIndex >> 28);
        final int x = (int) ((pMapTileIndex << 4) >> 28);
        final int y = (int) (pMapTileIndex & 0xFFFFFFF);
        return getBaseUrl() + zoom + "/" + x + "/" + y + "?key=" + MAPQUEST_API_KEY;
    }
} 