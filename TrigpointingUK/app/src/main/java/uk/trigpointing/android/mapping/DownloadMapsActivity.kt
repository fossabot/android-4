package uk.trigpointing.android.mapping

import android.os.Bundle
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
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.DecimalFormat

class DownloadMapsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: MapDownloadAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download_maps)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        recyclerView.layoutManager = LinearLayoutManager(this)

        fetchMapDownloads()
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
                    var extractedSize = 0L

                    body.byteStream().use { inputStream ->
                        BufferedInputStream(inputStream).use { bufferedInputStream ->
                            TarArchiveInputStream(bufferedInputStream).use { tarInput ->
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
                                        extractedSize += entry.size
                                        val progress = ((extractedSize * 100) / totalSize).toInt()
                                        withContext(Dispatchers.Main) {
                                            adapter.updateProgress(mapDownload, progress)
                                        }
                                    }
                                    entry = tarInput.nextTarEntry
                                }
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DownloadMapsActivity, "${mapDownload.name} download complete!", Toast.LENGTH_SHORT).show()
                        adapter.updateProgress(mapDownload, 100) // Mark as complete
                        finish() // Close the activity
                    }
                } else {
                    showError("Download failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download/Extraction failed", e)
                showError("Download failed")
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

    companion object {
        private const val TAG = "DownloadMapsActivity"
        private const val YAML_URL = "https://trigpointinguk-maps.s3.eu-west-1.amazonaws.com/map_downloads.yaml"
    }
}
