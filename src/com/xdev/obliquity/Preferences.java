package com.xdev.obliquity;

import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import com.google.android.apps.analytics.easytracking.EasyTracker;
import com.google.android.apps.analytics.easytracking.TrackedPreferenceActivity;
import com.google.android.c2dm.C2DMessaging;

public class Preferences extends TrackedPreferenceActivity implements OnSharedPreferenceChangeListener, OnPreferenceClickListener{
	
	private final String TAG = Config.TAG_PREFERENCE;
	private final boolean DEBUG = Config.DEBUG_PREFERENCE;
	
	Util mUtil;
	Obliquity appState;
	
	Context mContext;
	EasyTracker tracker;
	PreferenceScreen preferenceScreen;
	SharedPreferences sharedPreferences;
	Editor editor;
	
	CheckBoxPreference pushStatus;	
	
	String[] rsvp = {"SUMname", "SUMemail", "SUMmobile"};
	HashMap<String, EditTextPreference> mRsvp;
	
	private Boolean sendToServer = false; // keeps track of weather any rsvp details have been changed
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.ac_preference);
		
		mContext = this;
		preferenceScreen = getPreferenceScreen();
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		editor = sharedPreferences.edit();
		mRsvp = new HashMap<String, EditTextPreference>();
		
		appState = (Obliquity)getApplication();
		mUtil = appState.getUtil();
		
		pushStatus = (CheckBoxPreference)preferenceScreen.findPreference("pushNotifications");
		
		EditTextPreference ep;
		for(int i = 0; i < rsvp.length; i++) {
			ep = (EditTextPreference)preferenceScreen.findPreference(rsvp[i]);
			ep.setSummary(sharedPreferences.getString(rsvp[i], ""));
			mRsvp.put(rsvp[i], ep);
		}
		
		PreferenceScreen share = (PreferenceScreen)findPreference("share");
		share.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference pref) {
				if(DEBUG) Log.i(TAG, "Share app Clicked");
				tracker.trackEvent("Preferences", "Share", "ShareApp.", 1);
				
				StringBuilder sb = new StringBuilder();
				sb.append("Hey! Check out the official Obliquity App for Android at : \n( "); // TODO : FIX THIS
				sb.append("http://play.google.com/Obliquity");
				sb.append(" )");
				
				Intent intent = new Intent(Intent.ACTION_SEND);
			    intent.setType("text/plain");
			    intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
			    startActivity(Intent.createChooser(intent, "Share with"));
				
				return true;
			}
			
		});
		
//		PreferenceScreen credits = (PreferenceScreen)findPreference("credits");
//		credits.setOnPreferenceClickListener(new OnPreferenceClickListener() {
//			@Override
//			public boolean onPreferenceClick(Preference arg0) {
//				Intent mIntent = new Intent(mContext, ActivityAboutXdev.class);
//				startActivity(mIntent);
//				return true;
//			}
//		});
		
		PreferenceScreen cacheSize = (PreferenceScreen)findPreference("cache");
		long cache = mUtil.getLong(Config.PREF_FCACHE_SIZE, 0);
		cacheSize.setTitle("Image Cache Size: " + String.format("%.2f", cache/1024./1024.0f) + " MB");
		cacheSize.setOnPreferenceClickListener(this);
		setupPushStatus(false);
		
	}

	@Override
	public boolean onPreferenceClick(Preference p) {
		String key = p.getKey();
		if(DEBUG) Log.d(TAG, "Preference clicked, key : " + key);
		
		if(key.equals("cache")) {
			appState.clearFileCache();
			appState.commitDirectorySize();
			p.setTitle("Image Cache Size : 0.0 MB");
			return true;
		}
		
		return false;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		tracker = EasyTracker.getTracker();
		
		appState.registerPreferenceScreen(this);
		preferenceScreen.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	protected void onStop() {
		appState.unregisterPreferenceScreen();
		
		preferenceScreen.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		
		if(sendToServer) { // User details was changed
			if(DEBUG) Log.d(TAG, "Adding a send data to server to queue");
			
			// TODO : IF Needed make a seperate local variable of mSQueue
			appState.getServerQueue().addQueue(ServerQueue.REGISTER);
			
			if(!sharedPreferences.getString("SUMname", "").equals("") && 
			   !sharedPreferences.getString("SUMemail", "").equals("") &&
			   !sharedPreferences.getString("SUMmobile", "").equals(""))
			{
				if(DEBUG) Log.d(TAG, "Settings User Details to true");
				mUtil.addBoolean(Config.PREF_USER_DETAILS, true);
			}
		}
		
		super.onStop();
	}
	
	public void setupPushStatus(Boolean enabledFromService) {
		if(mUtil.getBoolean(Config.PREF_C2DM_STATUS, true)) { // If Enabled
			pushStatus.setSummary("Enabled");
			pushStatus.setEnabled(true);
			
		} else if(mUtil.getBoolean(Config.PREF_C2DM_RUNNING, false)) { // If Enabling || Disabling
			pushStatus.setSummary(mUtil.getString(Config.PREF_C2DM_RUNNING_STRING, "In Progress"));
			pushStatus.setEnabled(false);
			
		} else { // If disabled
			pushStatus.setEnabled(true);
			String tmp = mUtil.getString(Config.PREF_DISABLED_MESSAGE, "");
			pushStatus.setSummary(tmp);
		}	
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
		Log.d(TAG, "onSharedPreferenceChanged, key : " + key);
		
		// Deals with name, mobile, email
		if (key.startsWith("SUM")) {
	        EditTextPreference ePref = (EditTextPreference)findPreference(key);
	        String value = preferences.getString(key, "");
	        
			if(key.equals("SUMname") && containsDigit(value)) {
				Toast.makeText(mContext, "You have a number in your name? Kudos. But we take only letters :)", Toast.LENGTH_LONG).show();
				ePref.setText("");
				return;
			}
			
			else if(key.equals("SUMmobile") && containsCharacter(value)) {
				Toast.makeText(mContext, "Last time we checked, Mobile numbers on earth has only numbers.", Toast.LENGTH_LONG).show();
				ePref.setText("");
				return;
			}
			
		    ePref.setSummary(value);
	        sendToServer = true;
	    }
		
		// deals with pushData
		else if(key.equals("pushNotifications")) {
			if(pushStatus.isChecked()) { // Enabled
				mUtil.addBoolean(Config.PREF_C2DM_RUNNING, true);
				mUtil.addString(Config.PREF_C2DM_RUNNING, "Enabling Push Notifications");
				pushStatus.setEnabled(false);
				pushStatus.setSummary("Enabling Push Notifications");
				C2DMessaging.register(getApplication());
				
				
			}
			
			else { // If disabled
				mUtil.addBoolean(Config.PREF_C2DM_RUNNING, true);
				mUtil.addString(Config.PREF_C2DM_RUNNING, "Disabling Push Notifications");
				pushStatus.setSummary("Disabling Push Notifications");
				pushStatus.setEnabled(false);
				C2DMessaging.unregister(getApplication());
			}
		}
		
		/* deals with notifications
		else if(key.equals("notifFeed") || key.equals("notifEvent")) {
			sendToServer = true;
		}
		*/
	}
	
	// Util functions
	public final boolean containsDigit(final String s){    
	    for (char c: s.toCharArray()){
	        if(Character.isDigit(c)){
	            return true;
	        }
	    }
	    return false;
	}
	
	public final boolean containsCharacter(final String s){    
	    for (char c: s.toCharArray()){
	        if(Character.isLetter(c)){
	            return true;
	        }
	    }
	    return false;
	}
	
	public static Intent createIntent(Context context) {
        Intent i = new Intent(context, Preferences.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return i;
    }
}
