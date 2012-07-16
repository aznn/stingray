package com.xdev.obliquity;

import android.os.Bundle;
import android.view.Window;
import android.webkit.WebView;

import com.google.android.apps.analytics.easytracking.TrackedActivity;

public class ActivityAboutXdev extends TrackedActivity{

	 @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
	        setContentView(R.layout.ac_aboutxdev);
	        
	        WebView mWebView = (WebView)findViewById(R.id.webview);
	        mWebView.loadUrl("https://fbcdn-sphotos-a.akamaihd.net/hphotos-ak-ash3/s480x480/529196_10151036280714432_513840100_n.jpg");
	    }
	 
}
