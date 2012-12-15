package com.xdev.obliquity;

import java.io.BufferedReader;
import java.io.File;
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
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fedorvlasov.lazylist.ImageLoader;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.xdev.obliquity.ActivityGalleryGrid.ReturnedDataGalleryGrid.iData;

public class ActivityGalleryGrid extends Activity{
	
	private String TAG = Config.TAG_GALLERY_VIEW;
	private boolean DEBUG = true;
	
	Util mUtil;
	Obliquity superState;
	Context mContext;
	ImageLoader mImgLoader;
	String ID;
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.ac_gallerygrid);
        mContext = this;
        
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
			displayError("We could not connect to Facebook.");
			return;
		}
		
		List<String> URL = new ArrayList<String>();
		
		for(iData d:data.data) {
			URL.add(d.images.get(0).source);
		}
		
		initGridView(URL);
	}
	
	// Displays an error, exists activity
		private void displayError(String error) {
			Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show();
			finish(); // Close this activity
		}
		
	
	// Sets up the gridview
	private void initGridView(List<String> val) {
		// Hide loading progressbar
		ProgressBar pbar = (ProgressBar) findViewById(R.id.gallery_progressbar);
		TextView ptext = (TextView) findViewById(R.id.gallery_loading_text);
		pbar.setVisibility(View.INVISIBLE);
		ptext.setVisibility(View.INVISIBLE);
				
				
		GridView mGridView = (GridView) findViewById(R.id.gallerygrid_gridview);
		mGridView.setAdapter(new ImageAdapter(this, val));
		
		// Display the image Clicked
		mGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int arg2,
					long arg3) {
				String URL = (String) v.getTag(R.string.key_image_url);
				
				if(DEBUG) Log.d(TAG, "Viewing image : " + URL);
				
				File f = mImgLoader.getFileCache().getFileExists(URL);
				
				if(f == null) {
					Log.e(TAG, "View Image : File is not in cache!");
					return;
				}
				
				// Create and fire intent to display file
				Intent intent = new Intent();
				intent.setAction(android.content.Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.fromFile(f), "image/png");
				startActivity(intent);
			}
		
		});
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
//	        ImageView imageView;
//	        if (convertView == null) {  // if it's not recycled, initialize some attributes
//	            imageView = new ImageView(mContext);
//	            imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
//	            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
//	            imageView.setPadding(8, 8, 8, 8);
//	        } else {
//	            imageView = (ImageView) convertView;
//	        }

	    	View v;
	    	if (convertView == null) {
	    		LayoutInflater inflater = (LayoutInflater) mContext
    					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            v = inflater.inflate(R.layout.gallery_item, null);
	    	} else {
	    		v = convertView;
	    	}
	    	
	    	ImageView img = (ImageView)v.findViewById(R.id.gallery_image);
	    	
	    	String url = albumIDs.get(position);
	        mImgLoader.DisplayImage(url, img, 125);
	        
	        // URL tag will be used when sending the view intent
	        v.setTag(R.string.key_image_url, url);	        
	        return v;
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
