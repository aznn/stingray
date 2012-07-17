package com.xdev.obliquity;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.fedorvlasov.lazylist.ImageLoader;

public class Obliquity extends Application {
	
	private final String TAG = Config.TAG_OBLIQUITY;
	private final boolean DEBUG = Config.DEBUG_OBLIQUITY_APP;
	
	// Boolean Checks
	private boolean subscribed = false; // Has Client Subscribed
	private boolean dlFailed = false; // Status of Last Download
	private boolean chSuccess = false; // Status of Last Cache
	private boolean dlFinished = false; // Weather a previous download has finished
	private boolean init = false;
	
	private DownloadHandler mDownloadHandler = null;
	private Util mUtil = null;
	private ImageLoader mImgLoader = null;
	
	private Handler mHandler; // Handler for DownloadHandler class
	
	@Override
	public void onCreate() {
		// TODO DOWNLOAD API 10. ENABLE STRICT MODE. DEBUG!
		/*
		if (Config.STRICT_MODE) {
	         StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
	                 .detectDiskReads()
	                 .detectDiskWrites()
	                 .detectNetwork()   // or .detectAll() for all detectable problems
	                 .penaltyLog()
	                 .build());
	         StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
	                 .detectLeakedSqlLiteObjects()
	                 .detectLeakedClosableObjects()
	                 .penaltyLog()
	                 .penaltyDeath()
	                 .build());
	     }
		*/
		super.onCreate();
	}

	private Handler sHandler; // Handler of the subscriber
	
	private int cached_what = -1; // Cached Message to be send to subscribers
	
	private void init() {
		// Handler
		init = true;
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message message) {
				Log.i(TAG, "Message Recieved. What : " + message.what);
				
				switch(message.what) {
					case 0: // Download Failure
						dlFailed = true;
						
						downloadFailed(message.what);
						break;
					case 1: // Download finished
						downloadSuccessful(message.what);
						break;
					case 2: // isOnline returns false
					case 99: // Connected yet server not responding
						noInternetAccess(message.what);
						break;
					case 10: // Cache Failed to Load
						cacheFailed(message.what);
						break;
					case 11: // Cache Successfully Loaded
						cacheSuccessful(message.what);
						break;
				}
			}
		};
	}
	
	// Cache Failed
	private void cacheFailed(int what) { // What - 10
		Log.d(TAG, "dl failed" + dlFailed);
		
		if(!dlFailed)
			mDownloadHandler.refresh();
		else {
			if(DEBUG) Log.e(TAG, "Last Download failed. Cache failed. Assume server not responding");
			sendSubMessage(99); // 99 = Server not responding | Internet connected yet an error.
		}
			
	}
	
	// Download Failed
	private void downloadFailed(int what) { // What - 0
		if(DEBUG) Log.e(TAG, "Download Failed");
		
		dlFailed = true;
		dlFinished = false;
		sendSubMessage(what);
	}
	
	// Download Finished
	private void downloadSuccessful(int what) { // What - 1
		if(DEBUG) Log.d(TAG, "Download Successful");
		dlFailed = false;
		dlFinished = true;
		chSuccess = false; // Invalidate Currently Loaded cache
		sendSubMessage(what);
	}
	
	// No Internet Access
	private void noInternetAccess(int what) { // What - 2
		if(DEBUG) Log.e(TAG, "No Internet Access | server not responding");
		sendSubMessage(what);
	}
	
	// Cache Successful
	private void cacheSuccessful(int what) { // What - 11 
		if(DEBUG) Log.d(TAG, "Cache Successfully Loaded");
		chSuccess = true;
		sendSubMessage(what);
	}
	
	// Sends Messages to Subscribers
	private void sendSubMessage(int what) {
		if(subscribed) {
			Log.i(TAG, "Messaging Subscriber what : " + what);
			sHandler.sendEmptyMessage(what);
		} else {
			cached_what = what;
			Log.i(TAG, "Caching Subscriber what : " + what);
		}
	}
	
	// When a client has subscribed
	private void hasSubscribed() {
		subscribed = true;
		if(cached_what != -1)
			sendSubMessage(cached_what);
	}
	
	// PUBLIC METHODS
	
	// Return reference to the downloadhandler | if DownloadHandler is null calls init
	// Takes a Handler which is used to send Messages to subscribers
	// C2DM - True : Loads from Cache, False : Downloads Data
	// immediateAction : Starts download/Cache immidiately.
	public DownloadHandler getDownloadHandler(Handler handler, boolean C2DMStatus, boolean immediateAction) {
		Log.i(TAG, "Subscriber recieved c2dm : " + C2DMStatus + "| immediate : " + immediateAction);
		sHandler = handler;
		
		initDownloadHandler(C2DMStatus, immediateAction, false);
		
		hasSubscribed();
		return mDownloadHandler;
	}
	
	// Called From the service when it recieves a C2DM Message saying new Data is on the server
	public void forceRefresh(Handler handler) {
		Log.i(TAG, "Initiating a Force Refresh");
		sHandler = handler;
		
		initDownloadHandler(false, false, true);
		
		hasSubscribed();
	}
	
	// GetUtils
	public Util getUtil() {
		if(mUtil == null) {
			mUtil = new Util(getApplicationContext());
		}
		
		return mUtil;
	}

	// GetServerQueue
	private ServerQueue mServerQueue;
	public ServerQueue getServerQueue() {
		if(mServerQueue == null) 
			mServerQueue = new ServerQueue(this, getUtil());
		
		return mServerQueue;
	}
	
	// Initializes the Downloadhandler
	// boolea init : To initiate, or already initiated.
	// init should be set to false, if download/cache start is desired yet dhandler already initialized
	// CalledFromService : Called when a C2DM Message is recieved and service asks to download new data
	public void initDownloadHandler(boolean c2dm, boolean immediateAction, boolean calledFromService) {
		if(DEBUG) Log.i(TAG, "InitDownloadHandler with c2dm : " + c2dm);
		
		if(!init) init();
		
		if(mDownloadHandler ==  null)
			mDownloadHandler = new DownloadHandler(this, mHandler, getUtil());
		
		// Called From Service. Do a Force Refresh
		if(calledFromService) {
			mDownloadHandler.refresh();
		}
		
		if(immediateAction) {
			if(c2dm) {
				if(DEBUG) Log.d(TAG, "load cache. chSuccess : " + chSuccess);
				
				if(chSuccess)
					cacheSuccessful(11); // IF chSuccess(Cache is already loaded) Notify the handler
				else
					mDownloadHandler.loadCache();
			} else {
				if(DEBUG) Log.d(TAG, "Download data. dlFinished : " + dlFinished);
				
				if(dlFinished)
					downloadSuccessful(1); // IF dlSuccess(Download was initiated before) Notify the handler
				else
					mDownloadHandler.refresh();
			}
		}
	}
	
	public void initDownload() {
		mDownloadHandler.refresh();
	}
	
	// unsubscribe
	public void unsubscribe() {
		Log.i(TAG, "Unsubscribed");
		subscribed = false;
		sHandler = null;
	}
	
	// PREFERENCE SCREEN
	private Preferences preference = null;
	
	public void registerPreferenceScreen(Preferences p) {
		preference = p;
	}
	
	public void unregisterPreferenceScreen() {
		preference = null;
	}
	
	public void notifyPreferenceScreen() {
		if(preference != null) {
			preference.setupPushStatus(true);
		}
	}
	
	public int getAdvertID() {
		if(mDownloadHandler == null)
			return -1;
		
		else
			return mDownloadHandler.getAdvertId();
	}
	
	// Returns the instance of ImageLoader class
	public ImageLoader getImageLoader() {
		if(mImgLoader == null)
			mImgLoader = new ImageLoader((Context)this);
		
		return mImgLoader;
	}
	
	// Saves the directory size to memory, (Directory used to cache Downloaded Images)
	// Must be called on onPause() of any activity using ImageLoader for accuracy
	public void commitDirectorySize() {
		if(mImgLoader == null)
			mImgLoader = new ImageLoader(this);
		
		mImgLoader.getFileCache().commmitDirSize();
	}
	// END PUBLIC METHODS
}
