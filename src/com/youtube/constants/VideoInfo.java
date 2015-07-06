package com.youtube.constants;

import java.io.Serializable;

public class VideoInfo implements Serializable{
	private static final long serialVersionUID = 1L;    
	private String title = null;
	private String video_url = null;
	private String video_id = null;

	public String gettitle(){return title;}
	public  void settitle(String val){title = val;}
	public String getvideo_url(){return video_url;}
	public  void setvideo_url(String val){video_url = val;}
	public String getvideo_id(){return video_id;}
	public  void setvideo_id(String val){video_id = val;}

}
