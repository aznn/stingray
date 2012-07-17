package com.xdev.obliquity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.util.Log;

public class Util {
	
	private static String TAG = Config.TAG_UTIL;
	private static boolean DEBUG = Config.DEBUG_UTIL;
	
	// Preferences
	private Editor mainEditor;
	private SharedPreferences mainPrefs;
	private SharedPreferences defPrefs;
	private Editor defEditor;
	
	// Internet Status
	ConnectivityManager cm;
	
	// Constructor
	public Util(Context context) {
		// Init Prefs
		this.mainPrefs = context.getSharedPreferences(Config.PREF_NAME_MAIN, Context.MODE_PRIVATE);
		this.mainEditor = mainPrefs.edit();
		this.defPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		this.defEditor = defPrefs.edit();
		// Init Internet Status
		cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
	}
	
	
	// Checks Internet Status
	public boolean isOnline() {
		/* OLD IMPLEMENTATION
		if(cm != null) {
			NetworkInfo ni = cm.getActiveNetworkInfo();
		
			return (ni != null && ni.isAvailable() && ni.isConnected()) ? true : false;
		}
		return false;
		*/
		if(cm == null)
			return false;
		
		final NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		if (activeNetwork != null && activeNetwork.getState() == NetworkInfo.State.CONNECTED) {
		    return true;
		} else {
		    return false;
		} 
	}

	// ------------------------------------------------------------------
	// -------------------==OTHER UTILS==---------------------------
	
	public String getDeviceID(Context context) {
		String deviceID =  Secure.getString(context.getContentResolver(), Secure.ANDROID_ID); 
		if(DEBUG) Log.d(TAG, "Returning DeviceID : " + deviceID);
		return deviceID;
	}
	
	// ------------------------------------------------------------------
	// -------------------==MAIN PREFERENCES==---------------------------
		
		public Editor getEditor() {
			return mainEditor;
		}
		
		public void addString(String key, String val) {
			mainEditor.putString(key, val);
			mainEditor.commit();
		}
		
		public void addBoolean(String key, boolean val) {
			mainEditor.putBoolean(key, val);
			mainEditor.commit();
		}
		
		public void addInt(String key, int val) {
			mainEditor.putInt(key, val);
			mainEditor.commit();
		}
		
		public void addLong(String key, long val) {
			mainEditor.putLong(key, val);
			mainEditor.commit();
		}
		
		public void removeKey(String key) {
			mainEditor.remove(key);
			mainEditor.commit();
		}
		
		public String getString(String key, String defValue) {
			return mainPrefs.getString(key, defValue);
		}
		
		public boolean getBoolean(String key, boolean defValue) {
			return mainPrefs.getBoolean(key, defValue);
		}
		
		public int getInt(String key, int defValue) {
			return mainPrefs.getInt(key, defValue);
		}
		
		public long getLong(String key, long defValue) {
			return mainPrefs.getLong(key, defValue);
		}
		
		// -------------------------------------------------------------
		// -------------------==DEFAULT PREFERENCES==---------------------------
			
		public Editor getDefaultEditor() {
			return defEditor;
		}
		
		public void addDefaultString(String key, String val) {
			defEditor.putString(key, val).commit();
		}
		
		public void addDefaultBoolean(String key, boolean val) {
			defEditor.putBoolean(key, val).commit();
		}
		
		public void addDefaultInt(String key, int val) {
			defEditor.putInt(key, val).commit();
		}
		
		public void removeDefaultKey(String key) {
			defEditor.remove(key).commit();
		}
		
		public String getDefaultString(String key, String defValue) {
			return defPrefs.getString(key, defValue);
		}
		
		public boolean getDefaultBoolean(String key, boolean defValue) {
			return defPrefs.getBoolean(key, defValue);
		}
		
		public int getDefaultInt(String key, int defValue) {
			return defPrefs.getInt(key, defValue);
		}

	
}
