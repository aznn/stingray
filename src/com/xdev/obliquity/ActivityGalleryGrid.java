package com.xdev.obliquity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.fedorvlasov.lazylist.ImageLoader;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.xdev.obliquity.ActivityGalleryGrid.ReturnedDataGalleryGrid.iData;

public class ActivityGalleryGrid extends Activity{
	
	private String TAG = "TEST";
	private boolean DEBUG = true;
	
	Util mUtil;
	Obliquity superState;
	ImageLoader mImgLoader;
	String ID;
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.ac_gallerygrid);
        
        // Setup local classes
        superState = (Obliquity)getApplication();
        mUtil = superState.getUtil();
        mImgLoader = superState.getImageLoader();
        
        ID = getIntent().getExtras().getString("ID");
        
        new LoadJSONTaskAlbum().execute();
	}
	
	
	private void jsonDownloaded(ReturnedDataGalleryGrid data) {
		if(data == null) {
			Log.e(TAG, "Download Error");
			return;
		}
		
		List<String> URL = new ArrayList<String>();
		
		for(iData d:data.data) {
			URL.add(d.images.get(0).source);
		}
		
		initGridView(URL);
	}
	
	// Sets up the gridview
	private void initGridView(List<String> val) {
		GridView mGridView = (GridView) findViewById(R.id.gallerygrid_gridview);
		mGridView.setAdapter(new ImageAdapter(this, val));
	}
	
	/*
	 * Image Adapter
	 * 
	 */
	public class ImageAdapter extends BaseAdapter {
	    private Context mContext;
	    private List<String> albumIDs;
	    
	    public ImageAdapter(Context c, List<String> albumIds) {
	        mContext = c;
	        albumIDs = albumIds;
	    }

	    public int getCount() { return albumIDs.size(); }

	    public long getItemId(int position) { return position; }

	    // create a new ImageView for each item referenced by the Adapter
	    public View getView(int position, View convertView, ViewGroup parent) {
	        ImageView imageView;
	        if (convertView == null) {  // if it's not recycled, initialize some attributes
	            imageView = new ImageView(mContext);
	            imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
	            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
	            imageView.setPadding(8, 8, 8, 8);
	        } else {
	            imageView = (ImageView) convertView;
	        }

	        mImgLoader.DisplayImage(albumIDs.get(position), imageView, 85);
	        return imageView;
	    }

	@Override
	public Object getItem(int position) { return albumIDs.get(position); }
	    
	}
	
	/*
	 * Classes dealing with downloading the JSON
	 */
	private class LoadJSONTaskAlbum extends AsyncTask<Void, Void, String> {
		
		String formatURL = "https://graph.facebook.com/%s/photos?fields=images&limit=100";
		
		@Override
		protected String doInBackground(Void... arg0) {
			if(DEBUG) Log.i(TAG, "Downloading JSON");
			
			String graphApiURL = String.format(formatURL, ID);
			
			int timeoutSocket = Config.TIMEOUT_SOCKET;
			int timeoutConnection = Config.TIMEOUT_CONN;
			
	    	HttpParams httpParameters = new BasicHttpParams();
	    	HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
	    	HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
	    	HttpClient client = new DefaultHttpClient(httpParameters);
	    	HttpGet getRequest = new HttpGet(graphApiURL);

	    	try {
	    		HttpResponse getResponse = client.execute(getRequest);
	    		final int statusCode = getResponse.getStatusLine().getStatusCode();

	    		if(statusCode != HttpStatus.SC_OK) {
	    			if(DEBUG) Log.w(TAG, "Download Error: " + statusCode + "| for URL: " + graphApiURL);
	    			//Error in Download
	    			return null;
	    		}
	    		
	    		String line = "";
	    		StringBuilder total = new StringBuilder();
	    		
	    		HttpEntity getResponseEntity = getResponse.getEntity();
	    		
	    		BufferedReader reader = new BufferedReader(new InputStreamReader(getResponseEntity.getContent()));	
	    		
	    		while((line = reader.readLine()) != null) {
	    			total.append(line);
	    		}
	    		
	    		line = total.toString();
	    		Log.i(TAG, "Downloaded Data : " + line);
	    		return line;
	    	
	    	} catch (IOException e) {
	    		getRequest.abort();
	    		if(DEBUG) Log.w(TAG, "IO Exception : " + e.toString());
	    	} catch (Exception e) {
	     		if(DEBUG) Log.w(TAG, "Download Exception : " + e.toString());
	    	}
	    	
	    	if(DEBUG) Log.w(TAG, "Download return null");
	    	return null;
		}
	
		@Override
		protected void onPostExecute(String result) {
			if(DEBUG) Log.i(TAG, "Parsing JSON : " + result);
			Gson gson = new Gson();
		    
		    try {
		    	jsonDownloaded(gson.fromJson(result, ReturnedDataGalleryGrid.class));
		    	return;
		    } 
		    catch (JsonSyntaxException e) {
		    	if(DEBUG) Log.w(TAG, "JsonSyntaxException :" + e.toString());
		    }
		    catch (JsonIOException e) {
		    	if(DEBUG) Log.w(TAG, "JsonIOException :" + e.toString());
		    }
		    
		    if(DEBUG) Log.w(TAG, "ParseJson return null");
		    jsonDownloaded(null);
		}
	}
	
	public class ReturnedDataGalleryGrid {
		
		class iData {
			public class Image {
				
				@SerializedName("height")
				public int height;
				
				@SerializedName("width")
				public int width;
				
				@SerializedName("source")
				public String source;
			}
			
			@SerializedName("images")
			public List<Image> images;
			
			@SerializedName("id")
			public String ID;
			
			@SerializedName("created_time")
			public String createdTime;
		}
		
		@SerializedName("data")
		List<iData> data;
		
	}
}
