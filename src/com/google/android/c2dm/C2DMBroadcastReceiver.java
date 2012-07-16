/*
 */
package com.google.android.c2dm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.xdev.obliquity.Config;

/**
 * Helper class to handle BroadcastReciver behavior.
 * - can only run for a limited amount of time - it must start a real service 
 * for longer activity
 * - must get the power lock, must make sure it's released when all done.
 * 
 */
public class C2DMBroadcastReceiver extends BroadcastReceiver {
    
	private static boolean DEBUG = Config.DEBUG_GOOGLE_BROADCAST_RECIEVER;
	private static String TAG = Config.TAG_GOOGLE_BROADCAST_RECIEVER;
	
    @Override
    public final void onReceive(Context context, Intent intent) {
        
    	//------------------------------------------------------------------------------- CUSTOM CODE
    	if(DEBUG) Log.d(TAG, "Broadcastrcvr Activated : " + intent.getAction());

    	if (intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE"))
    	{
    		NetworkInfo info = (NetworkInfo)intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
    		SharedPreferences pref = context.getSharedPreferences(Config.PREF_NAME_MAIN, Context.MODE_PRIVATE);
    		
    		Boolean bQueue = pref.getBoolean(Config.QUEUE_PENDING, false); //Anything in Queue pending
    		Boolean bNet = info.getState().equals(NetworkInfo.State.CONNECTED);
    		
    		if(DEBUG) Log.d(TAG, "Queue Pending : " + bQueue.toString() + "| State Connected : " + bNet.toString());
    		
	    	if (bQueue && bNet) {     	//---------------------------------------------------------------------------------- END CUSTOM CODE
	    		if(DEBUG) Log.d(TAG, "Running Intent in Service, OnNetworkChanged");
	            C2DMBaseReceiver.runIntentInService(context, intent);
	    	}
    	}
    	else
    	{
            C2DMBaseReceiver.runIntentInService(context, intent);
            setResult(Activity.RESULT_OK, null /* data */, null /* extra */); 
    	}   
    }
}