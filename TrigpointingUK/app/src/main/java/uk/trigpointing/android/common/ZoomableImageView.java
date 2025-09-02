package uk.trigpointing.android.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * Custom ImageView that supports pinch-to-zoom and pan gestures
 * Modern implementation using standard Android gesture detection
 */
public class ZoomableImageView extends AppCompatImageView {

    // Matrix for transformations
    private final Matrix mMatrix = new Matrix();

    // Scale limits
    private static final float MIN_SCALE = 1.0f;
    private static final float MAX_SCALE = 10.0f;
    
    // Current scale
    private float mCurrentScale = 1.0f;
    
    // Gesture detectors
    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;
    
    // View dimensions
    private int mViewWidth;
    private int mViewHeight;
    
    // Image dimensions
    private int mImageWidth;
    private int mImageHeight;
    
    public ZoomableImageView(Context context) {
        super(context);
        init();
    }
    
    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public ZoomableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        setScaleType(ScaleType.MATRIX);
        
        mScaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        mGestureDetector = new GestureDetector(getContext(), new GestureListener());
    }
    
    @Override
    public void setImageBitmap(Bitmap bitmap) {
        super.setImageBitmap(bitmap);
        if (bitmap != null) {
            mImageWidth = bitmap.getWidth();
            mImageHeight = bitmap.getHeight();
            resetZoom();
        }
    }
    
    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        if (drawable != null) {
            mImageWidth = drawable.getIntrinsicWidth();
            mImageHeight = drawable.getIntrinsicHeight();
            resetZoom();
        }
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewWidth = w;
        mViewHeight = h;
        resetZoom();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean scaleResult = mScaleDetector.onTouchEvent(event);
        boolean gestureResult = mGestureDetector.onTouchEvent(event);
        
        return scaleResult || gestureResult || super.onTouchEvent(event);
    }
    
    @Override
    public boolean performClick() {
        // Call the parent implementation to handle accessibility
        super.performClick();
        return true;
    }
    
    /**
     * Reset zoom to fit image to screen
     */
    public void resetZoom() {
        if (mViewWidth == 0 || mViewHeight == 0 || mImageWidth == 0 || mImageHeight == 0) {
            return;
        }
        
        mMatrix.reset();
        
        // Calculate scale to fit image to view
        float scaleX = (float) mViewWidth / mImageWidth;
        float scaleY = (float) mViewHeight / mImageHeight;
        float scale = Math.min(scaleX, scaleY);
        
        mCurrentScale = scale;
        
        // Center the image
        float dx = (mViewWidth - mImageWidth * scale) / 2;
        float dy = (mViewHeight - mImageHeight * scale) / 2;
        
        mMatrix.setScale(scale, scale);
        mMatrix.postTranslate(dx, dy);
        
        setImageMatrix(mMatrix);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float newScale = mCurrentScale * scaleFactor;
            
            // Limit zoom range
            newScale = Math.max(MIN_SCALE, Math.min(newScale, MAX_SCALE));
            
            if (newScale != mCurrentScale) {
                float prevScale = mCurrentScale;
                mCurrentScale = newScale;
                
                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();
                
                // Scale around focus point
                mMatrix.postScale(mCurrentScale / prevScale, mCurrentScale / prevScale, focusX, focusY);
                
                // Keep image within bounds
                constrainPan();
                
                setImageMatrix(mMatrix);
            }
            
            return true;
        }
    }
    
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
            // Only pan if zoomed in
            if (mCurrentScale > MIN_SCALE) {
                mMatrix.postTranslate(-distanceX, -distanceY);
                constrainPan();
                setImageMatrix(mMatrix);
                return true;
            }
            return false;
        }
        
        @Override
        public boolean onSingleTapUp(@NonNull MotionEvent e) {
            // Call performClick for accessibility
            performClick();
            return true;
        }
        
        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) {
            if (mCurrentScale > MIN_SCALE) {
                // If zoomed in, reset to fit
                resetZoom();
            } else {
                // If at minimum scale, zoom to 2x at tap point
                float targetScale = MIN_SCALE * 2;
                float scaleFactor = targetScale / mCurrentScale;
                mCurrentScale = targetScale;
                
                float focusX = e.getX();
                float focusY = e.getY();
                
                mMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY);
                constrainPan();
                setImageMatrix(mMatrix);
            }
            return true;
        }
        
        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return true;
        }
    }
    
    /**
     * Constrain panning to keep image within bounds
     */
    private void constrainPan() {
        float[] values = new float[9];
        mMatrix.getValues(values);
        
        float dx = values[Matrix.MTRANS_X];
        float dy = values[Matrix.MTRANS_Y];
        float scaleX = values[Matrix.MSCALE_X];
        float scaleY = values[Matrix.MSCALE_Y];
        
        float scaledImageWidth = mImageWidth * scaleX;
        float scaledImageHeight = mImageHeight * scaleY;
        
        float minX = Math.min(0, mViewWidth - scaledImageWidth);
        float maxX = Math.max(0, mViewWidth - scaledImageWidth);
        float minY = Math.min(0, mViewHeight - scaledImageHeight);
        float maxY = Math.max(0, mViewHeight - scaledImageHeight);
        
        // Constrain X
        if (dx < minX) {
            dx = minX;
        } else if (dx > maxX) {
            dx = maxX;
        }
        
        // Constrain Y
        if (dy < minY) {
            dy = minY;
        } else if (dy > maxY) {
            dy = maxY;
        }
        
        values[Matrix.MTRANS_X] = dx;
        values[Matrix.MTRANS_Y] = dy;
        mMatrix.setValues(values);
    }
}
