package com.media.ffmpeg;

import android.util.Log;


public class FFMpeg {
	
	public static final String[] LIBS = new String[] {
		"/data/data/com.youtube.main/lib/libmp3lame.so",
		"/data/data/com.youtube.main/lib/libffmpeg_jni.so"	// ffmpeg libs compiled to jni lib
		
	};
	
	
	private static boolean sLoaded = false;

	
    public FFMpeg() {
    	if (!loadLibs()) {
    		System.out.println("Couldn't load native libs");
    	}
    }
    
    /**
     * loads all native libraries
     * @return true if all libraries was loaded, otherwise return false
     */
    private static boolean loadLibs() {
    	if (sLoaded) {
    		return true;
    	}
    	boolean err = false;
    	for (int i = 0; i < LIBS.length; i++) {
    		try {
    			System.load(LIBS[i]);
    		} catch (UnsatisfiedLinkError e) {
    			// fatal error, we can't load some our libs
    			Log.d("FFMpeg", "Couldn't load lib: " + LIBS[i] + " - " + e.getMessage());
    			err = true;
    		}
		}
    	if (!err) {
    		sLoaded = true;
    	}
    	return sLoaded;
    }

	public native int native_ffmpeg(int argc, Object[] argv);
}
