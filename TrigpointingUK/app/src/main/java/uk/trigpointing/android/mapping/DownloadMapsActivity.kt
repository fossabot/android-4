package uk.trigpointing.android.mapping

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import uk.trigpointing.android.R
import uk.trigpointing.android.common.BaseActivity
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.DecimalFormat
import java.util.zip.GZIPInputStream

class DownloadMapsActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: MapDownloadAdapter
    private lateinit var cacheUsageText: TextView
    private lateinit var clearCacheLink: TextView
    private lateinit var tileCacheDir: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download_maps)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize tile cache directory
        tileCacheDir = File(cacheDir, "map_tiles")
        if (!tileCacheDir.exists()) {
            tileCacheDir.mkdirs()
        }

        // Initialize UI components
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        cacheUsageText = findViewById(R.id.cacheUsageText)
        clearCacheLink = findViewById(R.id.clearCacheLink)
        
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Set up cache usage display and clear link
        setupCacheUsage()
        
        fetchMapDownloads()
    }

    override fun onResume() {
        super.onResume()
        // Refresh cache usage when returning to the activity
        // This ensures the display is updated after the user clears cache via system settings
        setupCacheUsage()
    }

    private fun fetchMapDownloads() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(YAML_URL).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val yamlString = response.body?.string()
                    val mapper = ObjectMapper(YAMLFactory())
                    val list = mapper.readValue(yamlString, MapDownload.MapDownloadsList::class.java)
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        adapter = MapDownloadAdapter(list.maps) { mapDownload ->
                            downloadAndExtract(mapDownload)
                        }
                        recyclerView.adapter = adapter
                    }
                } else {
                    showError("Failed to load map list")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch YAML", e)
                showError("Failed to load map list")
            }
        }
    }

    private fun downloadAndExtract(mapDownload: MapDownload) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(mapDownload.fileUrl).build()
                val response = client.newCall(request).execute()
                val body = response.body

                if (response.isSuccessful && body != null) {
                    val totalSize = body.contentLength()
                    var downloadedBytes = 0L
                    var extractedFileCount = 0
                    
                    Log.d(TAG, "Starting download of ${mapDownload.name}, total size: $totalSize bytes")

                    body.byteStream().use { inputStream ->
                        // Create a counting input stream to track actual bytes read
                        val countingInputStream = object : InputStream() {
                            override fun read(): Int {
                                val byte = inputStream.read()
                                if (byte != -1) {
                                    downloadedBytes++
                                    // Update progress every 64KB to avoid too many UI updates
                                    if (downloadedBytes % 65536 == 0L || downloadedBytes == totalSize) {
                                        val progress = if (totalSize > 0) {
                                            ((downloadedBytes * 100) / totalSize).toInt().coerceIn(0, 100)
                                        } else 0
                                        
                                        lifecycleScope.launch(Dispatchers.Main) {
                                            adapter.updateProgress(mapDownload, progress)
                                        }
                                    }
                                }
                                return byte
                            }
                            
                            override fun read(b: ByteArray, off: Int, len: Int): Int {
                                val bytesRead = inputStream.read(b, off, len)
                                if (bytesRead > 0) {
                                    downloadedBytes += bytesRead
                                    // Update progress every 64KB to avoid too many UI updates
                                    if (downloadedBytes % 65536 < bytesRead || downloadedBytes == totalSize) {
                                        val progress = if (totalSize > 0) {
                                            ((downloadedBytes * 100) / totalSize).toInt().coerceIn(0, 100)
                                        } else 0
                                        
                                        lifecycleScope.launch(Dispatchers.Main) {
                                            adapter.updateProgress(mapDownload, progress)
                                        }
                                    }
                                }
                                return bytesRead
                            }
                        }
                        
                        BufferedInputStream(countingInputStream).use { bufferedInputStream ->
                            // Determine if we need to decompress the stream based on the type field
                            val archiveInputStream = when (mapDownload.type) {
                                "tgz" -> {
                                    Log.d(TAG, "Processing tgz file, applying gzip decompression")
                                    GZIPInputStream(bufferedInputStream)
                                }
                                "tar" -> {
                                    Log.d(TAG, "Processing tar file, no decompression needed")
                                    bufferedInputStream
                                }
                                else -> {
                                    Log.w(TAG, "Unknown file type: ${mapDownload.type}, treating as tar")
                                    bufferedInputStream
                                }
                            }
                            
                            TarArchiveInputStream(archiveInputStream).use { tarInput ->
                                var entry = tarInput.nextTarEntry
                                while (entry != null) {
                                    if (!entry.isDirectory) {
                                        val outputFile = File(cacheDir, entry.name)
                                        outputFile.parentFile?.mkdirs()
                                        FileOutputStream(outputFile).use { fos ->
                                            val buffer = ByteArray(4096)
                                            var len: Int
                                            while (tarInput.read(buffer).also { len = it } != -1) {
                                                fos.write(buffer, 0, len)
                                            }
                                        }
                                        extractedFileCount++
                                        Log.v(TAG, "Extracted file ${extractedFileCount}: ${entry.name}")
                                    }
                                    entry = tarInput.nextTarEntry
                                }
                            }
                        }
                    }
                    
                    Log.d(TAG, "Download complete: ${mapDownload.name}, extracted $extractedFileCount files")
                    
                    withContext(Dispatchers.Main) {
                        // Update cache usage display
                        setupCacheUsage()
                        
                        Toast.makeText(this@DownloadMapsActivity, "${mapDownload.name} download complete! Extracted $extractedFileCount files.", Toast.LENGTH_LONG).show()
                        adapter.updateProgress(mapDownload, 100) // Mark as complete
                        // Don't automatically close activity - let user see completion and navigate back manually
                    }
                } else {
                    showError("Download failed - HTTP ${response.code}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download/Extraction failed for ${mapDownload.name}", e)
                showError("Download failed: ${e.message}")
            }
        }
    }

    private suspend fun showError(message: String) {
        withContext(Dispatchers.Main) {
            progressBar.visibility = View.GONE
            Toast.makeText(this@DownloadMapsActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private class MapDownloadAdapter(
        private val mapDownloads: List<MapDownload>,
        private val onDownloadClick: (MapDownload) -> Unit
    ) : RecyclerView.Adapter<MapDownloadAdapter.ViewHolder>() {

        private val progressMap = mutableMapOf<String, Int>()

        fun updateProgress(mapDownload: MapDownload, progress: Int) {
            progressMap[mapDownload.name] = progress
            val index = mapDownloads.indexOf(mapDownload)
            if (index != -1) {
                notifyItemChanged(index)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_map_download, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val mapDownload = mapDownloads[position]
            holder.bind(mapDownload, progressMap.getOrDefault(mapDownload.name, -1))
        }

        override fun getItemCount() = mapDownloads.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val mapName: TextView = itemView.findViewById(R.id.mapName)
            private val mapDescription: TextView = itemView.findViewById(R.id.mapDescription)
            private val mapSize: TextView = itemView.findViewById(R.id.mapSize)
            private val downloadButton: Button = itemView.findViewById(R.id.downloadButton)
            private val downloadProgressBar: ProgressBar = itemView.findViewById(R.id.downloadProgressBar)

            fun bind(mapDownload: MapDownload, progress: Int) {
                mapName.text = mapDownload.name
                mapDescription.text = mapDownload.description
                val df = DecimalFormat("#.##")
                val sizeInMB = df.format(mapDownload.fileSize.toDouble() / (1024 * 1024))
                mapSize.text = "Size: $sizeInMB MB"

                when {
                    progress in 0..99 -> {
                        downloadProgressBar.visibility = View.VISIBLE
                        downloadProgressBar.progress = progress
                        downloadButton.isEnabled = false
                        downloadButton.text = "Downloading..."
                    }
                    progress >= 100 -> {
                        downloadProgressBar.visibility = View.GONE
                        downloadButton.isEnabled = false
                        downloadButton.text = "Downloaded"
                    }
                    else -> {
                        downloadProgressBar.visibility = View.GONE
                        downloadButton.isEnabled = true
                        downloadButton.text = "Download"
                    }
                }

                downloadButton.setOnClickListener {
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        onDownloadClick(mapDownloads[adapterPosition])
                    }
                }
            }
        }
    }

    private fun setupCacheUsage() {
        // Calculate and display cache usage in background
        lifecycleScope.launch(Dispatchers.IO) {
            val stats = getDirectoryStats(tileCacheDir)
            val totalSize = stats[0]
            val fileCount = stats[1]

            withContext(Dispatchers.Main) {
                updateCacheUsageDisplay(totalSize, fileCount)
            }
        }

        // Set up clear cache link click handler
        clearCacheLink.setOnClickListener {
            openAppSettings()
        }
    }

    private fun updateCacheUsageDisplay(totalSize: Long, fileCount: Long) {
        val df = DecimalFormat("#.##")
        val sizeInMB = df.format(totalSize.toDouble() / (1024 * 1024))
        
        val cacheText = getString(R.string.cacheUsageFormat, "${sizeInMB}MB", fileCount.toInt())
        cacheUsageText.text = cacheText
        
        // Show clear cache link if there are tiles to clear
        if (fileCount > 0) {
            clearCacheLink.visibility = View.VISIBLE
        } else {
            clearCacheLink.visibility = View.GONE
        }
    }

    private fun getDirectoryStats(directory: File): LongArray {
        var size = 0L
        var count = 0L
        
        if (directory.isDirectory && directory.listFiles() != null) {
            directory.listFiles()?.forEach { file ->
                if (file.isFile) {
                    size += file.length()
                    count++
                } else if (file.isDirectory) {
                    val subDirStats = getDirectoryStats(file)
                    size += subDirStats[0]
                    count += subDirStats[1]
                }
            }
        }
        
        return longArrayOf(size, count)
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            
            // Show guidance to the user
            Toast.makeText(this, "Tap 'Storage and Cache' → 'Clear Cache' in the settings that open", Toast.LENGTH_LONG).show()
            
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app settings", e)
            Toast.makeText(this, "Unable to open app settings", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "DownloadMapsActivity"
        private const val YAML_URL = "https://trigpointinguk-maps.s3.eu-west-1.amazonaws.com/map_downloads.yaml"
    }
}
