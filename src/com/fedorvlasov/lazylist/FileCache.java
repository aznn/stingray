package com.fedorvlasov.lazylist;

import java.io.File;
import java.util.Stack;

import android.content.Context;

public class FileCache {
    
    private File cacheDir;
    private long mDirSize;
    
    public FileCache(Context context){
        //Find the dir to save cached images
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            cacheDir=new File(android.os.Environment.getExternalStorageDirectory(),"Obliquity");
        else
            cacheDir=context.getCacheDir();
        if(!cacheDir.exists())
            cacheDir.mkdirs();
        
        mDirSize = dirSize();
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
    }
    
    // Calculates the size of the files in the directory
    private long dirSize() {
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
    
    // Returns current calculated size of the directory in bytes
    public long getFileCacheSize() {
    	return mDirSize;
    }
        
}