package com.youtube.xmlparser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

import com.youtube.constants.VideoInfo;
import com.youtube.httpclient.HttpConnectionUtil;

public class XmlPareser {
	private HttpConnectionUtil conn;
	
	public XmlPareser(HttpConnectionUtil conn)
	{
		this.conn = conn;
	}
	
	public ArrayList<VideoInfo> getVideoInfo(String url) throws IOException, XmlPullParserException
	{
		ArrayList<VideoInfo> list = new ArrayList<VideoInfo>();
		VideoInfo item = null;
		
		InputStream is = conn.getInputStream(url);		
		if(is!=null)
		{
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(is, "utf-8");
			int type = parser.getEventType();
						
			while(type!=XmlPullParser.END_DOCUMENT)
			{
				switch (type)
	            {
	            	case XmlPullParser.START_TAG:
	                	String tagName = parser.getName().trim();
	                	if("entry".equals(tagName)){
	                		item = new VideoInfo();
	                		list.add(item);
	                	}
	                	else if("title".equals(tagName))
	                	{
	                		if(item != null)
	                			item.settitle(parser.nextText());
	                	}
	                	else if("videoid".equals(tagName))
	                	{
	                		if(item != null)
	                			item.setvideo_id(parser.nextText());
	                	}
	                	else if("player".equals(tagName)){
	                		if(item != null)
	                			item.setvideo_url(parser.getAttributeValue(0));
	                	}
	                
	                	break;
	             }				
				 type = parser.next();
			}
			is.close();
			
		}
		
		return list;
	}
	
}
