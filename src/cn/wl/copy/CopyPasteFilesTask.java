package cn.wl.copy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import cn.wl.utils.LogUtils;

public class CopyPasteFilesTask extends FileOperationTask {
    private static final String TAG = "CopyPasteFilesTask";
    File mSrcList = null;
    String mDstFolder = null;

    public CopyPasteFilesTask(OperationEventListener operationEvent, Context context, File src,
            String destFolder) {
        super(operationEvent, context);
        mSrcList = src;
        mDstFolder = destFolder;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        synchronized (mContext.getApplicationContext()) {
            LogUtils.i(TAG, "doInBackground...");
           //final long beforeTime  = System.currentTimeMillis();
            List<File> fileList = new ArrayList<File>();
            UpdateInfo updateInfo = new UpdateInfo();
            int ret = getAllFile(mSrcList, fileList, updateInfo);
            if (ret < 0) {
                LogUtils.i(TAG, "doInBackground,ret = " + ret);
                return ret;
            }

//            if(isGreaterThan4G(updateInfo) && isFat32Disk(mDstFolder)) {
//            	LogUtils.i(TAG, "doInBackground, destination is FAT32.");
//            	return OperationEventListener.ERROR_CODE_COPY_GREATER_4G_TO_FAT32;
//            }
            
            if (!isEnoughSpace(updateInfo, mDstFolder)) {
                LogUtils.i(TAG, "doInBackground, not enough space.");
                return OperationEventListener.ERROR_CODE_NOT_ENOUGH_SPACE;
            }

            publishProgress(new ProgressInfo("", 0, TOTAL, 0, updateInfo.getTotalNumber()));

            byte[] buffer = new byte[BUFFER_SIZE];
            HashMap<String, String> pathMap = new HashMap<String, String>();
            if (!fileList.isEmpty()) {
            	LogUtils.i(TAG, "fileList.get(0).getParent() = " + fileList.get(0).getParent());
            	LogUtils.i(TAG, "mDstFolder = " + mDstFolder);
                pathMap.put(fileList.get(0).getParent(), mDstFolder);
            }
            
            
            
            for (File file : fileList) {
                File dstFile = getDstFile(pathMap, file, mDstFolder);
                if (isCancelled()) {
                    LogUtils.i(TAG, "doInBackground,user cancel.");
                    return OperationEventListener.ERROR_CODE_USER_CANCEL;
                }
                if (dstFile == null) {
                    publishProgress(new ProgressInfo(
                            OperationEventListener.ERROR_CODE_PASTE_UNSUCCESS, true));
                    continue;
                }
                if (file.isDirectory()) {
                    if (mkdir(pathMap, file, dstFile)) {
                        updateInfo.updateProgress(file.length());
                        updateInfo.updateCurrentNumber(1);
                        updateProgressWithTime(updateInfo, file);
                    }
                } else {
                	/*
                    if (FileInfo.isDrmFile(file.getName()) || !file.canRead()) {
                        publishProgress(new ProgressInfo(
                                OperationEventListener.ERROR_CODE_COPY_NO_PERMISSION, true));
                        updateInfo.updateProgress(file.length());
                        updateInfo.updateCurrentNumber(1);
                        continue;
                    }
                    */
                    updateInfo.updateCurrentNumber(1);
                    ret = copyFile(buffer, file, dstFile, updateInfo);
                    if (ret == OperationEventListener.ERROR_CODE_USER_CANCEL) {
                        return ret;
                    } else if (ret < 0) {
                        publishProgress(new ProgressInfo(ret, true));
                        updateInfo.updateProgress(file.length());
                        updateInfo.updateCurrentNumber(1);
                    } else {
                    	
                    }
                }
            }
            LogUtils.i(TAG, "doInBackground,return success.");
            return OperationEventListener.ERROR_CODE_SUCCESS;
        }
    }
}
