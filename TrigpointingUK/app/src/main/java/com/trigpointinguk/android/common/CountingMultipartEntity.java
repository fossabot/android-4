package com.trigpointinguk.android.common;

/**
 * Legacy placeholder kept for source compatibility. The Apache HttpClient MultipartEntity
 * is removed; uploads now use OkHttp. This class only preserves the progress listener shape
 * for reuse.
 */
public class CountingMultipartEntity {

    public interface ProgressListener {
        void transferred(long num);
    }

}
