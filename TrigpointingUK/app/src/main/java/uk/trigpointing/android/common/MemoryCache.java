package uk.trigpointing.android.common;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import android.graphics.Bitmap;

public class MemoryCache {
    private final HashMap<String, SoftReference<Object>> cache= new HashMap<>();
    
    public Bitmap getBitmap(String id){
        if(!cache.containsKey(id))
            return null;
        SoftReference<Object> ref=cache.get(id);
        if (ref != null) {
            return (Bitmap)ref.get();
        }
        return null;
    }
    
    public String getString(String id){
        if(!cache.containsKey(id))
            return null;
        SoftReference<Object> ref=cache.get(id);
        if (ref != null) {
            return (String)ref.get();
        }
        return null;
    }
    
    public void put(String id, Bitmap bitmap){
        cache.put(id, new SoftReference<>(bitmap));
    }

    public void put(String id, String string){
        cache.put(id, new SoftReference<>(string));
    }

    public void clear() {
        cache.clear();
    }

}