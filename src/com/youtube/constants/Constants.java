package com.youtube.constants;

import android.os.Environment;

public class Constants {

	public static int 				displayWidth = 320;
	public static int 				displayHeight = 480;
	public static final String 		SEARCH_URL = "https://gdata.youtube.com/feeds/api/videos?";
	public static final String 		VIDEO_INFO_URL = "http://www.youtube.com/get_video_info?video_id=";
	public static final int			MSG_SUCCESS = 0;
	public static final int			MSG_ERROR = 1;
	public static final int			MSG_DOWNLOAD_SUCCESS = 2;
	public static final int			MSG_DOWNLOAD_ERROR = 7;
	public static final int			MSG_SIZE = 3;
	public static final int			MSG_DOWNLOAD_PROGRESS = 4;
	public static final int			MSG_CONVERT_ERROR = 5;
	public static final int			MSG_CONVERT_SUCCESS = 6;
	public static String 			videofile_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/youtube_download/";
}
