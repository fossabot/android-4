package uk.trigpointing.android.mapping;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import uk.trigpointing.android.R;

public class DownloadService extends Service {

    private static final String TAG = "DownloadService";
    public static final String ACTION_DOWNLOAD = "uk.trigpointing.android.action.DOWNLOAD";
    public static final String EXTRA_URL = "extra_url";
    public static final String EXTRA_MAP_NAME = "extra_map_name";
    public static final String EXTRA_SOURCE_URL = "extra_source_url";

    public static final String ACTION_PROGRESS = "uk.trigpointing.android.action.PROGRESS";
    public static final String EXTRA_PROGRESS = "extra_progress";

    public static final String ACTION_COMPLETE = "uk.trigpointing.android.action.COMPLETE";
    public static final String EXTRA_SUCCESS = "extra_success";

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "DownloadChannel";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_DOWNLOAD.equals(intent.getAction())) {
            String url = intent.getStringExtra(EXTRA_URL);
            String mapName = intent.getStringExtra(EXTRA_MAP_NAME);
            String sourceUrl = intent.getStringExtra(EXTRA_SOURCE_URL);
            startForeground(NOTIFICATION_ID, createNotification(mapName, 0));
            downloadAndExtract(url, mapName, sourceUrl);
        }
        return START_NOT_STICKY;
    }

    private void downloadAndExtract(String url, String mapName, String sourceUrl) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Download failed", e);
                stopSelf();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Download failed: " + response);
                    stopSelf();
                    return;
                }

                ResponseBody body = response.body();
                if (body == null) {
                    stopSelf();
                    return;
                }

                try (InputStream inputStream = body.byteStream();
                     BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                     TarArchiveInputStream tarInput = new TarArchiveInputStream(bufferedInputStream)) {

                    TarArchiveEntry entry;
                    File cacheDir = getCacheDir();
                    long totalSize = body.contentLength();
                    long extractedSize = 0;

                    while ((entry = tarInput.getNextTarEntry()) != null) {
                        if (entry.isDirectory()) {
                            continue;
                        }
                        File outputFile = new File(cacheDir, entry.getName());
                        outputFile.getParentFile().mkdirs();

                        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                            byte[] buffer = new byte[4096];
                            int len;
                            while ((len = tarInput.read(buffer)) != -1) {
                                fos.write(buffer, 0, len);
                            }
                        }
                        
                        extractedSize += entry.getSize();
                        int progress = (int) ((extractedSize * 100) / totalSize);
                        updateNotification(mapName, progress);
                        broadcastProgress(progress);
                    }
                    broadcastComplete(true);
                } catch (Exception e) {
                    Log.e(TAG, "Extraction failed", e);
                    broadcastComplete(false);
                } finally {
                    stopSelf();
                }
            }
        });
    }
    
    private void broadcastProgress(int progress) {
        Intent intent = new Intent(ACTION_PROGRESS);
        intent.putExtra(EXTRA_PROGRESS, progress);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastComplete(boolean success) {
        Intent intent = new Intent(ACTION_COMPLETE);
        intent.putExtra(EXTRA_SUCCESS, success);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private Notification createNotification(String mapName, int progress) {
        createNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Downloading " + mapName)
                .setSmallIcon(R.drawable.icon)
                .setOnlyAlertOnce(true)
                .setProgress(100, progress, false);

        if (progress >= 100) {
            builder.setContentText("Download complete");
        } else {
            builder.setContentText("Progress: " + progress + "%");
        }
        return builder.build();
    }
    
    private void updateNotification(String mapName, int progress) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, createNotification(mapName, progress));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Download Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
