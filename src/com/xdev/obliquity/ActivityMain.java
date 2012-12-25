package com.xdev.obliquity;

import java.io.FileNotFoundException;
import java.io.InputStream;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.apps.analytics.easytracking.EasyTracker;
import com.google.android.apps.analytics.easytracking.TrackedActivity;

public class ActivityMain extends TrackedActivity implements OnClickListener{
	//TODO implement the queue feature to the broadast receiver. Cache Message not working properly. DEBUG
	private final static String TAG = Config.TAG_HOMESCREEN;
	private final static boolean DEBUG = Config.DEBUG_HOME_SCREEN;
	
	Context mContext;
    Obliquity appState;
	Util mUtil;
	ServerQueue mSQueue;
    
	ImageView advert; 
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	if(DEBUG) Log.d(TAG, "onCreate");
    	
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.ac_home);
        
        mContext = this;
        appState = (Obliquity)getApplication();
        mSQueue = appState.getServerQueue();
        
        ImageView feed = (ImageView)findViewById(R.id.btn_feed);
        ImageView event = (ImageView)findViewById(R.id.btn_event);
        ImageView pref = (ImageView)findViewById(R.id.btn_preferences);
        ImageView photos = (ImageView)findViewById(R.id.btn_photos);
        ImageView obliq = (ImageView)findViewById(R.id.btn_info);
        advert = (ImageView)findViewById(R.id.img_advert);
        
        feed.setOnClickListener(this);
        event.setOnClickListener(this);
        pref.setOnClickListener(this);
        obliq.setOnClickListener(this);
        photos.setOnClickListener(this);
        
        
        feed.setOnTouchListener(mOnTouchListener);
        event.setOnTouchListener(mOnTouchListener);
        pref.setOnTouchListener(mOnTouchListener);
        obliq.setOnTouchListener(mOnTouchListener);
        photos.setOnTouchListener(mOnTouchListener);
 
        
        // Initiates the Util Class
        mUtil = appState.getUtil();
        
        handleAdvert(advert);
        
        // Gets Preference Variables
        boolean c2dm = mUtil.getBoolean(Config.PREF_C2DM_STATUS, true);
        boolean alert = mUtil.getBoolean(Config.ALERT_ENABLED, false);
        int firstRun = mUtil.getInt(Config.PREF_FIRST_RUN, -1);
        
        // Handles firstRun
        if(DEBUG) Log.i(TAG, "First Run : " + firstRun);
        if(firstRun == -1) { // First Run
        	c2dm = false;
        	firstRun();
        } 
        
        // Initiates DownloadHandler
        if(DEBUG) Log.i(TAG, "Initiating Download Handler with c2dm : " + c2dm);
        // second boolean, ImmediateAction Required
        appState.initDownloadHandler(c2dm, true, false); 
        
        // Check if any alert for user are pending
        if(DEBUG) Log.i(TAG, "Alert : " + alert);
        if(alert) {
        	Toast.makeText(this, Config.ALERT_MESSAGE, Toast.LENGTH_LONG).show();
        	mUtil.addBoolean(Config.ALERT_ENABLED, false);
        }
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	if(DEBUG) Log.d(TAG, "OnResume()");
    	
    	boolean queue = mUtil.getBoolean(Config.QUEUE_PENDING, false);
    	
        // Check if anything in queue
        if(DEBUG) Log.i(TAG, "Queue Pending : " + queue);
        if(queue) 
        	handleQueue();
    
        handleAdvert(advert);
    }
    
    // Reads adfeed.jpg from memory and sets it. Incase of error does nothing so set default adfeed image in xml
    private void handleAdvert(ImageView v) {
    	int storedID = mUtil.getInt(Config.PREF_ADVERT_ID, -1);
    	
    	// Load a stored Image
    	if(storedID != -1) {
    		InputStream is;
    		Bitmap b = null;
    		
			try {
				is = openFileInput("adfeed.jpg");
	    		b = BitmapFactory.decodeStream(is);
			} catch (FileNotFoundException e) {
				if(DEBUG) {
					e.printStackTrace();
					Log.e(TAG, "Error reading adfeed.jpg from memory, FileNotFound");
				}
				return;
			}
			
			if(b != null)
				v.setImageBitmap(b);
    	}
    }
    
    // Handles the pending queue
    public void handleQueue() {
    	if(!mUtil.isOnline()) {
    		Log.d(TAG, "handleQueue quitting : network failure");
    		return;
    	}
    	
    	mSQueue.handleQueueOnThread(null);
    }
    
    public void firstRun() {
    	// TODO : Ask for registration
    	
    	mUtil.addInt(Config.PREF_FIRST_RUN, 1);
    	mUtil.addBoolean(Config.PREF_C2DM_STATUS, false);
    	mUtil.addDefaultBoolean("pushNotifications", false); // Push Notification status in preferences
    	mSQueue.addQueue(ServerQueue.REGISTER);
    	
    	EasyTracker tracker = EasyTracker.getTracker();
    	tracker.setCustomVar(Config.CUSTOM_VAR_MODEL, "DeviceModel", android.os.Build.MODEL, 1);
    }

	@Override
	public void onClick(View v) {
		
		Intent intent = null;
		
		switch(v.getId()) {
		
			case R.id.btn_feed:
				intent = new Intent(mContext, ActivityFeed.class);
				break;
			
			case R.id.btn_event:
				intent = new Intent(mContext, ActivityEvent.class);
				break;

			case R.id.btn_preferences:
				intent = new Intent(mContext, Preferences.class);
				break;
			
			case R.id.btn_photos:
				intent = new Intent(mContext, ActivityAlbumView.class);
				break;
				
			case R.id.btn_info:
				intent = new Intent(mContext, ActivityAboutObliquity.class);
				break;
			
		}
		
		startActivity(intent);
	}
			
	
	// Animate button clicks
	OnTouchListener mOnTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent e) {
			
			ImageView mView = (ImageView)v;
			
			if (e.getAction() == MotionEvent.ACTION_DOWN) {
				mView.setAlpha(100);
				
			} else if (e.getAction() == MotionEvent.ACTION_UP) {
				mView.setAlpha(255);
			}
			
			
			return false;
		}
		
	};
	
	public static Intent createIntent(Context context) {
        Intent i = new Intent(context, ActivityMain.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return i;
    }
}