package com.xdev.obliquity;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;

import com.google.android.apps.analytics.easytracking.EasyTracker;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.xdev.obliquity.JsonResponse.Events;
import com.xdev.obliquity.JsonResponse.Feeds;

public class DownloadHandler {
	
	//URLs
	private final String allURL = Config.SERVER_ALL_JSON_URL;
	
	// Constants
	private final String TAG =  Config.TAG_DATAHANDLER;
	private final boolean DEBUG = Config.DEBUG_DOWNLOAD_HANDLER;
	private final String RAW_JSON = Config.PREF_STRING_RAW_JSON;
	
	private Handler mHandler;
	private Util mUtil;
	private EasyTracker tracker;
	private Context mContext;
	
	// Data
	private JsonResponse mResponse;
	private JsonResponse cachedResponse;
	
	// Boolean Checks
	private boolean cacheLoaded; // True : if Loaded from memory, False : If failure to load (Cache not saved)
	private boolean inProgress = false;
	
	
	// Constructor 
	DownloadHandler(Context context, Handler handler, Util util) {
		mContext = context;
		mHandler = handler;
		mUtil = util;
		tracker = EasyTracker.getTracker();
	}
	
	// Initiates the Download Sequence on a seperate Thread.
	private void initDownloadThread() {
		Thread thread = new Thread() {
			
			public void run() {
				
				if(mUtil.isOnline()) {
					inProgress = true;
					Log.i(TAG, "Download Thread Starts");
					int what = 1;
					
					String rawJSON = Download(allURL);
					if(rawJSON != null) 
						mResponse = parseJSON(rawJSON);
					else
						what = 0;
					
					if(rawJSON != null && mResponse != null) {
						saveJSON(rawJSON);
						formatDate(mResponse);
						updateAdvert(mResponse.advert);
						
					} else
						what = 0;
					
					
					Log.i(TAG, "Messaging Handler what: " + what);	
					mHandler.sendEmptyMessage(what);
					
					if(what == 0)
						loadCachedData();
					
					inProgress = false;
				
				} else {
					Log.e(TAG, "Network Unavailable");
					mHandler.sendEmptyMessage(2);
				}
			}
			
		};
		
		thread.start();
	}
	
	private void loadFromMemoryThread() {
		if(inProgress) return;
		
		Thread thread = new Thread() {
			
			public void run() {
				inProgress = true;
				
				if(DEBUG) Log.i(TAG, "Load From Memory Thread Starts");
					
				loadCachedData();
				
				if(DEBUG) Log.i(TAG, "Cached Data Loaded : " + cacheLoaded);
				
				if(cacheLoaded) {
					formatDate(cachedResponse);
					mHandler.sendEmptyMessage(11);
				}
				else
					mHandler.sendEmptyMessage(10);
					
				inProgress = false;
			}
			
		};
		
		thread.start();
	}
	
	// Formats the time stamps
	public void formatDate(JsonResponse response)
	{
		Log.i(TAG, "Formatting date");
		DateFormat input = new SimpleDateFormat(Config.DATE_OLD_FEED);
		DateFormat output = new SimpleDateFormat(Config.DATE_NEW_FEED);
		Date stamp = null;
		
		for(Feeds feed : response.feeds)
		{
			try {
				stamp = (Date)input.parse(feed.timeStamp);
			} catch (ParseException e) {
				Log.w(TAG, "Failure parsing date in feed : " + feed.timeStamp);
				e.printStackTrace();
				break;
			}
			
			feed.timeStamp = output.format(stamp);
		}
		
		input = new SimpleDateFormat(Config.DATE_OLD_EVENT);
		output = new SimpleDateFormat(Config.DATE_NEW_EVENT);
		
		for(Events event : response.events)
		{
			try {
				stamp = (Date)input.parse(event.date);
			} catch (ParseException e) {
				Log.w(TAG, "Failure parsing date in feed : " + event.date);
				e.printStackTrace();
				break;
			}
			
			event.date = output.format(stamp);
		}
	}
	
	
	// Downloads and saves an advert image if applicable
	private void updateAdvert(int downloadedID) {
		int currentID = mUtil.getInt(Config.PREF_ADVERT_ID, -1);
		
		if(DEBUG) Log.i(TAG, "ADVERT, downloaded ID : " + downloadedID + " | currentID : " + currentID);
		if(currentID < downloadedID) {
			// Download New Image, write to file
			InputStream in;
			Bitmap bm = null;
			
			try {
				if(DEBUG) Log.d(TAG, "Downloading the image");
				in = new URL(Config.ADVERT_URL).openConnection().getInputStream();
				bm = BitmapFactory.decodeStream(in);
				
			} catch (MalformedURLException e) {
				if(DEBUG) e.printStackTrace();
				return;
			} catch (IOException e) {
				if(DEBUG) e.printStackTrace();
				return;
			}
			
			if(bm != null) {
				FileOutputStream fos;
				try {
					if(DEBUG) Log.d(TAG, "Saving downloaded Image");
					
					fos = mContext.openFileOutput("adfeed.jpg", Context.MODE_PRIVATE);
					bm.compress(CompressFormat.JPEG, 90, fos);
					fos.close();
					
				} catch (FileNotFoundException e) {
					if(DEBUG) {
						Log.e(TAG, "Error Saving downloaded Image");
						e.printStackTrace();
					}
					return;
				} catch (IOException e) {
					if(DEBUG) e.printStackTrace();
					return;
				}
			}
		//Everything was successful
		mUtil.addInt(Config.PREF_ADVERT_ID, downloadedID);
		}
	}
	
	// Loads Cached Data From Preferences
	private void loadCachedData() {
		String json = mUtil.getString(RAW_JSON, "");
		
		if(json == null) {
			cacheLoaded = false;
			Log.i(TAG, "Failed to load Cached Data");
		} else {
			cachedResponse = parseJSON(json);
			
			if(cachedResponse != null) 
				cacheLoaded = true;
			
			Log.i(TAG, "Cached JSON loaded : " + json);
		}
	}
	
	// Saves raw JSON as a string to shared preferences
	private void saveJSON(String JSON) {
		Log.i(TAG, "Saving JSON data");
		
		mUtil.addString(RAW_JSON, JSON);
	}
	
	// Parses JSON | Failure : Returns null
	private JsonResponse parseJSON(String rawJSON){
    	Log.i(TAG, "Parsing JSON : " + rawJSON);
		Gson gson = new Gson();
	    
	    try {
	    	return gson.fromJson(rawJSON, JsonResponse.class);
	    } 
	    catch (JsonSyntaxException e) {
	    	Log.w(TAG, "JsonSyntaxException :" + e.toString());
	    }
	    catch (JsonIOException e) {
	    	Log.w(TAG, "JsonIOException :" + e.toString());
	    }
	    
	    Log.w(TAG, "ParseJson return null");
	    return null;
	}
	
	// Downloads data | Failure : Returns null
	private String Download(String url) {
    	
		Log.i(TAG, "Init Download()");
		
		int timeoutSocket = Config.TIMEOUT_SOCKET;
		int timeoutConnection = Config.TIMEOUT_CONN;
		
    	HttpParams httpParameters = new BasicHttpParams();
    	HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
    	HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
    	HttpClient client = new DefaultHttpClient(httpParameters);
    	HttpGet getRequest = new HttpGet(url);

    	try {
    		HttpResponse getResponse = client.execute(getRequest);
    		final int statusCode = getResponse.getStatusLine().getStatusCode();

    		if(statusCode != HttpStatus.SC_OK) {
    			Log.w(TAG, "Download Error: " + statusCode + "| for URL: " + url);
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
    		Log.w(TAG, "IO Exception : " + e.toString());
    		
    		tracker.trackEvent("Error", "JSON Download Error", e.toString(), 0);
    	} catch (Exception e) {
     		Log.w(TAG, "Download Exception : " + e.toString());
     		tracker.trackEvent("Error", "JSON Download Error", e.toString(), 0);
    	}
    	
    	Log.w(TAG, "Download return null");
    	return null;
    }

	// --------------------------------------------------------------------------------------------- 
	// PUBLIC METHODS
	
	// Refresh data
	public synchronized void refresh() {
		if(DEBUG) Log.d(TAG, "Refresh Called. Thread InProgress : " + inProgress);
		if(!inProgress){ 
			initDownloadThread();
		}
	}
	
	// Load Cache
	public synchronized void loadCache() {
		if(DEBUG) Log.d(TAG, "LoadCache called. Thread inProgress : " + inProgress);
		if(!inProgress) {
			loadFromMemoryThread();
		}
	}
	
	public synchronized List<Feeds> getFeeds() {
		return mResponse.feeds;
	}
	
	public synchronized List<Events> getEvents() {
		return mResponse.events;
	}
	
		// LOAD CACHED DATA | CHECK CACHED STATUS BEFORE CALLING METHODS
		public boolean getCachedStatus() {
			return cacheLoaded;
		}
	
		public List<Feeds> getCachedFeeds() {
			return cachedResponse.feeds;
		}
		
		public List<Events> getCachedEvents() {
			return cachedResponse.events;
			
		}
	
	public int getAdvertId() {
		if(cacheLoaded)
			return cachedResponse.advert;
		
		else if(mResponse != null)
			return mResponse.advert;
		
		return -1;
	}
	// END PUBLIC METHODS
	
}
