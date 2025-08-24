package uk.trigpointing.android.ar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import uk.trigpointing.android.types.Trig;

/**
 * Custom view that draws trigpoint overlays on top of camera for sensor-based AR
 */
public class AROverlayView extends View {
    private static final String TAG = "AROverlayView";
    
    private Paint textPaint;
    private Paint iconPaint;
    private List<TrigpointData> trigpoints = new ArrayList<>();
    private float deviceAzimuth = 0;
    private float devicePitch = 0;
    private Location currentLocation;
    
    // Simple data holder for trigpoint information
    public static class TrigpointData {
        long id;
        String name;
        double lat;
        double lon;
        String type;
        String condition;
        
        public TrigpointData(long id, String name, double lat, double lon, String type, String condition) {
            this.id = id;
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.type = type;
            this.condition = condition;
        }
        
        public long getId() { return id; }
        public String getName() { return name; }
        public double getLat() { return lat; }
        public double getLon() { return lon; }
        public String getType() { return type; }
        public String getCondition() { return condition; }
    }
    
    public AROverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }
    
    private void initPaints() {
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40);
        textPaint.setAntiAlias(true);
        textPaint.setShadowLayer(2, 1, 1, Color.BLACK);
        
        iconPaint = new Paint();
        iconPaint.setAntiAlias(true);
    }
    
    public void updateTrigpoints(List<TrigpointData> trigpoints) {
        this.trigpoints = new ArrayList<>(trigpoints);
        invalidate(); // Trigger redraw
    }
    
    public void updateOrientation(float azimuth, float pitch, float roll) {
        this.deviceAzimuth = azimuth;
        this.devicePitch = pitch;
        invalidate(); // Trigger redraw
    }
    
    public void setCurrentLocation(Location location) {
        this.currentLocation = location;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (trigpoints.isEmpty() || currentLocation == null) {
            return;
        }
        
        int screenWidth = getWidth();
        int screenHeight = getHeight();
        float fieldOfView = 60.0f; // Degrees, typical phone camera FOV
        
        for (TrigpointData trig : trigpoints) {
            // Calculate bearing to trigpoint
            Location trigLocation = new Location("trigpoint");
            trigLocation.setLatitude(trig.getLat());
            trigLocation.setLongitude(trig.getLon());
            
            float bearing = currentLocation.bearingTo(trigLocation);
            if (bearing < 0) bearing += 360; // Normalize to 0-360
            
            float distance = currentLocation.distanceTo(trigLocation);
            
            // Calculate relative bearing (difference between trigpoint bearing and device bearing)
            float relativeBearing = bearing - deviceAzimuth;
            
            // Normalize relative bearing to -180 to 180
            while (relativeBearing > 180) relativeBearing -= 360;
            while (relativeBearing < -180) relativeBearing += 360;
            
            // Only draw trigpoints within field of view
            if (Math.abs(relativeBearing) <= fieldOfView / 2) {
                // Calculate screen position
                float screenX = screenWidth / 2 + (relativeBearing / (fieldOfView / 2)) * (screenWidth / 2);
                
                // For now, put all trigpoints at horizon level (center Y)
                // TODO: Adjust for elevation differences using devicePitch
                float screenY = screenHeight / 2;
                
                // Draw trigpoint icon
                drawTrigpointIcon(canvas, trig, screenX, screenY, distance);
            }
        }
    }
    
    private void drawTrigpointIcon(Canvas canvas, TrigpointData trig, float x, float y, float distance) {
        try {
            // Get trigpoint icon
            int iconRes = getTrigpointIconResource(trig);
            Drawable icon = ContextCompat.getDrawable(getContext(), iconRes);
            
            if (icon != null) {
                // Scale icon based on distance (closer = larger, but with reasonable limits)
                float scale = Math.max(0.3f, Math.min(1.0f, 1000.0f / distance));
                int iconSize = (int) (64 * scale); // Base size 64dp
                
                // Draw icon
                icon.setBounds(
                    (int) (x - iconSize / 2),
                    (int) (y - iconSize / 2),
                    (int) (x + iconSize / 2),
                    (int) (y + iconSize / 2)
                );
                icon.draw(canvas);
                
                // Draw trigpoint name below icon
                String text = trig.getName();
                Rect textBounds = new Rect();
                textPaint.getTextBounds(text, 0, text.length(), textBounds);
                
                float textX = x - textBounds.width() / 2;
                float textY = y + iconSize / 2 + textBounds.height() + 10;
                
                // Draw text background
                canvas.drawRect(
                    textX - 5, textY - textBounds.height() - 5,
                    textX + textBounds.width() + 5, textY + 5,
                    new Paint() {{ setColor(0x80000000); }} // Semi-transparent black
                );
                
                canvas.drawText(text, textX, textY, textPaint);
                
                // Draw distance
                String distanceText = String.format("%.0fm", distance);
                textPaint.getTextBounds(distanceText, 0, distanceText.length(), textBounds);
                float distanceX = x - textBounds.width() / 2;
                float distanceY = textY + textBounds.height() + 10;
                
                canvas.drawRect(
                    distanceX - 5, distanceY - textBounds.height() - 5,
                    distanceX + textBounds.width() + 5, distanceY + 5,
                    new Paint() {{ setColor(0x80000000); }}
                );
                
                canvas.drawText(distanceText, distanceX, distanceY, textPaint);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error drawing trigpoint icon for " + trig.getName(), e);
        }
    }
    
    private int getTrigpointIconResource(TrigpointData trigpoint) {
        // For AR view, always use bright green "good condition" icons for visibility
        Trig.Physical physicalType = Trig.Physical.fromCode(trigpoint.getType());
        // Always use the highlighted (bright green) version for AR visibility
        return physicalType.icon(true); // true = use highlighted/bright version
    }
    
    @Override
    public boolean performClick() {
        // Handle click events on trigpoint icons
        // TODO: Implement hit testing to determine which trigpoint was clicked
        return super.performClick();
    }
}
