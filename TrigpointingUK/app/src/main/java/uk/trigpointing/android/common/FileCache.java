package uk.trigpointing.android.common;

import java.io.File;
import android.content.Context;

public class FileCache {

	private final File cacheDir;

	public FileCache(Context context, String cachedir){
		//Use internal cache directory for better reliability
		cacheDir = new File(context.getCacheDir(), cachedir);
		if(!cacheDir.exists()) {
			boolean created = cacheDir.mkdirs();
			android.util.Log.d("FileCache", "Created cache directory " + cacheDir.getAbsolutePath() + ": " + created);
		}
	}

	public File getFile(String url){
		String filename=String.valueOf(url.hashCode());
        return new File(cacheDir, filename);
	}

	public File getCacheDir() {
		return cacheDir;
	}

	public int clear(){
		File[] files=cacheDir.listFiles();
		if (files == null) {return 0;}

		int deletedCount = 0;
		for (File f : files) {
			deletedCount += deleteRecursively(f);
		}
		return deletedCount;
	}

	private int deleteRecursively(File target) {
		if (target == null || !target.exists()) {return 0;}
		if (target.isDirectory()) {
			int total = 0;
			File[] children = target.listFiles();
			if (children != null) {
				for (File child : children) {
					total += deleteRecursively(child);
				}
			}
			boolean dirDeleted = target.delete();
			if (!dirDeleted) {
				android.util.Log.w("FileCache", "Failed to delete directory: " + target.getAbsolutePath());
			}
			return total; // Only count files; directories not included in count
		} else {
			boolean ok = target.delete();
			if (!ok) {
				android.util.Log.w("FileCache", "Failed to delete file: " + target.getAbsolutePath());
				return 0;
			}
			return 1;
		}
	}

}
