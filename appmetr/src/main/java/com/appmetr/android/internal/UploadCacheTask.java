package com.appmetr.android.internal;

import android.util.Log;

import com.appmetr.android.BuildConfig;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Copyright (c) 2019 AppMetr.
 * All rights reserved.
 */
public class UploadCacheTask  {
    public enum UploadStatus {
        None,
        Pending,
        Success,
        NetworkError,
        IOError
    }

    private final static String TAG = "AppMetrUploadCacheTask";
    private final static String METHOD_TRACK = "server.track";

    private final static Lock mUploadCacheLock = new ReentrantLock();
    private ContextProxy mContextProxy;
    private WebServiceRequest mWebServiceRequest;
    private RequestParameters mRequestParameters;
    private UploadStatus mStatus = UploadStatus.None;

    public UploadCacheTask(ContextProxy contextProxy, String token, String macAddress) {
        this(contextProxy, new WebServiceRequest(contextProxy.webServiceUrl), new RequestParameters(contextProxy.getContext(), token, macAddress));
    }

    public UploadCacheTask(ContextProxy contextProxy, WebServiceRequest webServiceRequest, RequestParameters requestParameters) {
        mContextProxy = contextProxy;
        mWebServiceRequest = webServiceRequest;
        mRequestParameters = requestParameters;
    }

    public UploadStatus getStatus() {
        return mStatus;
    }

    public int upload(ArrayList<String> fileList) {
        int res = 0;
        if(fileList.size() == 0) {
            mStatus = UploadStatus.Success;
            return res;
        }
        mStatus = UploadStatus.Pending;
        // locking this thread to prevent some conflicts from several threads, like in issue #37
        mUploadCacheLock.lock();

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[uploadCache] Thread started.");
        }

        try {
            res = uploadBatches(fileList);

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[uploadCache] Thread finished.");
            }

        } catch (final Throwable t) {
            Log.e(TAG, "uploadBatches failed", t);
        } finally {
            // releasing thread lock
            mUploadCacheLock.unlock();
        }
        return res;
    }

    public boolean uploadData(byte[] data) {
        if(data == null || data.length == 0) {
            mStatus = UploadStatus.Success;
            return true;
        }
        mStatus = UploadStatus.Pending;
        mUploadCacheLock.lock();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[uploadCache] Thread started.");
        }

        try {
            List<HttpNameValuePair> parameters = mRequestParameters.getForMethod(mContextProxy.getContext(), METHOD_TRACK);
            return mWebServiceRequest.sendRequest(parameters, data);
        } catch (IOException e) {
            Log.e(TAG, "Failed to upload data to the server, IO error", e);
            return false;
        } finally {
            mUploadCacheLock.unlock();
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[uploadCache] Thread finished.");
            }
        }
    }

    /**
     * Private method that uploads list of files to server.
     *
     * @return - number of files which are uploaded.
     */

    private int uploadBatches(ArrayList<String> fileList) {
        int ret = 0;
        int count = fileList.size();
        for (int i = 0; i < count; i++) {
            String fileName = fileList.get(i);
            try {
                if (uploadBatchFile(fileName, mRequestParameters)) {
                    mContextProxy.deleteFile(fileName);
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "[uploadBatches] Server returns OK. Remove file: " + fileName);
                    }
                    ret++;
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Server error, break.");
                    }
                    mStatus = UploadStatus.NetworkError;
                    return ret;
                }
            } catch (FileNotFoundException fileError) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "[uploadBatches] File '" + fileName
                            + "' not found. Maybe it's already uploaded.");
                }
            } catch (IOException ioError) {
                Log.e(TAG, "Failed to upload data to the server, IO error", ioError);
                Log.e(TAG, "Internal error, break.");
                mStatus = UploadStatus.IOError;
                return ret;
            }
        }
        mStatus = UploadStatus.Success;
        return ret;
    }

    private boolean uploadBatchFile(String fileName, RequestParameters requestParameters) throws IOException {
        byte[] batchFileContent = mContextProxy.getFileContent(fileName);
        List<HttpNameValuePair> parameters = requestParameters.getForMethod(mContextProxy.getContext(), METHOD_TRACK);
        return mWebServiceRequest.sendRequest(parameters, batchFileContent);
    }
}
