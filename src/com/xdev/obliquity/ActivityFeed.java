package com.xdev.obliquity;

import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.analytics.easytracking.EasyTracker;
import com.google.android.apps.analytics.easytracking.TrackedActivity;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.xdev.obliquity.JsonResponse.Feeds;

public class ActivityFeed extends TrackedActivity {
	
	private final String TAG = Config.TAG_FEED;
	private final boolean DEBUG = Config.DEBUG_FEED;
	
	Context mContext;
	
	Handler mHandler; // Feed Handler
	Obliquity appState; // Application Subclass
	DownloadHandler dlHandler; // Reference to Download Manager
	Util mUtil;
	FeedListAdapter adapter; // Adapter
	PullToRefreshListView list;
	ListView mActualList; // Pull to refresh's PullToRefreshListView is not the actual list. This is. 
	Vibrator mVibrator;
	
	boolean listInitialized = false;
	boolean refreshing = false;
	
	// LIFECYCLE FUNCTIONS
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.ac_feed);
    
        appState = (Obliquity)getApplication();
        mUtil = appState.getUtil();
        mContext = this;
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
        // Handles Messages from the Obliquity class (Application class)
        mHandler = new Handler() {
        	@Override
			public void handleMessage(Message message) {
        		Log.i(TAG, "Message Recieved what : "+ message.what);
        		switch(message.what) {
        			case 1:
        				downloadSuccessful();
        				break;
        			case 0:
        				downloadFailed();
        				break;
        			case 2: // isOnline returns false
        			case 99: // Connected yet server not responding
        				noInternetAccess(message.what);
        				break;
        			case 11: 
        				cacheSuccessful();
        				break;
        			// Cache Failed would not propogate, Handled by Obliquity class
        		}	
        	}
        };
        
        Log.d(TAG, "Feed Subscribing");
		// Second boolean, ImmediateAction
		dlHandler = appState.getDownloadHandler(mHandler, mUtil.getBoolean(Config.PREF_C2DM_STATUS, true), true); // Subscribe to the Download Manager
		
	}
	
	@Override
	public void onResume() { // TODO, immideateAction POSSIBLE LOOPHOLE
		super.onResume();
		Log.d(TAG, "isOnline :" + mUtil.isOnline());
		
		if(!mUtil.isOnline())
			makeToast("No internet access. These may not be upto date.", Toast.LENGTH_LONG);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		Log.d(TAG, "Feed unsubscribing");
		appState.unsubscribe();
	}
	
	// END LIFECYCLE FUNCTIONS
	
	// Handles refresh calls
	public void refresh() {
		mVibrator.vibrate(Config.REFRESH_VIBRATE_DURATION);
		
		// InternetAccess will be handled by application & DataHandler. Handled Here to avoid wasteful function calls
		if(mUtil.isOnline()) {
			refreshData();
		} else {
			list.onRefreshComplete();
			noInternetAccess(2);
		}
	}
	
	// Called by Obliquity(APP) on failed download
	private void downloadFailed() {
		makeToast("We encountered an error while trying to refresh.", Toast.LENGTH_LONG);
		
		refreshing = false;
		if(list != null)
			list.onRefreshComplete();
		
		if(askLoadCache())
			dlHandler.loadCache();
		else
			displayEmpty();
	}
	
	// Called by Obliquity(APP) on successful download
	private void downloadSuccessful() {
		setAdapter(dlHandler.getFeeds());
	}
	
	// Called by Obliquity on successful cache load
	private void cacheSuccessful() {
		
		setAdapter(dlHandler.getCachedFeeds());
		
	}
	
	// Asks User weather to load data from Cache
	private boolean askLoadCache() {
		return true; // TODO
	}
	
	// No Cache or Downloaded data to display. modify UI and alert user
	public void displayEmpty() {
		// TODO
	}
	
	// No Internet Access. Load Cache if possible. Else call displayEmpty()
	public void noInternetAccess(int what) {
		if(DEBUG) Log.e(TAG, "No Internet Access, Trying to Load from Cache");

		switch(what) {
			case 2:
				makeToast("Cannot refresh without an active internet connection.", Toast.LENGTH_SHORT);
				break;
			case 99:
				makeToast("Sorry, We are facing some issues right now. Please try again later. ", Toast.LENGTH_LONG);
		}
		
		refreshing = false;
		if(list != null)
			list.onRefreshComplete();
	}
	
	private void makeToast(String text, int duration) {
		Toast.makeText(mContext, text, duration).show();
	}
	
	private void refreshData() {
		dlHandler.refresh();
	}
	
	// -----------------------------------------------------------------------
	// Initialize Adapter
	private void setAdapter(List<Feeds> values) {
		Log.i(TAG, "Setting up Adapter with count : " + values.size());
		if(listInitialized) { // Returning from a refresh() call
			adapter.changeDataSet(values); 
			if(refreshing) {
				makeToast("Refresh completed", Toast.LENGTH_SHORT);
				Date d = new Date();
				CharSequence s  = DateFormat.format("hh:mm a", d.getTime());
				list.onRefreshComplete();
				list.setLastUpdatedLabel("Last Updated : " + s);
				refreshing = false;
			}
		} else {
			adapter = new FeedListAdapter(mContext, values);
			list = (PullToRefreshListView)findViewById(R.id.list);
			mActualList = (ListView) list.getRefreshableView();
			
			list.setOnRefreshListener(new OnRefreshListener(){
				@Override
				public void onRefresh() {
					refreshing = true;
					refresh();
					
				}
			});
			
			mActualList.setAdapter(adapter);
			adapter.init();
			listInitialized = true;
		}
	}
	
	// ADAPTER
	public class FeedListAdapter extends ArrayAdapter<Feeds> {
    	private final String TAG = "--Feed--Adapter";
    	
    	private Context context;
    	private EasyTracker tracker;
    	private List<Feeds> values;
    	
    	// View Holder Class
    	private class ViewHolder {
    		public TextView txtMessage;
    		public TextView txtDate;
    	}
    	
    	// Constructor
    	public FeedListAdapter(Context context, List<Feeds> values) {
    		super(context, R.layout.feed_row, values);
    		Log.d(TAG, "Adapter Constructor");
    		this.context = context;
    		this.values = values;
    		
    		tracker = EasyTracker.getTracker();
    	}
    	
    	// Called when Adapter is bound to listview
    	public void init() {
    		mActualList.setOnItemLongClickListener(new OnItemLongClickListener() {

    			@Override
    			public boolean onItemLongClick(AdapterView<?> parent, View v, int pos, long id) {
    				if(DEBUG) Log.i(TAG, "Item Long Clicked pos : " + pos);
    				tracker.trackEvent("Feed", "LongClick", "Feed SharedWith.", 1);
    				
    				ViewHolder holder = (ViewHolder) v.getTag();
    				
    				StringBuilder sb = new StringBuilder();
    				sb.append("Obliquity's Status on (");
    				sb.append(holder.txtDate.getText());
    				sb.append(") : \n");
    				sb.append("\"");
    				sb.append(holder.txtMessage.getText());
    				sb.append("\"");
    				sb.append("\n");
    				
    				Intent intent = new Intent(Intent.ACTION_SEND);
    			    intent.setType("text/plain");
    			    intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
    			    startActivity(Intent.createChooser(intent, "Share with"));
    				
    				return true;
    			}
    		
    		});
    	}
    	
    	// Swaps dataset and notifies listview
    	public void changeDataSet(List<Feeds> values) {
    		this.values = values;
    		this.notifyDataSetChanged();
    	}
    	
    	View rowView;
    	ViewHolder holder;
    	
    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		Log.d(TAG, "GetView : " + position);
    		
    		rowView = convertView;
    		
    		if (rowView == null) {
    			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    			rowView = inflater.inflate(R.layout.feed_row, parent, false);
    			
    			holder = new ViewHolder();
    			holder.txtMessage = (TextView) rowView.findViewById(R.id.txtMessage);
    			holder.txtDate = (TextView) rowView.findViewById(R.id.txtDate);
    			rowView.setTag(holder);
    		} else {
    			holder = (ViewHolder)rowView.getTag();
    		}
    		
    		holder.txtMessage.setText(values.get(position).message);
    		holder.txtDate.setText(values.get(position).timeStamp);
    		
    		return rowView;
    	}
    }
}
