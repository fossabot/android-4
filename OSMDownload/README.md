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
# Download zoom levels 0-10 (warning: this is a lot of tiles!)
python osm_tile_downloader.py --min-zoom 0 --max-zoom 10

# Download a smaller range for testing
python osm_tile_downloader.py --min-zoom 0 --max-zoom 5

# Resume download starting from tile 1000, download only 500 more tiles
python osm_tile_downloader.py --min-zoom 0 --max-zoom 10 --start-tile 1000 --limit 500

# Download with slower rate limiting (be more respectful)
python osm_tile_downloader.py --min-zoom 8 --max-zoom 12 --min-delay 1.0 --max-delay 3.0
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

## Understanding Tile Counts

The number of tiles grows exponentially with zoom level:

| Zoom Level | Tiles at This Level | Total Tiles (0 to Z) |
|------------|--------------------|--------------------|
| 0          | 1                  | 1                  |
| 1          | 4                  | 5                  |
| 2          | 16                 | 21                 |
| 3          | 64                 | 85                 |
| 4          | 256                | 341                |
| 5          | 1,024              | 1,365              |
| 6          | 4,096              | 5,461              |
| 7          | 16,384             | 21,845             |
| 8          | 65,536             | 87,381             |
| 9          | 262,144            | 349,525            |
| 10         | 1,048,576          | 1,398,101          |

**Important**: High zoom levels require significant storage space and download time. Start with lower zoom levels for testing.

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
