# OSM Tile Downloader for TrigpointingUK Leaflet Maps

This Python application downloads OpenStreetMap tiles from the Mapnik provider for offline use with Leaflet maps in the TrigpointingUK Android application. **Only downloads tiles that intersect with the UK region** (including Northern Ireland), dramatically reducing download size compared to worldwide tiles.

## Features

- **UK-Specific Region**: Only downloads tiles covering the UK bounding box (-8.5°W to 2.0°E, 49.5°N to 61.0°N)
- **Resumable Downloads**: Automatically skips existing tiles, allowing you to resume interrupted downloads
- **Zoom Level Control**: Specify minimum and maximum zoom levels to download
- **Rate Limiting**: Configurable random delays between requests to be respectful to tile servers
- **Progress Tracking**: Real-time progress reporting with UK tile bounds per zoom level
- **Web Server Ready**: Creates tiles in standard z/x/y.png directory structure
- **Statistics**: View download progress and tile counts by zoom level
- **Efficient Storage**: Dramatically reduced download size vs. worldwide tiles

## Installation

1. Ensure you have Python 3.7+ installed
2. Install dependencies:
```bash
pip install -r requirements.txt
```

## Usage

### Basic Examples

```bash
# Light download - Overview + moderate detail (561 MB)
python osm_tile_downloader.py --min-zoom 0 --max-zoom 12

# Standard download - Covers most mapping needs (2.2 GB)
python osm_tile_downloader.py --min-zoom 0 --max-zoom 13

# Testing - Small download for experimentation (225 KB)
python osm_tile_downloader.py --min-zoom 0 --max-zoom 5

# Resume download starting from tile 1000, download only 500 more tiles
python osm_tile_downloader.py --min-zoom 0 --max-zoom 12 --start-tile 1000 --limit 500

# Download with slower rate limiting (be more respectful)
python osm_tile_downloader.py --min-zoom 8 --max-zoom 12 --min-delay 1.0 --max-delay 3.0

# Heavy usage - High detail (8.9 GB, takes time!)
python osm_tile_downloader.py --min-zoom 0 --max-zoom 14
```

### Check Download Statistics

```bash
python osm_tile_downloader.py --stats
```

### Command Line Options

- `--min-zoom`: Minimum zoom level (default: 0)
- `--max-zoom`: Maximum zoom level (default: 10)
- `--start-tile`: Starting tile number for resuming downloads (default: 0)
- `--limit`: Maximum number of tiles to download in this session (default: unlimited)
- `--min-delay`: Minimum delay between requests in seconds (default: 0.5)
- `--max-delay`: Maximum delay between requests in seconds (default: 2.0)
- `--tiles-dir`: Directory to store tiles (default: tiles)
- `--stats`: Show download statistics and exit

## UK Tile Count Estimates

The downloader only fetches tiles covering the UK region, dramatically reducing download size:

| Zoom | UK Tiles | World Tiles | Reduction | Cumulative UK | Est. Size |
|------|----------|-------------|-----------|---------------|-----------|
| 0    | 1        | 1           | 1x        | 1             | 15 KB     |
| 1    | 2        | 4           | 2x        | 3             | 45 KB     |
| 2    | 2        | 16          | 8x        | 5             | 75 KB     |
| 3    | 2        | 64          | 32x       | 7             | 105 KB    |
| 4    | 4        | 256         | 64x       | 11            | 165 KB    |
| 5    | 4        | 1,024       | 256x      | 15            | 225 KB    |
| 6    | 12       | 4,096       | 341x      | 27            | 405 KB    |
| 7    | 40       | 16,384      | 410x      | 67            | 1.0 MB    |
| 8    | 144      | 65,536      | 455x      | 211           | 3.2 MB    |
| 9    | 480      | 262,144     | 546x      | 691           | 10.4 MB   |
| 10   | 1,829    | 1,048,576   | 573x      | 2,520         | 37.8 MB   |
| 11   | 7,076    | 4,194,304   | 593x      | 9,596         | 144 MB    |
| 12   | 27,840   | 16,777,216  | 603x      | 37,436        | 561 MB    |
| 13   | 111,360  | 67,108,864  | 603x      | 148,796       | 2.2 GB    |
| 14   | 444,033  | 268,435,456 | 605x      | 592,829       | 8.9 GB    |
| 15   | 1,774,278| 1,073,741,824| 605x     | 2,367,107     | 35.5 GB   |
| 16   | 7,091,491| 4,294,967,296| 606x     | 9,458,598     | 142 GB    |

**Practical Recommendations:**
- **Light usage**: Zoom 0-12 (561 MB) - Good for overview and moderate detail
- **Standard usage**: Zoom 0-13 (2.2 GB) - Covers most mapping needs
- **Heavy usage**: Zoom 0-14 (8.9 GB) - High detail for serious users
- **Maximum practical**: Zoom 0-16 (142 GB) - Only for extreme offline needs

**10M Tile Limit**: Cumulative tiles stay under 10 million through zoom 16 (9.46M tiles).

## Output Structure

Tiles are saved in the standard web mapping directory structure:
```
tiles/
├── 0/
│   └── 0/
│       └── 0.png
├── 1/
│   ├── 0/
│   │   ├── 0.png
│   │   └── 1.png
│   └── 1/
│       ├── 0.png
│       └── 1.png
├── 2/
│   ├── 0/
│   │   ├── 0.png
│   │   ├── 1.png
│   │   ├── 2.png
│   │   └── 3.png
...
```

## Creating ZIP Files for Leaflet Cache

Once you have downloaded tiles, create ZIP files for the Android Leaflet cache:

### Method 1: Create a single ZIP file (recommended)
```bash
cd tiles
zip -r ../osm_tiles.zip .
```

### Method 2: Create separate ZIP files by zoom level
```bash
cd tiles
for zoom in */; do
    zip -r "../osm_tiles_zoom_${zoom%/}.zip" "$zoom"
done
```

### Method 3: Create ZIP files for specific zoom ranges
```bash
cd tiles
# Zoom levels 0-8 (low detail for overview)
zip -r ../osm_tiles_low.zip {0..8}

# Zoom levels 9-12 (high detail for close-up)
zip -r ../osm_tiles_high.zip {9..12}
```

## Web Server Setup for Leaflet

Host ZIP files on a web server for the Android app to download:

```bash
# Example: Copy ZIP files to web server
scp osm_tiles*.zip user@yourserver.com:/var/www/html/

# Update Android app arrays.xml with your URLs:
# https://yourserver.com/osm_tiles.zip
# https://yourserver.com/osm_tiles_low.zip
# https://yourserver.com/osm_tiles_high.zip
```

The Android DownloadMapsActivity will extract ZIP files directly into the Leaflet WebView cache for offline use.

## Rate Limiting and Ethics

This downloader includes rate limiting to be respectful to OpenStreetMap's tile servers:

- Default delays: 0.5-2.0 seconds between requests
- Uses proper User-Agent header
- Supports resumable downloads to minimize re-downloading

**Please be respectful**:
- Don't download more tiles than you need
- Use appropriate delays between requests
- Consider supporting OpenStreetMap if you use their tiles extensively

## Troubleshooting

### Download Errors
- Check your internet connection
- Verify the tile server is accessible
- Some tiles may not exist at high zoom levels for certain areas

### Resuming Downloads
- The downloader automatically skips existing tiles
- Use `--start-tile` to resume from a specific point if needed
- Check `--stats` to see current progress

### Storage Space
- Monitor disk space when downloading high zoom levels
- Each tile is typically 5-50 KB depending on content
- Zoom level 10 for the entire world requires ~70 GB

## Logs

Download progress and errors are logged to:
- Console output (real-time)
- `download.log` file (persistent)

Check the log file for detailed error information if downloads fail.
