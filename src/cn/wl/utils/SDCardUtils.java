package cn.wl.utils;

import java.io.File;


public class SDCardUtils {
	
	public static boolean isExistSDCard() {  
		if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) 
			return true;
		else     
			return false;   
	}
	
	public static boolean checkFsWritable(String dir) {  
        if (dir == null)  
            return false;  
        File directory = new File(dir);  
        if (!directory.isDirectory()) {  
            if (!directory.mkdirs()) {  
                return false;  
            }  
        }  
        File f = new File(directory, ".keysharetestgzc");  
        try {  
            if (f.exists()) {  
                f.delete();  
            }  
            if (!f.createNewFile()) {  
                return false;  
            }  
            f.delete();  
            return true;  
        } catch (Exception e) {  
        }  
        return false;  
    }  
}
