#!/usr/bin/env python3
"""
Test script for OSM Tile Downloader
Downloads a small set of tiles for testing purposes
"""

import subprocess
import sys
import os
from pathlib import Path

def run_test():
    """Run a small download test."""
    print("Testing OSM Tile Downloader...")
    print("=" * 50)
    
    # Test 1: Download just zoom level 0 (1 tile)
    print("\nTest 1: Downloading zoom level 0 (1 tile) from OSM")
    script_dir = Path(__file__).parent
    downloader_script = str(script_dir / "osm_tile_downloader.py")
    result = subprocess.run([
        sys.executable, downloader_script,
        "--provider", "osm",
        "--min-zoom", "0",
        "--max-zoom", "0",
        "--limit", "5"
    ], capture_output=True, text=True, cwd=str(script_dir))
    
    if result.returncode == 0:
        print("✓ Test 1 passed")
    else:
        print("✗ Test 1 failed:")
        print(result.stderr)
        return False
    
    # Test 2: Show statistics
    print("\nTest 2: Checking statistics")
    script_dir = Path(__file__).parent
    downloader_script = str(script_dir / "osm_tile_downloader.py")
    result = subprocess.run([
        sys.executable, downloader_script,
        "--stats"
    ], capture_output=True, text=True, cwd=str(script_dir))
    
    if result.returncode == 0:
        print("✓ Test 2 passed")
        print("Statistics output:")
        print(result.stdout)
    else:
        print("✗ Test 2 failed:")
        print(result.stderr)
        return False
    
    # Test 3: Check if tile files exist
    print("\nTest 3: Verifying tile files exist")
    tile_path = Path(__file__).parent / "tiles/OpenStreetMap/0/0/0.png"
    if tile_path.exists() and tile_path.stat().st_size > 0:
        print(f"✓ Test 3 passed - tile exists: {tile_path}")
        print(f"  File size: {tile_path.stat().st_size} bytes")
    else:
        print("✗ Test 3 failed - tile file not found or empty")
        return False
    
    print("\n" + "=" * 50)
    print("All tests passed! 🎉")
    print("\nTo create a ZIP file for testing:")
    print("  cd tiles && zip -r ../test_tiles.zip . && cd ..")
    print("\nTo clean up test files:")
    print("  rm -rf tiles/ download.log")
    
    return True

if __name__ == "__main__":
    if not run_test():
        sys.exit(1)
