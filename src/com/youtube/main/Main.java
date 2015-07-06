package com.youtube.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.media.ffmpeg.FFMpeg;
import com.youtube.constants.Constants;
import com.youtube.constants.VideoInfo;
import com.youtube.httpclient.HttpConnectionUtil;
import com.youtube.xmlparser.XmlPareser;

public class Main extends Activity {
	private ArrayList<VideoInfo> 	videolist = null;
	private EditText 				searchEditText = null;
	private Button					searchbtn = null;
	private Button					downloadbtn = null;
	private Button					convertbtn = null;
	private ListView				listView = null;
	private String					search_url = null;
	private ProgressDialog			progress = null;
	private ResultListAdapter 		adapter = null;
	private int						selindex = 0;
	private ProgressBar				timeBar = null;
	private FFMpeg		mFFMpeg;
	private int						downloadindex = -1;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        System.setProperty("http.keepAlive", "false");
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        initView();
        setListener();
        initData();
    }
    
    public void initView(){
    	searchEditText = (EditText)findViewById(R.id.searchEditText);
    	searchbtn = (Button)findViewById(R.id.searchbtn);
    	downloadbtn = (Button)findViewById(R.id.downloadbtn);
    	convertbtn = (Button)findViewById(R.id.convertbtn);
    	listView = (ListView)findViewById(R.id.listView);
    	timeBar = (ProgressBar)findViewById(R.id.progressBar);
    	timeBar.setVisibility(View.INVISIBLE);
    }
    
    public void initData(){
    	progress = new ProgressDialog(this);
    	progress.setMessage("Please wait...");
    	File file = new File(Constants.videofile_path);
		if(!file.exists())
			file.mkdir();
		mFFMpeg = new FFMpeg();
    }
    
    public void setListener(){
    	searchbtn.setOnClickListener(buttonListener);
    	downloadbtn.setOnClickListener(buttonListener);
    	convertbtn.setOnClickListener(buttonListener);
    }
    
    public void getResult(){
    	search_url = Constants.SEARCH_URL + String.format("q=%s&start-index=%s&max-results=%s&v=2", searchEditText.getText().toString(),1,10);
    	progress.setMessage("Please wait...");
    	progress.show();
    	new Thread(){
    		public void run(){
    			HttpConnectionUtil conn = new HttpConnectionUtil();
    			XmlPareser xmlPareser = new XmlPareser(conn);
    			try{
    				videolist = (ArrayList<VideoInfo>)xmlPareser.getVideoInfo(search_url);
    				mHandler.sendEmptyMessage(Constants.MSG_SUCCESS);
    			}catch(Exception e){
    				mHandler.sendEmptyMessage(Constants.MSG_ERROR);
    			}
    		}
    	}.start();
    }
    public boolean checkInternetConnection() {
		ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();
		if (info != null && info.isAvailable()) {
			return true;
		} else {
			return false;
		}
	}
    private OnClickListener buttonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			int viewId = v.getId();
			
			switch (viewId) {
			case R.id.searchbtn:
			{
				if(!checkInternetConnection())
					return;
				getResult();
			}
				break;
			case R.id.downloadbtn:
			{
				timeBar.setVisibility(View.VISIBLE);
				downloadindex = selindex;
				startDownload();
			}
				break;
			case R.id.convertbtn:
			{
				progress.setMessage("Converting...");
				progress.show();
				startConvert();
			}
				break;
			
			}
		}
	};
	public void startConvert(){
		new Thread(){
			public void run(){
				if(downloadindex == -1)
				{
					mHandler.sendEmptyMessage(Constants.MSG_CONVERT_ERROR);
					return;
				}
				String src = Constants.videofile_path + videolist.get(downloadindex).getvideo_id() + ".mp4";
				String drc = Constants.videofile_path + videolist.get(downloadindex).getvideo_id() + ".mp3";
				File file = new File(drc);
				if(file.exists())
					file.delete();
				String command = String.format("-i %s %s", src, drc);
				int ret = runCommand("ffmpeg " + command);
				if(ret != 0)
				{
					mHandler.sendEmptyMessage(Constants.MSG_CONVERT_ERROR);
					return;
				}
				mHandler.sendEmptyMessage(Constants.MSG_CONVERT_SUCCESS);
				
			}
		}.start();
	}
	private void startDownload(){
		new Thread(){
			public void run(){
				if(videolist == null || videolist.size() <= selindex)
				{
					mHandler.sendEmptyMessage(Constants.MSG_DOWNLOAD_ERROR);
					return;
				}
				try{
					URL url = new URL(String.format(Constants.VIDEO_INFO_URL + "%s", videolist.get(selindex).getvideo_id()));
					HttpURLConnection con = (HttpURLConnection)url.openConnection();
					con.setRequestMethod("GET");
					InputStream stream = con.getInputStream();
					InputStreamReader reader = new InputStreamReader(stream);
					
					
					StringBuffer buffer = new StringBuffer();
					char[] buf = new char[1024];
					int chars_read;
					while((chars_read = reader.read(buf)) != -1)
					{
						buffer.append(buf,0,chars_read);
					}
					String tmpstr = buffer.toString();
					if(tmpstr == null || tmpstr.length() == 0)
					{
						mHandler.sendEmptyMessage(Constants.MSG_DOWNLOAD_ERROR);
						return;
					}
					int startindex = tmpstr.indexOf("url_encoded_fmt_stream_map") + 33;
					int endindex = tmpstr.indexOf("quality") - 3;
					if(startindex > endindex)
					{
						mHandler.sendEmptyMessage(Constants.MSG_DOWNLOAD_ERROR);
						return;
					}
					tmpstr = tmpstr.substring(startindex, endindex);
					while(tmpstr.contains("%25")){
						tmpstr = tmpstr.replaceAll("%25", "%");
					}
					tmpstr = Uri.decode(tmpstr);
					downloadFunc(tmpstr);
					mHandler.sendEmptyMessage(Constants.MSG_DOWNLOAD_SUCCESS);
				}catch(Exception e){
					mHandler.sendEmptyMessage(Constants.MSG_DOWNLOAD_ERROR);
				}
							
				
			}
		}.start();
	}
	 Handler mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				progress.hide();
				switch (msg.what) {
				case Constants.MSG_SUCCESS: {
					if(videolist == null)
						return;
					adapter = new ResultListAdapter(Main.this,videolist);
					listView.setAdapter(adapter);
				}
					break;
				case Constants.MSG_ERROR: {
					
				}
					break;
				case Constants.MSG_DOWNLOAD_SUCCESS: {
					timeBar.setVisibility(View.INVISIBLE);
					showMessage("","Success to Download!");
				}
					break;
				case Constants.MSG_SIZE:
				{
					
				}
					break;
				case Constants.MSG_DOWNLOAD_ERROR:
				{
					timeBar.setVisibility(View.INVISIBLE);
					showMessage("","This video isnt downloadable!");
				}
					break;
				case Constants.MSG_DOWNLOAD_PROGRESS: {
					int val = msg.arg1;
					timeBar.setProgress(val);
				}
					break;
				case Constants.MSG_CONVERT_ERROR:
				{
					showMessage("","Fail to Convert!");
				}
					break;
				case Constants.MSG_CONVERT_SUCCESS:
				{
					showMessage("","Success to Convert!");
				}
					break;
				}
			}

		};
	 public class ResultListAdapter extends BaseAdapter {
			private Context mContext = null;
			private ArrayList<VideoInfo> itemList = null;
			private VideoInfo item = null;

			public ResultListAdapter(Context mContext,
					ArrayList<VideoInfo> itemList) {
				this.mContext = mContext;
				this.itemList = itemList;
			}

			@Override
			public int getCount() {
				return itemList.size();
			}

			@Override
			public Object getItem(int position) {
				return itemList.get(position);
			}

			@Override
			public long getItemId(int position) {
				return position;
			}

			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				item = itemList.get(position);
				View view = convertView;
				ViewCache cache;
				if (view == null) {
					LayoutInflater inflater = LayoutInflater.from(mContext);
					view = inflater.inflate(R.layout.titlecontent, null);
					cache = new ViewCache(view);
					view.setTag(cache);
				} else {
					cache = (ViewCache) view.getTag();
				}
				TextView titleView = cache.getTextView();
				titleView.setText(item.gettitle());
				if(position == selindex)
					titleView.setBackgroundColor(0xff7dc5f7);
				else
					titleView.setBackgroundColor(Color.TRANSPARENT);
				titleView.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						selindex = position;
						adapter.notifyDataSetChanged();
					}
				});
				return view;
			}

		}
	   
		 class ViewCache {
				private View view;
				
				private TextView textView = null;
			
				public ViewCache(View view) {
					this.view = view;
				}

				public TextView getTextView() {
					if (textView == null) {
						textView = (TextView) view.findViewById(R.id.titleView);
					}
					return textView;
				}
				
			}
		 
		 public void downloadFunc(String val){
			 URL u = null;
			 InputStream is = null;  

			      try {
			               u = new URL(val);
			               is = u.openStream(); 
			               HttpURLConnection huc = (HttpURLConnection)u.openConnection();//to know the size of video
			               int total_size = huc.getContentLength();                 

			           if(huc != null){

			               File file = new File(Constants.videofile_path + videolist.get(selindex).getvideo_id() + ".mp4");
			               if(file.exists())
			               {
			            	   file.delete();
			            	   file.createNewFile();
			               }

			               FileOutputStream fos = new FileOutputStream(file);
			               byte[] buffer = new byte[1024];
			               int len1 = 0; 
			               int updatelength = 0;
			               if(is != null){
			                  while ((len1 = is.read(buffer)) > 0) {
			                        fos.write(buffer,0, len1);   
			                        updatelength += len1;
			                        Message msg = mHandler.obtainMessage();
			    					msg.what = Constants.MSG_DOWNLOAD_PROGRESS;
			    					int percent = 100 * updatelength / total_size;
			    					msg.arg1 = percent;
			    					msg.sendToTarget();
			                  }
			               }
			               if(fos != null){
			                  fos.close();
			               }
			           }                     
			      }catch (MalformedURLException mue) {
			             mue.printStackTrace();
			      } catch (IOException ioe) {
			             ioe.printStackTrace();
			     } finally {
			                try {                
			                  if(is != null){
			                    is.close();
			                  }
			                }catch (IOException ioe) {
			                      // just going to ignore this one
			                }
			     }
		 }
		 public void showMessage(String title,String msg)
			{
		    	Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(msg);
				builder.setTitle(title);
				builder.setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								
								dialog.dismiss();
								}
						});

				Dialog dialog = builder.create();
				dialog.show();
			}
		 private int runCommand(String param) {
		    	String[] argv = param.split(" ");
		    	return mFFMpeg.native_ffmpeg(argv.length, argv);
		    }
}