package com.fedorvlasov.lazylist;

import java.io.File;
import java.util.Stack;

import android.content.Context;
import android.util.Log;

import com.xdev.obliquity.Config;
import com.xdev.obliquity.Util;

public class FileCache {
    
	private static final String TAG = Config.TAG_IMGLOAD_FILECH;
    private static final boolean DEBUG = Config.DEBUG_IMGLOAD_FILECH;
	private static final String KEY = Config.PREF_FCACHE_SIZE; //FileCache size preference key
    
    private File cacheDir;
    private Util mUtil;
    
    private long mDirSize; // Current DirectorySize
    private boolean running; // If Calculating DirSize in a thread. If true mDirSize must not be accessed
    private int tempDirSize; // If any updateDirSize() calls made during 'running' add it here. 
    
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
        
        if((mDirSize = mUtil.getLong(KEY, -99)) == -99) 
        	initDirSize(); // If Util has no dirSize calculated
        
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
    
    public File getFile(String url){
        //I identify images by hashcode. Not a perfect solution, good for the demo.
        String filename=String.valueOf(url.hashCode());
        //Another possible solution (thanks to grantland)
        //String filename = URLEncoder.encode(url);
        File f = new File(cacheDir, filename);
        return f;
        
    }
    
    public void clearCache(){
        File[] files=cacheDir.listFiles();
        if(files==null)
            return;
        for(File f:files)
            f.delete();
        
        mDirSize = 0;
        commmitDirSize();
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
}