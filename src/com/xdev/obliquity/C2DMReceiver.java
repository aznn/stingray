package com.xdev.obliquity;

import java.io.IOException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.google.android.c2dm.C2DMBaseReceiver;
import com.xdev.obliquity.ServerQueue.OnThreadCompletedListener;

public class C2DMReceiver extends C2DMBaseReceiver {
	
	/*
	 * Entry Points (Ensure Exit)
	 * onMessage()
	 * handleRegistration() // Exit Handled in SuperClass
	 * C2DMessaging.Register() // Another Class. Possible wakeLock leak
	 * onNetworkChanged() // Must be handled appropriately
	 */
	
	private static String TAG = Config.TAG_C2DMRECIEVER;
	private static boolean DEBUG = Config.DEBUG_C2DM;
	
	private Obliquity superState;
	private Util mUtil;
	private ServerQueue mSQueue;
	
	public C2DMReceiver() {
		super(Config.C2DM_EMAIL_SENDER);
	}
	
	// Called by superclass (Initializer) ----------- CUSTOM SUPER CLASS METHOD
	@Override
	public void construct() {
		if(DEBUG) Log.d(TAG, "Service construct()");
		superState = (Obliquity)getApplication();
		mUtil = superState.getUtil();
		mSQueue = superState.getServerQueue();
	}
	
	
	// Network WIFI Connected. Will Launch If WIFI Reciever enabled in preferences. Presumed previous failed download. 
	// Retry Download
	@Override
	public void onNetworkChanged(final Context context) {
		if(DEBUG) Log.d(TAG, "Network Changed In service called");
		
		mSQueue.handleQueueOnThread(new OnThreadCompletedListener(){
			public void threadCompleted() {
				if(mSQueue.getServerQueue().contains(ServerQueue.PENDING_INTENT)) {
					mSQueue.removeQueue(ServerQueue.PENDING_INTENT);
					onMessage(context, new Intent().putExtra(Config.C2DM_MESSAGE, mUtil.getString(String.valueOf(ServerQueue.PENDING_INTENT), "Obliquity")));
				}
				
				super_die();
			}
		});
		
		// If onMessage wasnt called wake lock must be released.
	}
	
	@Override
	public void onRegistered(Context context, String registrationId) throws IOException {
		if(DEBUG) Log.d(TAG, "Registered with C2DM. RegId : " + registrationId);
	
		mSQueue.removeQueue(ServerQueue.PENDING_C2DM_REGISTER);
		mUtil.addBoolean(Config.PREF_C2DM_STATUS, true);
		mUtil.addDefaultBoolean("pushNotifications", true);
		mSQueue.registerServer("Registered");
		mUtil.addBoolean(Config.PREF_C2DM_RUNNING, false);
		
		Toast.makeText(getApplicationContext(), "Obliquity : Push Data enabled.", Toast.LENGTH_LONG).show();
		superState.notifyPreferenceScreen();
	}
	
	@Override
	public void onUnregistered(Context context) {
		if(DEBUG) Log.d(TAG, "Unregistered with C2DM");
		
		mSQueue.unRegisterServer();
		
		Editor editor = mUtil.getEditor();
		editor.putBoolean(Config.PREF_C2DM_STATUS, false);
		editor.putBoolean(Config.ALERT_ENABLED, true); // Signals a present Alert
		editor.putString(Config.ALERT_MESSAGE, Config.C2DM_USER_DISABLED_MESSAGE); // Adds Description for Alert
		editor.putBoolean(Config.PREF_C2DM_RUNNING, false);
		editor.putString(Config.PREF_DISABLED_MESSAGE, "Disabled.");
		editor.commit();
		
		mUtil.addString(Config.PREF_DISABLED_MESSAGE, "Disabled.");
		
		Toast.makeText(getApplicationContext(), "Obliquity : Push Data disabled.", Toast.LENGTH_LONG).show();
		superState.notifyPreferenceScreen();
	}

	@Override
	public void onError(Context context, String errorId) {
		
		mSQueue.registerServer(errorId);
		
		if(DEBUG) Log.e(TAG, "Error with C2DM" + errorId);
		
		if ("ACCOUNT_MISSING".equals(errorId)) {
			//no Google account on the phone; ask the user to open the account manager and add a google account and then try again
			
			failure("No Google Account on Phone.");
		} else if ("AUTHENTICATION_FAILED".equals(errorId)) {
			//bad password (ask the user to enter password and try.  Q: what password - their google password or the sender_id password? ...)
			//i _think_ this goes hand in hand with google account; have them re-try their google account on the phone to ensure it's working
			//and then try again
			
			retry();
		} else if ("TOO_MANY_REGISTRATIONS".equals(errorId)) {
			//user has too many apps registered; ask user to uninstall other apps and try again
			
			failure("Too many apps using push notifications on your phone.");
		} else if ("INVALID_SENDER".equals(errorId)) {
			//this shouldn't happen in a properly configured system
			
			Log.e("Obliquity", "INVALID_SENDER Error!"); // Enabled on production build too.
			failure("System Error.");
		} else if ("PHONE_REGISTRATION_ERROR".equals(errorId)) {
			//the phone doesn't support C2DM; inform th e user
			
			failure("Phone does not support this.");
			
		} //else: SERVICE_NOT_AVAILABLE is handled by the super class and does exponential backoff retries
	}
	
	// C2DM Service registration failed. 
	public void failure(String description) {
		Editor editor = mUtil.getEditor();
		editor.putBoolean(Config.ALERT_ENABLED, true); // Signals a present Alert
		editor.putBoolean(Config.PREF_C2DM_STATUS, false);
		editor.putString(Config.PREF_DISABLED_MESSAGE, description); // Displayed in the preferences
		editor.putString(Config.ALERT_MESSAGE, description); // Adds Description for Alert
		editor.commit();
		mUtil.addDefaultBoolean("pushNotifications", false);
		
		mSQueue.registerServer(description);
	}
	
	// C2DM Retry 
	public void retry() {
		mSQueue.addQueue(ServerQueue.PENDING_C2DM_REGISTER);
		
		mUtil.addBoolean(Config.PREF_C2DM_STATUS, true); //C2DM Status is set to False only on Confirmed Failure
		mUtil.addDefaultBoolean("pushNotifications", true);
	}
	
	@Override
	public void onMessage(Context context, final Intent intent) {
		
		if(DEBUG) Log.v(TAG, "onMessage Intent message : " + intent.getExtras().getString(Config.C2DM_MESSAGE));
		
        
        Handler handler = new Handler() {
        	@Override
        	public void handleMessage(Message message) {
        		switch(message.what) {
        			case 1:
        				downloadSuccessful(intent);
        				break;
        			case 0:
        				downloadFailed(intent);
        				break;
        		}
        		
        		// Release the wakeLock after DownloadManager Returns
        		super_die();
        	}
        };
        
        // Initiates a full Refresh
        superState.forceRefresh(handler);
        
	}
	
	// Handles Successful Download of data | Creates a Notification
	private void downloadSuccessful(Intent intent) {
		String message = "";
		
		message = intent.getExtras().getString(Config.C2DM_MESSAGE);
		
		mSQueue.removeQueue(ServerQueue.PENDING_INTENT);
		
		if(DEBUG) Log.d(TAG, "dl Successful message : " + message);

// TODO : DELETE THIS CODE
		// Checks the notification preferences and act accordingly
//		if(!((message.equals(Config.C2DM_FEED) && mUtil.getDefaultBoolean("notifFeed", true)) ||
//		   (message.equals(Config.C2DM_EVENT) && mUtil.getDefaultBoolean("notifEvent", true)))) {
//			if(DEBUG) Log.i(TAG, "Aborting Notification according to the preferences");
//			return;
//		}
		
		NotificationManager mNotificationMgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		
		// Notification text Details
		CharSequence title = "Obliquity";
		CharSequence desc = intent.getExtras().getString("text");
		Intent nIntent = null; // Notification Intent
		if(message.equals(Config.C2DM_FEED)) {
			title = Config.NOTIFICATION_USER_TITLE_FEED;	
			nIntent = new Intent(this, ActivityFeed.class);
		} else if(message.equals(Config.C2DM_EVENT)) {
			title = Config.NOTIFICATION_USER_TITLE_EVENT;
			nIntent = new Intent(this, ActivityEvent.class);
		}
		
		// Notification time
		long time = System.currentTimeMillis();
		
		Notification notification = new Notification(Config.NOTIFICATION_ICON, desc, time);
		notification.defaults |= Config.NOTIFICATION_SETTING_DEFAULTS;
		notification.flags |= Config.NOTIFICATION_SETTING_FLAGS;
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, nIntent, 0);
		notification.setLatestEventInfo(getApplicationContext(), title, desc, contentIntent);
		
		final int ID = 1;
		
		mNotificationMgr.notify(ID, notification);
		if(DEBUG) Log.i(TAG, "Notification Sent");
	}
	
	// Handle a failed Download
	private void downloadFailed(Intent intent) {
		mSQueue.addQueue(ServerQueue.PENDING_INTENT);
		mUtil.addString(Integer.toString(ServerQueue.PENDING_INTENT), intent.getExtras().getString(Config.C2DM_MESSAGE));
		
		// Caching Intent Message
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
 