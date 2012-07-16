package com.xdev.obliquity;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebView;

public class ActivityImageViewer extends Activity{

private static final String HTML_FORMAT = "<html><body style=\"text-align: center; background-color: black; vertical-align: center;\"><img src = \"%s\" /></body></html>";
private WebView mWebView;

@Override
protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.ac_imageviewer);
    mWebView = (WebView) findViewById(R.id.webview);

	String URL = getIntent().getExtras().getString("URL");
    final String html = String.format(HTML_FORMAT, URL);
    
    mWebView.getSettings().setUseWideViewPort(false); 
    mWebView.getSettings().setBuiltInZoomControls(true);
    mWebView.setBackgroundColor(Color.BLACK);
    mWebView.loadDataWithBaseURL("", html, "text/html", "UTF-8", "");
    }
}