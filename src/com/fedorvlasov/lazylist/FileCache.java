package com.fedorvlasov.lazylist;

import java.io.File;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.PriorityQueue;
import java.util.Stack;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.xdev.obliquity.Config;
import com.xdev.obliquity.Util;

public class FileCache {
    
	private static final String TAG = Config.TAG_IMGLOAD_FILECH;
    private static final boolean DEBUG = Config.DEBUG_IMGLOAD_FILECH;
	private static final String KEY = Config.PREF_FCACHE_SIZE; //FileCache size preference key
    
    private File cacheDir;
    private Util mUtil;
    private Context mContext;
    
    private long mDirSize; // Current DirectorySize
    private boolean running; // If Calculating DirSize in a thread. If true mDirSize must not be accessed
    private int tempDirSize; // If any updateDirSize() calls made during 'running' add it here. 
    
    // Directory loaded in a priorityQueue according to when the file was created
    PriorityQueue<String> pQueue;
    Hashtable<String, String> mFiles; // mFiles<URLHash, URLHash_UnixTime> 
    
    final long maxDirectorySize = 20 * 1024 * 1024; 		// 20MB
    final long cleanupDirCache = 8 * 1024 * 1024; 		// 8 MB cleared when cache becomes full
    
    
    public FileCache(Context context, Util util){
        //Find the dir to save cached images
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            cacheDir=new File(android.os.Environment.getExternalStorageDirectory(),"Obliquity");
        else
            cacheDir=context.getCacheDir();
        if(!cacheDir.exists())
            cacheDir.mkdirs();
        
        mUtil = util;
        running = false;
        tempDirSize = 0;
        mContext = context;
        
        if((mDirSize = mUtil.getLong(KEY, -99)) == -99) 
        	initDirSize(); // If Util has no dirSize calculated
        
        if(DEBUG) Log.d(TAG, "MAX CACHE SIZE : " + maxDirectorySize + " MB");
        
        setupDirectoryOverflow();
        
        if(DEBUG) Log.d(TAG, "FileCache mDirSize : " + mDirSize/1024./1024. + "MB");
    }
    
    /*
     * Calculates dirSize in a thread. While thread is in progress running will be true
     * Check status of running in isRunning() which is threadsafe
     */
    private void initDirSize() {
    	running = true;
    	new Thread() {
    		@Override
    		public void run() {
    			mDirSize = calcDirSize();
    			mDirSize += tempDirSize;
    			commmitDirSize();
    			
    			if(DEBUG) Log.d(TAG, "DirSize calculated in thread : " + mDirSize/1024./1024. + "MB");
    			
    			setRunning(false);
    		}
    	}.start();
    }
    
    /*
     * Loads current directory's files in a priorityQueue according to the time the file was created
     * Top of the priority queue contains the file which was created first. Hence in case of memCache Size overflow
     * root should be deleted. 
     * Files are stored in the format urlHash|unixTime
     */
    private void setupDirectoryOverflow() {
    	Comparator<String> comparator = new UnixTimeComparator();
    	// Consider increasing the initial capcity. 
    	pQueue = new PriorityQueue<String>(50, comparator);  
    	mFiles = new Hashtable<String, String>();
    	
    	File[] files=cacheDir.listFiles();
        if(files==null) {
            pQueue = null;
        	return;
        }
        
        String x;
        for(File f:files) {
        	x = f.getName();
            pQueue.add(x);
            mFiles.put(x.substring(0, x.lastIndexOf("_")), x);
            if(DEBUG) Log.d(TAG, "Processing file : " + x);
        }
    }
    
    // Return File if exists, else NULL
    public File getFileExists(String url) {
    	String filename;
        
        String hash = String.valueOf(url.hashCode());
        
        if(mFiles.containsKey(hash)) {
        	filename = mFiles.get(hash);
        	if(DEBUG) Log.d(TAG, "File already saved. getting file : " + filename);
        } else {
            return null;
        }
        
        File f = new File(cacheDir, filename);
        
        return f;
    }
    
    // Files are stored in the formst urlHash|unixTime
    public File getFile(String url){
        String filename;
        
        String hash = String.valueOf(url.hashCode());
        
        if(mFiles.containsKey(hash)) {
        	filename = mFiles.get(hash);
        	if(DEBUG) Log.d(TAG, "File already saved. getting file : " + filename);
        } else {
            String unixTime = String.valueOf(System.currentTimeMillis());
            filename = hash.concat("_" + unixTime);
            
            if(isCacheFull()) {
            	cleanCache(false); // Deletes 5MB @param, = is a recursive call
            }
            
            if(DEBUG) Log.d(TAG, "Creating File, Fname : " + filename);
        }
        
        File f = new File(cacheDir, filename);
        
        pQueue.add(filename);
        mFiles.put(hash, filename);
        
        return f;
        
    }
    
    // Checks if currentDirectorySize >= maxDirectorySize
    private boolean isCacheFull() {
    	return mDirSize >= maxDirectorySize;
    }
    
    // Deletes 5MB worth of data from old files
    // Consider doing it in a thread. Probably cpu intensive task
    private void cleanCache(boolean recursiveCall) {
    	if(DEBUG) Log.e(TAG, "Cache Full!. Clearning up 2MB of files");
    	
    	if(recursiveCall) setupDirectoryOverflow(); //Rebuilt tree if this is a recursive call
    	
    	int delBytes = 0;
    	
    	File f;
    	String filename;
    	int i = 0;
    	while(delBytes <= cleanupDirCache) {
    		filename = pQueue.poll();
    		
    		if(DEBUG) Log.d(TAG, "Deleting file : " + filename);
    		
    		if(filename == null)
    			break;
    		
    		f = new File(cacheDir, filename);
    		
    		if(f != null) {
    			delBytes += f.length();
    			f.delete();
    			i++;
    		}
    	}
    	
    	mDirSize -= delBytes;
    	
    	if(!recursiveCall && delBytes < cleanupDirCache) // If deletedBytes was less than 5MB, probably because the priorityQueue is not built properly
    		cleanCache(true); // Try again with rebuilding of whole priority queue
    	
    	if(DEBUG) Log.d(TAG, delBytes/1024. + " kb of files deleted | No. of files deleted : " + i);
    }
    
    // Calculates the size of the files in the directory
    private long calcDirSize() {
        long result = 0;

        Stack<File> dirlist= new Stack<File>();
        dirlist.clear();

        dirlist.push(cacheDir);

        while(!dirlist.isEmpty())
        {
            File dirCurrent = dirlist.pop();

            File[] fileList = dirCurrent.listFiles();
            for (int i = 0; i < fileList.length; i++) {

                if(fileList[i].isDirectory())
                    dirlist.push(fileList[i]);
                else
                    result += fileList[i].length();
            }
        }
        
        return result;
    }
    
    // Checks running status in a threadSafe way
    private synchronized boolean isRunning() {
    	return running;
    }
    
    // Change running in a thread safe way
    private synchronized void setRunning(boolean b) {
    	running = b;
    }
    
    
    // public but only called by ImageLoader. 
    // Updates us about the size of a new downloaded file
    public void newFileSize(long n) {
    	if(DEBUG) Log.i(TAG, "New File Size added : " + n/1024. + "KB");
    	if(!isRunning())
    		mDirSize += n;
    	else
    		tempDirSize += n;
    }
    
    /*
     * PUBLIC FUNCTIONS
     */
    public void clearCache(){
        File[] files=cacheDir.listFiles();
        if(files==null)
            return;
        for(File f:files)
            f.delete();
        
        Toast.makeText(mContext, String.format("%.2f", mDirSize/1024./1024.0f) + "MB deleted", Toast.LENGTH_SHORT).show();
        mDirSize = 0;
        commmitDirSize();
    }
    
    // Returns current calculated size of the directory in bytes
    public long getFileCacheSize() {
    	return mDirSize;
    }
    
    // Force calculate dirSize 
    // WARNING : possibly could take some time. preferably called in a thread
    public void forceUpdateDirSize() {
    	calcDirSize();
    	commmitDirSize(); // Updates mUtil with latest calculated value
    }
    
    // Calculates dirSize in a thread. 
    public void forceUpdateDirSizeThread() {
    	initDirSize();
    }
    
    // Commits current mDirSize to file preferences
    public void commmitDirSize() {
    	if(DEBUG) Log.v(TAG, "CommitDirSize : " + mDirSize/1024./1024. + "MB");
    	mUtil.addLong(KEY, mDirSize);
    }
    
    
    // PriorityQueue comperator class
    public class UnixTimeComparator implements Comparator<String>
    {
        @Override
        public int compare(String x, String y)
        {
        	long unixTimeX = Long.parseLong(x.substring(x.lastIndexOf("_") + 1), 10);
        	long unixTimeY = Long.parseLong(y.substring(y.lastIndexOf("_") + 1), 10);
            if (unixTimeX < unixTimeY)
            {
                return -1;
            }
            
            if (unixTimeX > unixTimeY)
            {
                return 1;
            }
            return 0;
        }
    }
}