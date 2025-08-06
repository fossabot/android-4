package com.trigpointinguk.android.common;

import java.io.File;
import android.content.Context;

public class FileCache {

	private File cacheDir;

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
		File f = new File(cacheDir, filename);
		return f;
	}

	public File getCacheDir() {
		return cacheDir;
	}

	public int clear(){
		File[] files=cacheDir.listFiles();
		for(File f:files) {
			f.delete();
		}
		return files.length;
	}

}
