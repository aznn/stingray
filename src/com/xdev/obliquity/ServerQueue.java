package com.xdev.obliquity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.util.Log;

import com.google.android.c2dm.C2DMessaging;

public class ServerQueue {
	
	private Context mContext;
	private Util mUtil;
	
	private boolean DEBUG = Config.DEBUG_SERVER_QUEUE;
	private String TAG = Config.TAG_SERVER_QUEUE;
	
	// Constructor
	ServerQueue(Context mContext, Util mUtil) {
		this.mContext = mContext;
		this.mUtil = mUtil;
	}
	
	
	// ----------------------------------------------------------------------
	
	/*
	 * Executes pending Queue on a single thread
	 * A callback must be registered for when the thread completes 
	 */
	public void handleQueueOnThread(final OnThreadCompletedListener l) {
		
		// Queue of current Server Queue of List<Integers>
		final List<Integer> queue = getServerQueue();
		
		for(int i = 0; i < queue.size(); i++)
			Log.e(TAG, i + " : " + queue.get(i));
		
		new Thread() {
			@Override
			public void run() {
				
				if(queue != null) {
					for(int i = 0, l = queue.size(); i < l; i++) {
						Integer x = queue.get(i);
						
						if(x != null)
						switch(queue.get(i)) {
							case REGISTER :
								registerServer(mUtil.getString(Config.QUEUE_PENDING_REGISTER, ""));
								break;
							
							case UNREGISTER :
								unRegisterServer();
								break;
								
							case RSVP :
								RSVP(mUtil.getInt(Config.QUEUE_PENDING_RSVP, -99), mUtil.getString(Config.QUEUE_PENDING_RSVP_COMMENT, ""));
								break;
								
							case CANCEL_RSVP :
								removeRSVP(mUtil.getInt(Config.QUEUE_PENDING_REMOVERSVP, -99));
								break;
						}
					}
				}
				
				if(l != null) 
					l.threadCompleted();
			}
			
		}.start();
	}
	
	
	/*
	 * Do a specified single request on thread
	 */
	public void runOnThread(final int request, final int event_id, final String c2dmStatus, final String comments) {
		new Thread() {
			public void run() {
				switch(request) {
					case REGISTER :
						registerServer(c2dmStatus);
						break;
						
					case UNREGISTER :
						unRegisterServer();
						break;
						
					case RSVP :
						RSVP(event_id, comments);
						break;
						
					case CANCEL_RSVP :
						removeRSVP(event_id);
						break;
						
					case PENDING_C2DM_REGISTER :
						C2DMessaging.register(mContext);
						break;
				}
			}
			
		}.start();
	}

	
	// -----------------------------------------------------------------------
	
	/*
	 * Sends a register/update device information request to the server
	 */
	private boolean registerInProgress = false;
	public void registerServer(String c2dmStatus) {
		if(DEBUG) Log.d(TAG, "registerServer initiated");
		
		if(registerInProgress) {
			Log.e(TAG, "registerServer already in progress");
			return;
		}
		
		if(!mUtil.isOnline()) {
			if(DEBUG) Log.d(TAG, "Queing registerServer, no connection");
			addQueue(REGISTER);
			mUtil.addString(Config.QUEUE_PENDING_REGISTER, c2dmStatus);
			return;
		}
		
		registerInProgress = true;
		
		// Create a new HttpClient and Post Header
	    HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost(Config.SERVER_C2DM_UTIL);
	    
	    try {
	        // POST Data
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(9);
	        nameValuePairs.add(new BasicNameValuePair("auth", Config.SERVER_AUTH_auth));
	        nameValuePairs.add(new BasicNameValuePair("service", "register"));
	        nameValuePairs.add(new BasicNameValuePair("registrationID", C2DMessaging.getRegistrationId(mContext)));
	        nameValuePairs.add(new BasicNameValuePair("deviceID", mUtil.getDeviceID(mContext)));
	        nameValuePairs.add(new BasicNameValuePair("name", mUtil.getDefaultString("SUMname", "")));
	        nameValuePairs.add(new BasicNameValuePair("email", mUtil.getDefaultString("SUMemail", "")));
	        nameValuePairs.add(new BasicNameValuePair("mobile", mUtil.getDefaultString("SUMmobile", "")));
	        nameValuePairs.add(new BasicNameValuePair("model", android.os.Build.MODEL));
	        nameValuePairs.add(new BasicNameValuePair("status", c2dmStatus));
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

	        // Execute HTTP Post Request
	        httpclient.execute(httppost);
	        
		    removeQueue(REGISTER);
		    registerInProgress = false;
		    
		    if(DEBUG) Log.i(TAG, "Registration Request sent Successfully");
		    
	    } catch (ClientProtocolException e) {
	        if(DEBUG) Log.e(TAG, "Registration error : ClientProtocolException : " + e.getMessage());
			addQueue(REGISTER);
			mUtil.addString(Config.QUEUE_PENDING_REGISTER, c2dmStatus);
	    } catch (IOException e) {
	    	if(DEBUG) Log.e(TAG, "Registration error : IOException : " + e.getMessage());
			addQueue(REGISTER);
			mUtil.addString(Config.QUEUE_PENDING_REGISTER, c2dmStatus);
	    }
	}
	
	
	/*
	 * Unregister the device from the server
	 */
	public void unRegisterServer() {
		if(DEBUG) Log.d(TAG, "unRegisterServer initiated");
		
		if(!mUtil.isOnline()) {
			if(DEBUG) Log.d(TAG, "Queing unRegisterServer, no connection");
			addQueue(UNREGISTER);
			return;
		}
		
		// Create a new HttpClient and Post Header
	    HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost(Config.SERVER_C2DM_UTIL);

	    try {
	        // POST Data
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(5);
	        nameValuePairs.add(new BasicNameValuePair("auth", Config.SERVER_AUTH_auth));
	        nameValuePairs.add(new BasicNameValuePair("service", "unregister"));
	        nameValuePairs.add(new BasicNameValuePair("registrationID", C2DMessaging.getRegistrationId(mContext)));
	        nameValuePairs.add(new BasicNameValuePair("deviceID", mUtil.getDeviceID(mContext)));
	        nameValuePairs.add(new BasicNameValuePair("status", "Disabled by User"));
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

	        // Execute HTTP Post Request
	        httpclient.execute(httppost);
	        
		    removeQueue(UNREGISTER);
		    if(DEBUG) Log.i(TAG, "Registration Request sent Successfully");
		    
	    } catch (ClientProtocolException e) {
	        if(DEBUG) Log.e(TAG, "Registration error : ClientProtocolException : " + e.getMessage());
	        addQueue(UNREGISTER); // queue a send_to_server event
	    } catch (IOException e) {
	    	if(DEBUG) Log.e(TAG, "Registration error : IOException : " + e.getMessage());
	    	addQueue(UNREGISTER); // queue a send_to_server event
	    }
	}
	
	
	/*
	 * RSVP to an event
	 */
	public void RSVP(int event_id, String comments) {
		if(DEBUG) Log.d(TAG, "RSVP request initiated");
		
		if(!(event_id >= 0)) {
			if(DEBUG) Log.d(TAG, "ERROR event_id : " + event_id);
			return;
		}
		
		if(!mUtil.isOnline()) {
			if(DEBUG) Log.d(TAG, "Queing RSVP, no connection");
			addQueue(RSVP);
			mUtil.addInt(Config.QUEUE_PENDING_RSVP, event_id);
			mUtil.addString(Config.QUEUE_PENDING_RSVP_COMMENT, comments);
			return;
		}
		
		// Create a new HttpClient and Post Header
	    HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost(Config.SERVER_C2DM_UTIL);

	    try {
	        // POST Data
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
	        nameValuePairs.add(new BasicNameValuePair("auth", Config.SERVER_AUTH_auth));
	        nameValuePairs.add(new BasicNameValuePair("service", "RSVP"));
	        nameValuePairs.add(new BasicNameValuePair("eventID", Integer.toString(event_id)));
	        nameValuePairs.add(new BasicNameValuePair("deviceID", mUtil.getDeviceID(mContext)));
	        nameValuePairs.add(new BasicNameValuePair("comments", comments));
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

	        // Execute HTTP Post Request
	        httpclient.execute(httppost);
	        
		    removeQueue(RSVP);
		    addRSVPHistory(event_id);
		    if(DEBUG) Log.i(TAG, "RSVP Request sent Successfully");
		    return;
	    } catch (ClientProtocolException e) {
	        if(DEBUG) Log.e(TAG, "RSVP error : ClientProtocolException : " + e.getMessage());
	    } catch (IOException e) {
	    	if(DEBUG) Log.e(TAG, "RSVP error : IOException : " + e.getMessage());
	    }
	    
	    // on failure
    	addQueue(RSVP);
    	mUtil.addInt(Config.QUEUE_PENDING_RSVP, event_id); // queue a rsvp request
    	mUtil.addString(Config.QUEUE_PENDING_RSVP_COMMENT, comments);
	}
	
	
	/*
	 * Cancel RSVP to an event
	 */
	public void removeRSVP(int event_id) {
		if(DEBUG) Log.d(TAG, "removeRSVP request initiated");
		
		if(!(event_id >= 0)) {
			if(DEBUG) Log.d(TAG, "ERROR event_id : " + event_id);
			return;
		}
		
		if(!mUtil.isOnline()) {
			if(DEBUG) Log.d(TAG, "Queing removeRSVP, no connection");
			addQueue(CANCEL_RSVP);
			mUtil.addInt(Config.QUEUE_PENDING_REMOVERSVP, event_id);
			return;
		}
		
		// Create a new HttpClient and Post Header
	    HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost(Config.SERVER_C2DM_UTIL);

	    try {
	        // POST Data
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
	        nameValuePairs.add(new BasicNameValuePair("auth", Config.SERVER_AUTH_auth));
	        nameValuePairs.add(new BasicNameValuePair("service", "removeRSVP"));
	        nameValuePairs.add(new BasicNameValuePair("eventID", Integer.toString(event_id)));
	        nameValuePairs.add(new BasicNameValuePair("deviceID", mUtil.getDeviceID(mContext)));
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

	        // Execute HTTP Post Request
	        httpclient.execute(httppost);
	        
	        removeQueue(CANCEL_RSVP);
	        if(DEBUG) Log.i(TAG, "removeRSVP Request sent Successfully");
		 
	    } catch (ClientProtocolException e) {
	        if(DEBUG) Log.e(TAG, "removeRSVP error : ClientProtocolException : " + e.getMessage());
	        addQueue(CANCEL_RSVP);
	        mUtil.addInt(Config.QUEUE_PENDING_REMOVERSVP, event_id); // queue a rsvp request
	    } catch (IOException e) {
	    	if(DEBUG) Log.e(TAG, "removeRSVP error : IOException : " + e.getMessage());
	    	addQueue(CANCEL_RSVP);
	    	mUtil.addInt(Config.QUEUE_PENDING_REMOVERSVP, event_id); // queue a rsvp request
	    }
	}
	
	
	//-----------------------------------------------------------------------
	/*
	 * Manage RSVP History
	 */
	private String RSVPHistory = null; // Local history copy, stored as a string seperated by ';'
	public void addRSVPHistory(int event_id) {
		if(DEBUG) Log.d(TAG, "addRSVPHistory, event : " + event_id);
		
		String event = Integer.toString(event_id);
		
		if(RSVPHistory == null) 
			RSVPHistory = mUtil.getString(Config.PREF_RSVP_HISTORY, "");
		
		if(DEBUG) Log.d(TAG, "RSVPHistory, preQueue : " + RSVPHistory);
		
		// Checks if already queued. If not queues
		if(RSVPHistory.contains(event)) {
			return;
		}
		else
			RSVPHistory = RSVPHistory.concat(";" + event);
		
		mUtil.addString(Config.PREF_RSVP_HISTORY, RSVPHistory);
		
		if(DEBUG) Log.d(TAG, "RSVPHistory, postQueue : " + RSVPHistory);
	}
	
	public void removeRSVPHistory(int event) {
		if(DEBUG) Log.d(TAG, "removeRSVPRequest, event : " + event);
		
		if(RSVPHistory == null)
			RSVPHistory = mUtil.getString(Config.PREF_RSVP_HISTORY, "");
		
		if(DEBUG) Log.d(TAG, "removeRSVPRequest, preQueue : " + RSVPHistory);
		
		if(RSVPHistory.equals("")) {
			return;
		}
		
		// Removes ; + event
		RSVPHistory = RSVPHistory.replace(";" + event, "");
	
		mUtil.addString(Config.PREF_RSVP_HISTORY, RSVPHistory);
		
		if(DEBUG) Log.d(TAG, "removeRSVPRequest, postQueue : " + RSVPHistory);
	}
	
	//-----------------------------------------------------------------------

	
	/*
	 * Manage failed server request queue
	 */
	private String Queue = null; // Local history copy, stored as a string seperated by ';'
	public void addQueue(int request) {
		if(DEBUG) Log.d(TAG, "addServerRequest, event : " + request);
		
		String sRequest = Integer.toString(request);
		
		if(Queue == null) 
			Queue = mUtil.getString(Config.QUEUE_MAIN_STRING, "");
		
		if(DEBUG) Log.d(TAG, "addServerRequest, preQueue : " + Queue);
		
		// Checks if already queued. If not queues
		if(Queue.contains(sRequest))
			return;
		else {
			
// If we dont add the first semicolon we cant remove the first item ( VERIFY )
//			if(Queue.equals(""))
//				Queue = Queue.concat(sRequest);
//			else
			
				Queue = Queue.concat(";" + sRequest);
		}
		
		mUtil.addString(Config.QUEUE_MAIN_STRING, Queue);
		mUtil.addBoolean(Config.QUEUE_PENDING, true);
		
		if(DEBUG) Log.d(TAG, "addServerRequest, postQueue : " + Queue);
	}
	
	public void removeQueue(int request) {
		if(DEBUG) Log.d(TAG, "removeServerRequest, event : " + request);
		
		String sRequest = Integer.toString(request);
		
		if(Queue == null)
			Queue = mUtil.getString(Config.QUEUE_MAIN_STRING, "");
		
		if(DEBUG) Log.d(TAG, "removeServerRequest, preQueue : " + Queue);
		
		if(Queue.equals("")) {
			mUtil.addBoolean(Config.QUEUE_PENDING, false);
			return;
		}
		
		// Removes ; + event
		Queue = Queue.replace(";" + sRequest, "");
		
		if(Queue.equals("")) 
			mUtil.addBoolean(Config.QUEUE_PENDING, false);
		else
			mUtil.addBoolean(Config.QUEUE_PENDING, true);
		
		mUtil.addString(Config.QUEUE_MAIN_STRING, Queue);
		
		if(DEBUG) Log.d(TAG, "removeServerRequest, postQueue : " + Queue);
	}
	
	
	/*
	 * Returns List<Integer> of queued server requests
	 * Returns null on empty queue. Do a null check!
	 */
	public List<Integer> getServerQueue() {
		if(DEBUG) Log.d(TAG, "getQueue called");
		
		List<Integer> qList = new ArrayList<Integer>();
		
		if(Queue == null)
			Queue = mUtil.getString(Config.QUEUE_MAIN_STRING, "");
		
		if(DEBUG) Log.d(TAG, "Queue : " + Queue);
		
		if(Queue.equals("")) return null;
		
		String[] qArray = Queue.split(";");
		
		for(int i = 0; i < qArray.length; i++) {
			qList.add(Integer.parseInt(qArray[i]));
		}
		
		return qList;
			
	}
	
	
	//-----------------------------------------------------------------------
	
	public interface OnThreadCompletedListener {
	       void threadCompleted();
	}
	
	// Server Request Identifiers
	public static final int REGISTER = 1;
    public static final int UNREGISTER = 2;
	public static final int RSVP = 3;
	public static final int CANCEL_RSVP = 4;
	public static final int PENDING_INTENT = 5;
	public static final int PENDING_C2DM_REGISTER = 6;
}