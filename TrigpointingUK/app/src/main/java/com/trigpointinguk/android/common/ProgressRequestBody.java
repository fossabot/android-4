package com.trigpointinguk.android.common;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class ProgressRequestBody extends RequestBody {

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private final File file;
    private final MediaType mediaType;
    private final CountingMultipartEntity.ProgressListener progressListener;

    public ProgressRequestBody(File file, MediaType mediaType, CountingMultipartEntity.ProgressListener progressListener) {
        this.file = file;
        this.mediaType = mediaType;
        this.progressListener = progressListener;
    }

    @Override
    public MediaType contentType() {
        return mediaType;
    }

    @Override
    public long contentLength() {
        return file.length();
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            long uploaded = 0;
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                sink.write(buffer, 0, read);
                uploaded += read;
                if (progressListener != null) {
                    progressListener.transferred(uploaded);
                }
            }
        }
    }
}


