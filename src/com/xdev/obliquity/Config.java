package com.xdev.obliquity;

import android.app.Notification;

public class Config {
	
	// -------------------------------------------------------------------------------------------------------------
	// --==LOG TAGS==-----------------------------------------------------------------------------------------------
	public static final String TAG_OBLIQUITY = "--Application";
	public static final String TAG_C2DMUTIL = "--Application";
	public static final String TAG_DATAHANDLER = "--DataHandler";
	public static final String TAG_FEED = "--Feed";
	public static final String TAG_EVENT = "--Event";
	public static final String TAG_HOMESCREEN = "--main";
	public static final String TAG_C2DMRECIEVER = "--service(c2dmReciever)";
	public static final String TAG_UTIL = "--util";
	public static final String TAG_PREFERENCE = "--preference";
	public static final String TAG_SERVER_QUEUE = "--ServerQueue";
	public static final String TAG_IMGLOAD_LOADER = "--IMGLOAD--ImageLoader";
	public static final String TAG_IMGLOAD_FILECH = "--IMGLOAD--FileChche";
	public static final String TAG_IMGLOAD_MEMCACHE = "--IMGLOAD--MemCache";
	public static final String TAG_ALBUM_VIEW = "--albumView";
	public static final String TAG_ALBUM_DOWNLOADER = "--albumDownloader";
	public static final String TAG_GALLERY_VIEW = "--galleryView";
	public static final String TAG_GALLERY_DOWNLOADER = "--galleryDownloader";
	//GOGLE
	public static final String TAG_GOOGLE_BROADCAST_RECIEVER = "--broadcastRCVR";
	public static final String TAG_GOOGLE_C2DMESSENGER = "--google(c2dmMessenger)";
	
	
	// -------------------------------------------------------------------------------------------------------------
	// --==DEBUG MODE==---------------------------------------------------------------------------------------------
	public static final boolean DEBUG_C2DM = true;						//C2DMReceiver		
	public static final boolean DEBUG_DOWNLOAD_HANDLER = false;			//DownloadHandler
	public static final boolean DEBUG_OBLIQUITY_APP = false;				//Obliquity
	public static final boolean DEBUG_HOME_SCREEN = true; 				//ActivityMain
	public static final boolean DEBUG_FEED = false;						//ActivityFeed
	public static final boolean DEBUG_EVENT = false;						//Feed
	public static final boolean DEBUG_UTIL = true;						//Util
	public static final boolean DEBUG_PREFERENCE = true;				//Preference
	public static final boolean DEBUG_SERVER_QUEUE = true;				//ServerQueue
	public static final boolean DEBUG_ALBUM_VIEW = true;				//Album View
	public static final boolean DEBUG_ALBUM_DOWNLOADER = true;			//AlbumDownloader
	//IMAGELOADER
	public static final boolean DEBUG_IMGLOAD_LOADER = true;			//ImageLoader
	public static final boolean DEBUG_IMGLOAD_FILECH = true;					//FileCache
	public static final boolean DEBUG_IMGLOAD_MEMCACHE = true;					//MemoryCache 
	//GOOGLE
	public static final boolean DEBUG_GOOGLE_C2DMESSENGER = true;		//C2DMessaging
	public static final boolean DEBUG_GOOGLE_C2DMBASERECEIVER = true;	//C2DMBaseReceiver
	public static final boolean DEBUG_GOOGLE_BROADCAST_RECIEVER = true;	//C2DMBroadCastReceiver
	
	public static final boolean STRICT_MODE = true; // Strict Mode
	
	// -------------------------------------------------------------------------------------------------------------
	// --==PREFERENCES==--------------------------------------------------------------------------------------------
	public static final String PREF_NAME_MAIN = "xdev.obliquity.pref";
	public static final String PREF_STRING_RAW_JSON = "raw.json"; 
	public static final String PREF_STRING_CACHE_INTENT_MESSAGE = "cache.intent.message"; // Cached Intents Message
	public static final String PREF_C2DM_STATUS = "c2dm.status"; // C2DM Status
	public static final String PREF_FIRST_RUN = "first.run"; // First Run of app;
	public static final String PREF_RSVP_HISTORY = "rsvp.history"; // List of RSVP's events
	public static final String PREF_USER_DETAILS = "user.details"; // Is users details completed
	public static final String PREF_DISABLED_MESSAGE = "disabled.message"; // Showd as the summary in preference
	public static final String PREF_C2DM_RUNNING = "c2dm.running"; // C2DM Running ( Enabling or Disabling )
	public static final String PREF_C2DM_RUNNING_STRING = "c2dm.running.status";
	public static final String PREF_ADVERT_ID = "advert.id"; // Currently Displayed Advert Id
	public static final String PREF_FCACHE_SIZE = "fcache.size"; // FileCache size of ImageLoader
	
	// -------------------------------------------------------------------------------------------------------------
	// --==MISC==---------------------------------------------------------------------------------------------------
	public static final String ALERT_MESSAGE = "We encountered an error trying to register your device for Push Notifications";
	public static final String ALERT_ENABLED = "alert.status";
	public static final long REFRESH_VIBRATE_DURATION = 35; // time in milliseconds which the phone vibrates when refreshed
	
	// -------------------------------------------------------------------------------------------------------------
	// --==C2DM==---------------------------------------------------------------------------------------------------
	public static final String C2DM_EMAIL_SENDER = "349248144600";
	//public static final String C2DM_EMAIL_SENDER = "xdev.obliquity@gmail.com";
	public static final String C2DM_MESSAGE = "message"; // Recieved Message
	public static final String C2DM_FEED = "feed"; // Feed identifier
	public static final String C2DM_EVENT = "event"; // Event identifier
	public static final String C2DM_USER_DISABLED_MESSAGE = "Push Notification service has been deacctivated"; // Message displayed when c2dm disabled
	
	
	// -------------------------------------------------------------------------------------------------------------
	// --==NETWORK SETTINGS==---------------------------------------------------------------------------------------
	public static final int TIMEOUT_SOCKET = 3000;
	public static final int TIMEOUT_CONN = 3000;
	
	// How many Images to download concurrently by ImageLoader
	public static final int IMAGELOADER_THREADS = 2;
	
	// --------------------------------------------------------------------------------------------------------------
	// --==NOTIFICATIONS==-------------------------------------------------------------------------------------------
	public static final int NOTIFICATION_ICON = R.drawable.o_notif;
	public static final CharSequence NOTIFICATION_USER_TITLE_FEED = "Feed";
	public static final CharSequence NOTIFICATION_USER_DESC_FEED = "Feed Description";
	public static final CharSequence NOTIFICATION_USER_TITLE_EVENT = "Event";
	public static final CharSequence NOTIFICATION_USER_DESC_EVENT = "Event Description";
	// NOTIFICATION SETTINGS
	private static final int NOTIFICATION_DEFAULT_SOUND = Notification.DEFAULT_SOUND; // Sound scheme Default
	private static final int NOTIFICATION_DEFAULT_VIBRATE = Notification.DEFAULT_VIBRATE; // Vibrate scheme Default
	private static final int NOTIFICATION_DEFAULT_LIGHTS = Notification.DEFAULT_LIGHTS; // Lights scheme Default
	private static final int NOTIFICATION_FLAG_AUTO_CANCEL = Notification.FLAG_AUTO_CANCEL; // Cancels on Click
	private static final int NOTIFICATION_FLAG_ALERTS = Notification.FLAG_ONLY_ALERT_ONCE; // Vibrate|Sound once
	private static final int NOTIFICATION_FLAG_LIGHTS = Notification.FLAG_SHOW_LIGHTS; // Lights ON
	//DO NOT CHANGE
	public static final int NOTIFICATION_SETTING_DEFAULTS = NOTIFICATION_DEFAULT_SOUND|NOTIFICATION_DEFAULT_VIBRATE|NOTIFICATION_DEFAULT_LIGHTS;
	public static final int NOTIFICATION_SETTING_FLAGS = NOTIFICATION_FLAG_AUTO_CANCEL|NOTIFICATION_FLAG_ALERTS|NOTIFICATION_FLAG_LIGHTS;
	
	
	// -------------------------------------------------------------------------------------------------------------
	// --==DATE FORMATS==-------------------------------------------------------------------------------------------
	public static final String DATE_OLD_FEED = "yyyy-MM-dd HH:mm:ss";
	public static final String DATE_NEW_FEED = "E dd MMM | hh:mm a";
	public static final String DATE_OLD_EVENT = "yyyy-MM-dd";
	public static final String DATE_NEW_EVENT = "EEEE dd MMM";
	
	// -------------------------------------------------------------------------------------------------------------
	// --==SERVER CONFIG==------------------------------------------------------------------------------------------
	public static final String SERVER_AUTH_auth = "xdev.obliquity.auth"; // Compulsory Header for C2DMHelper.php in the server
	public static final String SERVER_ALL_JSON_URL = "http://obliquityindia.netau.net/AndroidCP/helpers/jsonResult.php"; // Main Download URL
	public static final String SERVER_C2DM_UTIL = "http://obliquityindia.netau.net/AndroidCP/helpers/c2dm/c2dmHelper.php"; // Reg/Unreg C2DM
	public static final String ADVERT_URL = "http://obliquityindia.netau.net/AndroidCP/img/adfeed.jpg"; // Advert URL

	
	// -------------------------------------------------------------------------------------------------------------
	// --==QUEUE==--------------------------------------------------------------------------------------------------
	public static final String QUEUE_PENDING = "queue.pending"; // If Anything server operation is in the queue
	public static final String QUEUE_MAIN_STRING = "server.queue"; // Server Queue Stored as a string seperated by ;
	public static final String QUEUE_PENDING_RSVP = "pending.rsvp"; // RSVP request pending
	public static final String QUEUE_PENDING_RSVP_COMMENT = "pending.rsvp.comment"; // Pending RSVP comment
	public static final String QUEUE_PENDING_REMOVERSVP = "pending.rsvp.remove"; // removeRSVP request pending
	public static final String QUEUE_PENDING_REGISTER = "pending.register.server"; // Register request to server pending

	
	
	// -------------------------------------------------------------------------------------------------------------
	// --==GOOGLE ANALYTICS==---------------------------------------------------------------------------------------
	public static final int CUSTOM_VAR_DATASYNC = 1; // DataSync status
	public static final int CUSTOM_VAR_MODEL = 2; // Model of the phone
	
}
