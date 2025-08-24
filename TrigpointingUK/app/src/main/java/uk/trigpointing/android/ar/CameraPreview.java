package uk.trigpointing.android.ar;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Custom view that handles camera preview for sensor-based AR
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CameraPreview";
    private SurfaceHolder holder;
    private Camera camera;
    
    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        holder = getHolder();
        holder.addCallback(this);
    }
    
    public void setCamera(Camera camera) {
        this.camera = camera;
        if (holder.getSurface() != null) {
            startPreview();
        }
    }
    
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startPreview();
    }
    
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (camera != null) {
            camera.stopPreview();
            startPreview();
        }
    }
    
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (camera != null) {
            camera.stopPreview();
        }
    }
    
    private void startPreview() {
        if (camera != null && holder.getSurface() != null) {
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (Exception e) {
                Log.e(TAG, "Error starting camera preview", e);
            }
        }
    }
}
