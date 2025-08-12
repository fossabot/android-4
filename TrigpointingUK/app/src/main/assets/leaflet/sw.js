// Service Worker for Leaflet Tile Caching
// Provides offline-first tile caching with bulk download support

const CACHE_NAME = 'trigpointing-tiles-v1';
const CACHE_VERSION = 1;
const MAX_CACHE_SIZE = 500 * 1024 * 1024; // 500MB cache limit
const TILE_CACHE_DURATION = 30 * 24 * 60 * 60 * 1000; // 30 days

console.log('Service Worker: Loading');

// Install event - setup initial cache
self.addEventListener('install', event => {
  console.log('Service Worker: Installing');
  
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => {
      console.log('Service Worker: Cache opened');
      return cache;
    })
  );
  
  // Skip waiting to activate immediately
  self.skipWaiting();
});

// Activate event - cleanup old caches
self.addEventListener('activate', event => {
  console.log('Service Worker: Activating');
  
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames
          .filter(cacheName => cacheName !== CACHE_NAME)
          .map(cacheName => {
            console.log('Service Worker: Deleting old cache:', cacheName);
            return caches.delete(cacheName);
          })
      );
    })
  );
  
  // Claim all clients immediately
  return self.clients.claim();
});

// Fetch event - handle tile requests with caching
self.addEventListener('fetch', event => {
  const url = event.request.url;
  
  // Only intercept tile requests (containing tile server patterns)
  if (isTileRequest(url)) {
    event.respondWith(handleTileRequest(event.request));
  }
});

// Check if URL is a tile request
function isTileRequest(url) {
  // Match common tile URL patterns
  const tilePatterns = [
    /\/\d+\/\d+\/\d+\.png/,           // Standard z/x/y.png
    /\/\d+\/\d+\/\d+\.jpg/,           // Standard z/x/y.jpg  
    /tile\.openstreetmap\.org/,       // OSM tiles
    /api\.os\.uk.*\/wmts/,            // OS tiles
    /otile.*\.mqcdn\.com/,            // MapQuest tiles
    /\{z\}|\{x\}|\{y\}/              // Template URLs
  ];
  
  return tilePatterns.some(pattern => pattern.test(url));
}

// Handle tile requests with cache-first strategy
async function handleTileRequest(request) {
  const cache = await caches.open(CACHE_NAME);
  
  try {
    // Try cache first
    const cachedResponse = await cache.match(request);
    
    if (cachedResponse) {
      console.log('Service Worker: Serving from cache:', request.url);
      
      // Check if cached tile is still fresh
      const cacheDate = new Date(cachedResponse.headers.get('sw-cached-date') || 0);
      const isExpired = Date.now() - cacheDate.getTime() > TILE_CACHE_DURATION;
      
      if (!isExpired) {
        return cachedResponse;
      } else {
        console.log('Service Worker: Cached tile expired, fetching fresh:', request.url);
      }
    }
    
    // Fetch from network
    console.log('Service Worker: Fetching from network:', request.url);
    const networkResponse = await fetch(request);
    
    if (networkResponse.ok) {
      // Clone response for caching
      const responseToCache = networkResponse.clone();
      
      // Add cache headers
      const headers = new Headers(responseToCache.headers);
      headers.set('sw-cached-date', new Date().toISOString());
      
      const cachedResponse = new Response(await responseToCache.blob(), {
        status: responseToCache.status,
        statusText: responseToCache.statusText,
        headers: headers
      });
      
      // Cache the response
      await cache.put(request, cachedResponse);
      console.log('Service Worker: Cached tile:', request.url);
      
      // Manage cache size
      manageCacheSize();
      
      return networkResponse;
    } else {
      console.log('Service Worker: Network request failed:', request.url);
      
      // Return cached version even if expired as fallback
      if (cachedResponse) {
        console.log('Service Worker: Serving expired cache as fallback');
        return cachedResponse;
      }
      
      return networkResponse;
    }
  } catch (error) {
    console.log('Service Worker: Fetch error:', error);
    
    // Try to return cached version as fallback
    const cachedResponse = await cache.match(request);
    if (cachedResponse) {
      console.log('Service Worker: Serving cache due to network error');
      return cachedResponse;
    }
    
    // Return a basic error response
    return new Response('Tile not available offline', { 
      status: 404,
      statusText: 'Not Found'
    });
  }
}

// Manage cache size to prevent unlimited growth
async function manageCacheSize() {
  try {
    const cache = await caches.open(CACHE_NAME);
    const cacheSize = await calculateCacheSize(cache);
    
    if (cacheSize > MAX_CACHE_SIZE) {
      console.log('Service Worker: Cache size exceeded, cleaning up...');
      await cleanupOldestTiles(cache);
    }
  } catch (error) {
    console.log('Service Worker: Cache management error:', error);
  }
}

// Calculate total cache size
async function calculateCacheSize(cache) {
  const requests = await cache.keys();
  let totalSize = 0;
  
  for (const request of requests) {
    try {
      const response = await cache.match(request);
      if (response) {
        const blob = await response.blob();
        totalSize += blob.size;
      }
    } catch (error) {
      console.log('Service Worker: Error calculating size for:', request.url);
    }
  }
  
  return totalSize;
}

// Remove oldest cached tiles to free space
async function cleanupOldestTiles(cache) {
  const requests = await cache.keys();
  const tilesWithDates = [];
  
  // Get cache dates for all tiles
  for (const request of requests) {
    try {
      const response = await cache.match(request);
      if (response) {
        const cacheDate = new Date(response.headers.get('sw-cached-date') || 0);
        tilesWithDates.push({ request, date: cacheDate });
      }
    } catch (error) {
      console.log('Service Worker: Error reading cache date for:', request.url);
    }
  }
  
  // Sort by date (oldest first)
  tilesWithDates.sort((a, b) => a.date - b.date);
  
  // Remove oldest 25% of tiles
  const tilesToRemove = Math.floor(tilesWithDates.length * 0.25);
  
  for (let i = 0; i < tilesToRemove; i++) {
    await cache.delete(tilesWithDates[i].request);
    console.log('Service Worker: Removed old tile:', tilesWithDates[i].request.url);
  }
  
  console.log(`Service Worker: Cleaned up ${tilesToRemove} old tiles`);
}

// Message handler for bulk download operations
self.addEventListener('message', event => {
  const { type, data } = event.data;
  
  switch (type) {
    case 'BULK_DOWNLOAD':
      handleBulkDownload(data.url, data.options)
        .then(result => {
          event.ports[0].postMessage({ success: true, result });
        })
        .catch(error => {
          event.ports[0].postMessage({ success: false, error: error.message });
        });
      break;
      
    case 'CACHE_STATUS':
      getCacheStatus()
        .then(status => {
          event.ports[0].postMessage({ success: true, status });
        })
        .catch(error => {
          event.ports[0].postMessage({ success: false, error: error.message });
        });
      break;
      
    case 'CLEAR_CACHE':
      clearTileCache()
        .then(() => {
          event.ports[0].postMessage({ success: true });
        })
        .catch(error => {
          event.ports[0].postMessage({ success: false, error: error.message });
        });
      break;
  }
});

// Handle bulk download from ZIP file
async function handleBulkDownload(zipUrl, options = {}) {
  console.log('Service Worker: Starting bulk download from:', zipUrl);
  
  try {
    // Download ZIP file
    const response = await fetch(zipUrl);
    if (!response.ok) {
      throw new Error(`Failed to download ZIP: ${response.status}`);
    }
    
    const zipBlob = await response.blob();
    console.log('Service Worker: Downloaded ZIP file, size:', zipBlob.size);
    
    // Note: ZIP processing would require a ZIP library
    // For now, return success - will implement ZIP processing in next step
    return {
      message: 'Bulk download initiated',
      size: zipBlob.size
    };
    
  } catch (error) {
    console.log('Service Worker: Bulk download error:', error);
    throw error;
  }
}

// Get cache status information
async function getCacheStatus() {
  const cache = await caches.open(CACHE_NAME);
  const requests = await cache.keys();
  const cacheSize = await calculateCacheSize(cache);
  
  return {
    tileCount: requests.length,
    totalSize: cacheSize,
    maxSize: MAX_CACHE_SIZE,
    usagePercent: Math.round((cacheSize / MAX_CACHE_SIZE) * 100)
  };
}

// Clear all cached tiles
async function clearTileCache() {
  console.log('Service Worker: Clearing tile cache');
  const cache = await caches.open(CACHE_NAME);
  const requests = await cache.keys();
  
  for (const request of requests) {
    await cache.delete(request);
  }
  
  console.log(`Service Worker: Cleared ${requests.length} cached tiles`);
}
