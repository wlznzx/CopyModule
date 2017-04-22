/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package cn.wl.copy;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import cn.wl.utils.FileUtils;
import cn.wl.utils.LogUtils;

abstract class FileOperationTask extends BaseAsyncTask {
    private static final String TAG = "FileOperationTask";
    //to increase the copy/paste speed
    protected static final int BUFFER_SIZE = 2048 * 1024;
    //protected static final int BUFFER_SIZE = 512 * 1024;
    protected static final int TOTAL = 100;
    protected Context mContext;

    public FileOperationTask(
            OperationEventListener operationEvent, Context context) {
        super(operationEvent);
        if (context == null) {
            LogUtils.e(TAG, "construct FileOperationTask exception! ");
            throw new IllegalArgumentException();
        } else {
            mContext = context;
        }
    }

    protected File getDstFile(HashMap<String, String> pathMap, File file, String defPath) {
        LogUtils.d(TAG, "getDstFile.");
        String curPath = pathMap.get(file.getParent());
        if (curPath == null) {
            curPath = defPath;
        }
        File dstFile = new File(curPath, file.getName());

        return checkFileNameAndRename(dstFile);
    }

    protected boolean deleteFile(File file) {
        if (file == null) {
            publishProgress(new ProgressInfo(OperationEventListener.ERROR_CODE_DELETE_UNSUCCESS,
                    true));
        } else {
            if (file.canWrite() && file.delete()) {
                return true;
            } else {
                LogUtils.d(TAG, "deleteFile fail,file name = " + file.getName());
                publishProgress(new ProgressInfo(
                        OperationEventListener.ERROR_CODE_DELETE_NO_PERMISSION, true));
            }
        }
        return false;
    }

    protected boolean mkdir(HashMap<String, String> pathMap, File srcFile, File dstFile) {
        LogUtils.d(TAG, "mkdir,srcFile = " + srcFile + ",dstFile = " + dstFile);
        if (srcFile.exists() && srcFile.canRead() && dstFile.mkdirs()) {
            pathMap.put(srcFile.getAbsolutePath(), dstFile.getAbsolutePath());
            return true;
        } else {
            publishProgress(new ProgressInfo(OperationEventListener.ERROR_CODE_PASTE_UNSUCCESS,
                    true));
            return false;
        }
    }

    private long calcNeedSpace(List<File> fileList) {
        long need = 0;
        for (File file : fileList) {
            need += file.length();
        }
        return need;
    }
    
    protected boolean isGreaterThan4G(UpdateInfo updateInfo) {
    	long size = updateInfo.getTotal();
    	if(size > (4L * 1024 * 1024 * 1024)) {
    		LogUtils.d(TAG, "isGreaterThan4G true.");
    		return true;
    	}
    	
    	LogUtils.d(TAG, "isGreaterThan4G false.");
    	return false;
    }
    
    protected boolean isEnoughSpace(UpdateInfo updateInfo, String dstFolder) {
        LogUtils.d(TAG, "isEnoughSpace,dstFolder = " + dstFolder);
        long needSpace = updateInfo.getTotal();
        File file = new File(dstFolder);
        long freeSpace = file.getFreeSpace();
        if (needSpace > freeSpace) {
            return false;
        }
        return true;
    }

    protected int getAllFile(File srcFile, List<File> fileList, UpdateInfo updateInfo) {
        if (isCancelled()) {
            LogUtils.i(TAG, "getAllFile, cancel.");
            return OperationEventListener.ERROR_CODE_USER_CANCEL;
        }
        fileList.add(srcFile);
        updateInfo.updateTotal(srcFile.length());
        updateInfo.updateTotalNumber(1);
        if (srcFile.isDirectory() && srcFile.canRead()) {
            File[] files = srcFile.listFiles();
            if (files == null) {
                return OperationEventListener.ERROR_CODE_UNSUCCESS;
            }
            for (File file : files) {
                int ret = getAllFile(file, fileList, updateInfo);
                if (ret < 0) {
                    return ret;
                }
            }
        }
        return OperationEventListener.ERROR_CODE_SUCCESS;
    }

    protected int copyFile(byte[] buffer, File srcFile, File dstFile, UpdateInfo updateInfo) {
        if ((buffer == null) || (srcFile == null) || (dstFile == null)) {
            LogUtils.i(TAG, "copyFile, invalid parameter.");
            return OperationEventListener.ERROR_CODE_PASTE_UNSUCCESS;
        }
        FileInputStream in = null;
        FileOutputStream out = null;
        int ret = OperationEventListener.ERROR_CODE_SUCCESS;
        try {
            if (!dstFile.createNewFile()) {
                LogUtils.i(TAG, "copyFile, create new file fail.");
                return OperationEventListener.ERROR_CODE_PASTE_UNSUCCESS;
            }
            if (!srcFile.exists()) {
                LogUtils.i(TAG, "copyFile, src file is not exist.");
                return OperationEventListener.ERROR_CODE_PASTE_UNSUCCESS;
            }
            in = new FileInputStream(srcFile);
            out = new FileOutputStream(dstFile);

            int len = 0;
            while ((len = in.read(buffer)) > 0) {
                // Copy data from in stream to out stream
                if (isCancelled()) {
                    LogUtils.d(TAG, "copyFile,commit copy file cancelled; " + "break while loop "
                            + "thread id: " + Thread.currentThread().getId());
                    if (!dstFile.delete()) {
                        LogUtils.w(TAG, "copyFile,delete fail in copyFile()");
                    }
                    return OperationEventListener.ERROR_CODE_USER_CANCEL;
                }
                out.write(buffer, 0, len);
               // LogUtils.i(TAG, "copyFile, copyFile,len= " + len);
                updateInfo.updateProgress(len);
                updateProgressWithTime(updateInfo, srcFile);
            }
        } catch (IOException ioException) {
            LogUtils.e(TAG, "copyFile,io exception!");
            ioException.printStackTrace();
            ret = OperationEventListener.ERROR_CODE_PASTE_UNSUCCESS;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioException) {
                LogUtils.e(TAG, "copyFile,io exception 2!");
                ioException.printStackTrace();
                ret = OperationEventListener.ERROR_CODE_PASTE_UNSUCCESS;
            } finally {
                LogUtils.d(TAG, "copyFile,update 100%.");
                publishProgress(new ProgressInfo(srcFile.getName(), TOTAL, TOTAL, (int) updateInfo
                        .getCurrentNumber(), updateInfo.getTotalNumber()));
            }
        }
        return ret;
    }

    File checkFileNameAndRename(File conflictFile) {
        File retFile = conflictFile;
        while (true) {
            if (isCancelled()) {
                LogUtils.i(TAG, "checkFileNameAndRename,cancel.");
                return null;
            }
            if (!retFile.exists()) {
                LogUtils.i(TAG, "checkFileNameAndRename,file is not exist.");
                return retFile;
            }
            retFile = FileUtils.genrateNextNewName(retFile);
            if (retFile == null) {
                LogUtils.i(TAG, "checkFileNameAndRename,retFile is null.");
                return null;
            }
        }
    }

    protected void updateProgressWithTime(UpdateInfo updateInfo, File file) {
        if (updateInfo.needUpdate()) {
            int progress = (int) (updateInfo.getProgress() * TOTAL / updateInfo.getTotal());
            LogUtils.d(TAG, "updateProgressWithTime progress = " + progress);
            publishProgress(new ProgressInfo(file.getName(), progress, TOTAL, (int) updateInfo
                    .getCurrentNumber(), updateInfo.getTotalNumber()));
        }
    }

    static class UpdateInfo {
        protected static final int NEED_UPDATE_TIME = 200;
        private long mStartOperationTime = 0;
        private long mProgressSize = 0;
        private long mTotalSize = 0;
        private long mCurrentNumber = 0;
        private long mTotalNumber = 0;

        public UpdateInfo() {
            mStartOperationTime = System.currentTimeMillis();
        }

        public long getProgress() {
            return mProgressSize;
        }

        public long getTotal() {
            return mTotalSize;
        }

        public long getCurrentNumber() {
            return mCurrentNumber;
        }

        public long getTotalNumber() {
            return mTotalNumber;
        }

        public void updateProgress(long addSize) {
            mProgressSize += addSize;
        }

        public void updateTotal(long addSize) {
            mTotalSize += addSize;
        }

        public void updateCurrentNumber(long addNumber) {
            mCurrentNumber += addNumber;
        }

        public void updateTotalNumber(long addNumber) {
            mTotalNumber += addNumber;
        }

        public boolean needUpdate() {
            long operationTime = System.currentTimeMillis() - mStartOperationTime;
            if (operationTime > NEED_UPDATE_TIME) {
                mStartOperationTime = System.currentTimeMillis();
                return true;
            }
            return false;
        }

    }
}
