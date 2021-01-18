package com.appmetr.android.internal;

import android.text.TextUtils;
import android.util.Log;

import com.appmetr.android.BuildConfig;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Copyright (c) 2019 AppMetr.
 * All rights reserved.
 */
public class UploadCacheTask {
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
    private final ContextProxy mContextProxy;
    private final WebServiceRequest mWebServiceRequest;
    private final RequestParameters mRequestParameters;
    private UploadStatus mStatus = UploadStatus.None;

    public UploadCacheTask(ContextProxy contextProxy, String token) {
        this(contextProxy, new WebServiceRequest(contextProxy.webServiceUrl), new RequestParameters(contextProxy.getContext(), token));
    }

    public UploadCacheTask(ContextProxy contextProxy, WebServiceRequest webServiceRequest, RequestParameters requestParameters) {
        mContextProxy = contextProxy;
        mWebServiceRequest = webServiceRequest;
        mRequestParameters = requestParameters;
    }

    /**
     * Get current status of upload progress
     *
     * @return - current upload status
     */
    public UploadStatus getStatus() {
        return mStatus;
    }

    /**
     * Method that uploads one file to server, than deletes it on
     * successful response
     *
     * @return - true if upload process finished successfully,
     * false otherwise
     */
    public boolean uploadFile(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            mStatus = UploadStatus.Success;
            return true;
        }
        mStatus = UploadStatus.Pending;
        // locking this thread to prevent some conflicts from several threads, like in issue #37
        mUploadCacheLock.lock();

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[uploadFile] Thread started.");
        }

        try {
            if (uploadBatchFile(fileName, mRequestParameters)) {
                mContextProxy.deleteFile(fileName);
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "[uploadFile] Server returns OK. File removed: " + fileName);
                }
                mStatus = UploadStatus.Success;
                return true;
            } else {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "[uploadFile] Server error, break.");
                }
                mStatus = UploadStatus.NetworkError;
                return false;
            }
        } catch (FileNotFoundException fileError) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[uploadFile] File '" + fileName
                        + "' not found. Maybe it's already uploaded.");
            }
            mStatus = UploadStatus.IOError;
            return false;
        } catch (IOException ioError) {
            Log.e(TAG, "[uploadFile] Failed to upload data to the server, IO error. File will be deleted", ioError);
            if(!mContextProxy.deleteFile(fileName)) {
                Log.w(TAG, "[uploadFile] Failed to delete corrupted file. Skipping");
            }
            mStatus = UploadStatus.IOError;
            return false;
        } finally {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[uploadFile] Thread finished.");
            }
            // releasing thread lock
            mUploadCacheLock.unlock();
        }
    }

    /**
     * Method for uploading binary data array
     *
     * @param data - array of bytes
     * @return - true if upload process finished successfully,
     * false otherwise
     */
    public boolean uploadData(byte[] data) {
        if (data == null || data.length == 0) {
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
            if(mWebServiceRequest.sendRequest(parameters, data)) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "[uploadData] Server returns OK.");
                }
                mStatus = UploadStatus.Success;
                return true;
            } else {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "[uploadData] Server error, break.");
                }
                mStatus = UploadStatus.NetworkError;
                return false;
            }
        } catch (IOException e) {
            mStatus = UploadStatus.NetworkError;
            Log.e(TAG, "Failed to upload data to the server, IO error", e);
            return false;
        } finally {
            mUploadCacheLock.unlock();
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[uploadCache] Thread finished.");
            }
        }
    }

    private boolean uploadBatchFile(String fileName, RequestParameters requestParameters) throws IOException {
        byte[] batchFileContent = mContextProxy.getFileContent(fileName);
        List<HttpNameValuePair> parameters = requestParameters.getForMethod(mContextProxy.getContext(), METHOD_TRACK);
        return sendWebRequest(parameters, batchFileContent);
    }

    private boolean sendWebRequest(List<HttpNameValuePair> parameters, byte[] content) {
        try {
            return mWebServiceRequest.sendRequest(parameters, content);
        } catch(IOException e) {
            Log.e(TAG, "[sendWebRequest] Failed with network error", e);
            return false;
        }
    }
}
