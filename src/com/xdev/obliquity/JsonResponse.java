package com.xdev.obliquity;

import java.util.List;

import com.google.gson.annotations.SerializedName;
		
public class JsonResponse {
		
	public class Feeds {
		
		@SerializedName("message")
		public String message;
		
		@SerializedName("date")
		public String timeStamp;
		
		@SerializedName("feedid")
		public int feedid;
	}	
		
	public class Events {
		
		@SerializedName("title")
		public String title;
		
		@SerializedName("description")
		public String description;
		
		@SerializedName("date")
		public String date;
		
		@SerializedName("eventId")
		public int eventId;
		
		@SerializedName("hasImage")
		public int hasImage;
		
		@SerializedName("url")
		public String largerImageURL;
	}	
		
	public List<JsonResponse.Events> events;
	public List<JsonResponse.Feeds> feeds;
		
	@SerializedName("advert")
	public int advert;
		
}