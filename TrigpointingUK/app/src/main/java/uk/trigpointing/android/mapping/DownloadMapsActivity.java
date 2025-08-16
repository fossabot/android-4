package uk.trigpointing.android.mapping;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import uk.trigpointing.android.R;
import uk.trigpointing.android.common.BaseActivity;

public class DownloadMapsActivity extends BaseActivity {

    private static final String TAG = "DownloadMapsActivity";
    private static final String YAML_URL = "https://trigpointinguk-maps.s3.eu-west-1.amazonaws.com/map_downloads.yaml";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private MapDownloadAdapter adapter;
    private BroadcastReceiver progressReceiver;
    private BroadcastReceiver completeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_maps);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fetchMapDownloads();

        progressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && DownloadService.ACTION_PROGRESS.equals(intent.getAction())) {
                    int progress = intent.getIntExtra(DownloadService.EXTRA_PROGRESS, 0);
                    if (adapter != null) {
                        adapter.updateProgress(progress);
                    }
                }
            }
        };
        
        completeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && DownloadService.ACTION_COMPLETE.equals(intent.getAction())) {
                    boolean success = intent.getBooleanExtra(DownloadService.EXTRA_SUCCESS, false);
                    if (success) {
                        Toast.makeText(context, "Map download complete!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Map download failed.", Toast.LENGTH_SHORT).show();
                    }
                    finish();
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver, new IntentFilter(DownloadService.ACTION_PROGRESS));
        LocalBroadcastManager.getInstance(this).registerReceiver(completeReceiver, new IntentFilter(DownloadService.ACTION_COMPLETE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(completeReceiver);
    }

    private void fetchMapDownloads() {
        progressBar.setVisibility(View.VISIBLE);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(YAML_URL).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(DownloadMapsActivity.this, "Failed to load map list", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to fetch YAML", e);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String yamlString = response.body().string();
                    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                    try {
                        MapDownload.MapDownloadsList list = mapper.readValue(yamlString, MapDownload.MapDownloadsList.class);
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            adapter = new MapDownloadAdapter(list.maps);
                            recyclerView.setAdapter(adapter);
                        });
                    } catch (IOException e) {
                        onFailure(call, e);
                    }
                } else {
                    onFailure(call, new IOException("Unexpected code " + response));
                }
            }
        });
    }

    private class MapDownloadAdapter extends RecyclerView.Adapter<MapDownloadAdapter.ViewHolder> {

        private final List<MapDownload> mapDownloads;
        private int downloadingPosition = -1;
        private int currentProgress = 0;

        public MapDownloadAdapter(List<MapDownload> mapDownloads) {
            this.mapDownloads = mapDownloads;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_map_download, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MapDownload mapDownload = mapDownloads.get(position);
            holder.mapName.setText(mapDownload.name);
            holder.mapDescription.setText(mapDownload.description);

            DecimalFormat df = new DecimalFormat("#.##");
            String sizeInMB = df.format((double) mapDownload.fileSize / (1024 * 1024));
            holder.mapSize.setText("Size: " + sizeInMB + " MB");

            if (position == downloadingPosition) {
                holder.downloadProgressBar.setVisibility(View.VISIBLE);
                holder.downloadProgressBar.setProgress(currentProgress);
                holder.downloadButton.setEnabled(false);
            } else {
                holder.downloadProgressBar.setVisibility(View.GONE);
                holder.downloadButton.setEnabled(true);
            }

            holder.downloadButton.setOnClickListener(v -> {
                downloadingPosition = holder.getAdapterPosition();
                currentProgress = 0;
                notifyDataSetChanged();

                Intent intent = new Intent(v.getContext(), DownloadService.class);
                intent.setAction(DownloadService.ACTION_DOWNLOAD);
                intent.putExtra(DownloadService.EXTRA_URL, mapDownload.fileUrl);
                intent.putExtra(DownloadService.EXTRA_MAP_NAME, mapDownload.name);
                intent.putExtra(DownloadService.EXTRA_SOURCE_URL, mapDownload.sourceUrl);
                v.getContext().startService(intent);
            });
        }

        @Override
        public int getItemCount() {
            return mapDownloads.size();
        }
        
        public void updateProgress(int progress) {
            if (downloadingPosition != -1) {
                currentProgress = progress;
                notifyItemChanged(downloadingPosition);
                if (progress >= 100) {
                    downloadingPosition = -1; // Reset
                }
            }
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView mapName;
            public TextView mapDescription;
            public TextView mapSize;
            public Button downloadButton;
            public ProgressBar downloadProgressBar;

            public ViewHolder(View itemView) {
                super(itemView);
                mapName = itemView.findViewById(R.id.mapName);
                mapDescription = itemView.findViewById(R.id.mapDescription);
                mapSize = itemView.findViewById(R.id.mapSize);
                downloadButton = itemView.findViewById(R.id.downloadButton);
                downloadProgressBar = itemView.findViewById(R.id.downloadProgressBar);
            }
        }
    }
}
