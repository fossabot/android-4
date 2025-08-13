package com.trigpointinguk.android.common;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Legacy placeholder kept for source compatibility. The Apache HttpClient MultipartEntity
 * is removed; uploads now use OkHttp. This class only preserves the progress listener shape
 * for reuse.
 */
public class CountingMultipartEntity {

    public interface ProgressListener {
        void transferred(long num);
    }

    public static class CountingOutputStream extends FilterOutputStream {
        private final ProgressListener listener;
        private long transferred;

        public CountingOutputStream(final OutputStream out, final ProgressListener listener) {
            super(out);
            this.listener = listener;
            this.transferred = 0;
        }

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            out.write(buffer, offset, length);
            this.transferred += length;
            this.listener.transferred(this.transferred);
        }

        @Override
        public void write(int oneByte) throws IOException {
            out.write(oneByte);
            this.transferred++;
            this.listener.transferred(this.transferred);
        }
    }
}
