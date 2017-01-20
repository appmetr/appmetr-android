/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.internal;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import com.appmetr.android.AppMetrListener;
import com.appmetr.android.BuildConfig;
import com.appmetr.android.internal.command.CommandsManager;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;

import org.OpenUDID.OpenUDID_manager;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.DataFormatException;

/**
 * Base class for AppMetr
 */
public class AppMetrTrackingManager {
    private final static String TAG = "AppMetrTrackingManager";

    protected String mToken = null;
    protected final RequestParameters mRequestParameters;
    protected final String mUserID;
    protected String mGoogleAID;
    protected String mWebServiceCustomUrl;
    protected WebServiceRequest mWebServiceRequest;
    protected boolean mTrackInstallByApp = true;

    protected int mCacheInterval = 0;
    protected int mUploadInterval = 0;
    protected int mMaxFileSize = 0;

    protected ExecutorService mThreadExecutor;
    protected ContextProxy mContextProxy;
    protected final LibraryPreferences mPreferences;

    protected Timer mTimer;

    protected Lock mStartLock = new ReentrantLock();
    protected Boolean mStarted = false;
    protected Long mStartTime;

    protected final ArrayList<JSONObject> mEventList = new ArrayList<JSONObject>();
    protected final ArrayList<String> mFileList;

    protected Lock mFileWritterLock = new ReentrantLock();
    protected StringFileWriter mCurrentFileWriter;

    private final Lock mFlushCacheLock = new ReentrantLock();
    private final Lock mUploadCacheLock = new ReentrantLock();

    protected final static String METHOD_TRACK = "server.track";
    protected final static String METHOD_GET_COMMANDS = "server.getCommands";
    protected final static String METHOD_VERIFY_PAYMENT = "server.verifyPayment";

    protected CommandsManager mCommandsManager;

    /**
     * Standard constructor. Initializes library with a specified activity.
     *
     * @param context Application context
     * @param handler Handler
     */
    protected AppMetrTrackingManager(Context context, Handler handler) {
        // initialize the OpenUDID
        initOpenUDID(context);
        readManifestMeta(context);

        mContextProxy = new ContextProxy(context);

        // load preferences
        mPreferences = createLibraryPreferences(context);

        mCacheInterval = LibraryPreferences.DEFAULT_CACHE_TIME;
        mUploadInterval = LibraryPreferences.DEFAULT_UPLOAD_TIME;
        mMaxFileSize = LibraryPreferences.DEFAULT_BATCH_SIZE;

        // initialize commands
        mCommandsManager = new CommandsManager(mPreferences);
        mCommandsManager.setCommandHandler(handler);

        mFileList = mPreferences.getFileList();

        mRequestParameters = new RequestParameters(context);
        mUserID = mRequestParameters.getUserID();

        new GetGAIDTask().execute(context);
    }

    private static void initOpenUDID(Context context) {
        try {
            OpenUDID_manager.sync(context);
        } catch (final Throwable t) {
            Log.e(TAG, "Failed to init OpenUDID", t);
        }
    }

    private void readManifestMeta(Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);

            if (appInfo != null && appInfo.metaData != null) {
                if (appInfo.metaData.containsKey("appmetrUrl")) {
                    mWebServiceCustomUrl = appInfo.metaData.getString("appmetrUrl");
                }

                if (appInfo.metaData.containsKey("trackInstallByApp")) {
                    mTrackInstallByApp = appInfo.metaData.getBoolean("trackInstallByApp");
                }
            }
        } catch (final Throwable t) {
            Log.e(TAG, "Failed to read meta-data from manifest", t);
        }
    }

    private void createTimers() {
        if (mTimer == null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "#createTimers");
            }
            mTimer = new Timer();

            // starting timer for upload methods
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    uploadCache();
                }
            }, mUploadInterval, mUploadInterval);

            // starting timer for flush methods
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    flushData();
                }
            }, mCacheInterval, mCacheInterval);

            // starting remote commands timer
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    sentQueryRemoteCommandList();
                }
            }, LibraryPreferences.DEFAULT_REMOTE_COMMAND_TIME, LibraryPreferences.DEFAULT_REMOTE_COMMAND_TIME);
        }
    }

    protected LibraryPreferences createLibraryPreferences(Context context) {
        return new LibraryPreferences(context);
    }

    protected void initThreadExecutor() {
        if (mThreadExecutor == null) {
            mThreadExecutor = Executors.newSingleThreadExecutor();
        }
    }

    protected String getWebServiceUrl() {
        if (mWebServiceCustomUrl != null && mWebServiceCustomUrl.length() > 0) {
            return mWebServiceCustomUrl;
        } else {
            return LibraryPreferences.DEFAULT_SERVICE_ADDRESS;
        }
    }

    /**
     * Method for additional setup libraries data. Must be valid parameter
     * token.
     *
     * @param token - parameter which is needed for data upload.
     * @throws DataFormatException - library throws exception if token is not valid
     */
    protected void initialize(String token) throws DataFormatException, SecurityException {
        String wesServiceUrl = getWebServiceUrl();

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "WebServiceUrl: " + wesServiceUrl);
        }

        mWebServiceRequest = new WebServiceRequest(wesServiceUrl);

        if (token == null || (token.length() > LibraryPreferences.TOKEN_MAX_SIZE && token.length() == 0)) {
            throw new DataFormatException("Not valid token!");
        }

        mToken = token;

        mStartLock.lock();
        if (!mStarted) {
            initThreadExecutor();
            createTimers();
            trackAppStart();

            mStartTime = new Date().getTime();
            mStarted = true;
        }
        mStartLock.unlock();
    }

    /**
     * Method which must be called when applications enters background mode
     */
    protected void sleepLibrary() {
        try {
            if (mTimer != null) {
                mTimer.cancel();
                mTimer = null;
            }

            mStartLock.lock();

            mPreferences.setSessionDuration(mPreferences.getSessionDuration() + (new Date().getTime() - mStartTime));

            unloadLibrary();
            flushDataImpl();
            closeCurrentFileWritter();

            mStarted = false;
            mStartLock.unlock();
        } catch (final Throwable t) {
            Log.e(TAG, "sleepLibrary failed", t);
        }
    }

    /**
     * Methods which must be called when applications exits background mode
     */
    protected void restoreLibrary() {
        mStartLock.lock();
        if (mToken != null && !mStarted) {
            initThreadExecutor();
            createTimers();
            trackAppStart();

            mStartTime = new Date().getTime();
            mStarted = true;
        }
        mStartLock.unlock();
    }

    /**
     * Methods which end library running.
     */
    protected void unloadLibrary() {
        if (mThreadExecutor != null) {
            mThreadExecutor.shutdown();
            try {
                mThreadExecutor.awaitTermination(LibraryPreferences.THREAD_POOL_TERMINATION_TIMEOUT_IN_SEC,
                        TimeUnit.SECONDS);
            } catch (InterruptedException error) {
                Log.e(TAG, "awaitTermination failed", error);
            }
            mThreadExecutor.shutdownNow();
            mThreadExecutor = null;
        }
    }

    /**
     * Public method for tracking any event.
     *
     * @param event - JSONObject with data of event
     */
    public void track(JSONObject event) {
        try {
            event.put("timestamp", new Date());
            JSONObject convertedEvent = Utils.convertDate(event);
            synchronized (mEventList) {
                mEventList.add(convertedEvent);
            }
        } catch (JSONException error) {
            Log.e(TAG, "track failed", error);
        }
    }

    /**
     * Method for tracking game event as "track session" with parameters
     *
     * @param properties properties for event
     */
    protected void trackSessionImpl(JSONObject properties) {
        try {
            JSONObject action = new JSONObject().put("action", "trackSession");

            if (properties == null) {
                properties = new JSONObject();
            }

            long duration = mPreferences.getSessionDuration() / 1000;
            mPreferences.setSessionDuration(0);
            if (!mPreferences.getIsFirstTrackSessionSent()) {
                duration = -1;
            }

            properties.put("$duration", duration);

            action.put("properties", properties);
            track(action);

            if (!mPreferences.getIsFirstTrackSessionSent()) {
                flushAndUploadAllEventsAsync();
                mPreferences.setIsFirstTrackSessionSent(true);
            }
        } catch (JSONException error) {
            Log.e(TAG, "trackSession failed", error);
        }
    }

    /**
     * Method for tracking game event as "installBroadcast" without parameters
     */
    protected void trackInstallBroadcast() {
        try {
            JSONObject action = new JSONObject().put("action", "installBroadcast");
            action.put("$country", ContextProxy.COUNTRY);

            track(action);
            flushAndUploadAllEventsAsync();
        } catch (JSONException error) {
            Log.e(TAG, "trackInstallBroadcast failed", error);
        }
    }

    protected void trackAppStart() {
        if (!mTrackInstallByApp) {
            trackAppStartImpl();
        }

        if (mPreferences.getIsPullCommandsOnResume()) {
            pullRemoteCommands();
        }
    }

    protected void trackAppStartImpl() {
        if (mPreferences.isFirstTimeRunning()) {
            if (!mTrackInstallByApp) {
                // add installation event
                trackInstallBroadcast();
            }
        }
    }

    /**
     * Flushing all events to the disk and uploading them to server
     */
    protected void flushAndUploadAllEvents() {
        flushDataImpl();
        uploadCache();
    }

    /**
     * Flushing all events to the disk and uploading them to server in new thread
     */
    protected void flushAndUploadAllEventsAsync() {
        mThreadExecutor.execute(new Runnable() {
            @Override public void run() {
                flushDataImpl();
                uploadCache();
            }
        });
    }

    /**
     * Private method which creates new thread and calls method to write to data
     * file.
     * Method called from timer.
     */
    protected void flushData() {
        // locking this thread to prevent some conflicts from several threads, like in issue #37
        mFlushCacheLock.lock();

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[flushData] Thread started.");
        }
        flushDataImpl();

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[flushData] Thread finished.");
        }

        // releasing thread lock
        mFlushCacheLock.unlock();
    }

    /**
     * Method copies event stack, encodes into base64 and saves into file with
     * correct name.
     * Methods creates new thread and works with file into new thread.
     */
    protected void flushDataImpl() {
        ArrayList<JSONObject> copyEvent;
        synchronized (mEventList) {
            copyEvent = new ArrayList<JSONObject>(mEventList);
            mEventList.clear();
        }

        if (copyEvent.size() > 0) {
            mFileWritterLock.lock();
            try {
                int batchId = mPreferences.getNextBatchID();
                String encodedString = Utils.getEncodedString(copyEvent, batchId);

                if (mCurrentFileWriter != null
                        && encodedString.length() + mCurrentFileWriter.getCurrentFileSize() > mMaxFileSize) {
                    closeCurrentFileWritter();
                }

                if (mCurrentFileWriter == null) {
                    mCurrentFileWriter = new StringFileWriter(mContextProxy.getContext(),
                            mPreferences.getNextFileIndex());
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "[flushDataImpl] Batch file not exist. " + "Created a new batch file: "
                                + mCurrentFileWriter.getFileName());
                    }
                }

                mCurrentFileWriter.addChunk(encodedString);
            } catch (Exception error) {
                Log.e(TAG, "Failed to save the data to disc.", error);
            }

            mFileWritterLock.unlock();
        }
    }

    /**
     * Private method which closes current pointer to file and saves file in
     * file list for upload.
     */
    protected void closeCurrentFileWritter() {
        mFileWritterLock.lock();

        try {
            if (mCurrentFileWriter != null) {
                mCurrentFileWriter.close();
                synchronized (mFileList) {
                    mFileList.add(mCurrentFileWriter.getFileName());
                }

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "[closeCurrentFileWritter] Close batch file " + mCurrentFileWriter.getFileName());
                }
            }
        } catch (final IOException e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to close stream.", e);
            }
        }

        mCurrentFileWriter = null;
        mFileWritterLock.unlock();

        mPreferences.setFileList(mFileList);
    }

    /**
     * Private method which creates new thread for uploading files with events
     * to server.
     */
    protected void uploadCache() {
        // locking this thread to prevent some conflicts from several threads, like in issue #37
        mUploadCacheLock.lock();

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[uploadCache] Thread started.");
        }

        try {
            uploadBatches();

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[uploadCache] Thread finished.");
            }

        } catch (final Throwable t) {
            Log.e(TAG, "uploadBatches failed", t);
        } finally {
            // releasing thread lock
            mUploadCacheLock.unlock();
        }
    }

    /**
     * Private method that uploads list of files to server.
     *
     * @return - number of files which are uploaded.
     */
    protected int uploadBatches() {
        // close current batch file
        closeCurrentFileWritter();

        int res = 0;
        ArrayList<String> copyFileList;
        synchronized (mFileList) {
            copyFileList = new ArrayList<String>(mFileList);
        }

        if ((res = uploadBatchesImpl(copyFileList)) > 0) {
            mPreferences.setFileList(mFileList);
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[uploadBatches] " + res + " batches of " + copyFileList.size() + " uploaded successfully");
            }
        }

        return res;
    }

    protected int uploadBatchesImpl(ArrayList<String> fileList) {
        int ret = 0;
        int count = fileList.size();
        for (int i = 0; i < count; i++) {
            String fileName = fileList.get(i);
            try {
                if (uploadBatchFile(fileName)) {
                    removeBatchFile(fileName);
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "[uploadBatches] Server returns OK. Remove file: " + fileName);
                    }
                    ret++;
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Server error, break.");
                    }
                    break;
                }
            } catch (FileNotFoundException fileError) {
                removeBatchFile(fileName);
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "[uploadBatches] File '" + fileName
                            + "' not found. Maybe issue #37?. Skipping this file.");
                }
            } catch (IOException ioError) {
                Log.e(TAG, "Failed to upload data to the server, IO error", ioError);
                Log.e(TAG, "Internal error, break.");
                break;
            }
        }

        return ret;
    }

    private boolean uploadBatchFile(String fileName) throws IOException {
        byte[] batchFileContent = mContextProxy.getFileContent(fileName);
        List<NameValuePair> parameters = getRequestParameters(METHOD_TRACK);

        return mWebServiceRequest.sendRequest(parameters, batchFileContent);
    }

    private void removeBatchFile(String fileName) {
        mContextProxy.deleteFile(fileName);
        synchronized (mFileList) {
            mFileList.remove(fileName);
        }
    }

    /**
     * Method which generates HTTP header in a required format
     *
     * @return - HTTP header
     */
    protected List<NameValuePair> getRequestParameters(String method) {
        List<NameValuePair> ret = new ArrayList<NameValuePair>(9);
        ret.add(new BasicNameValuePair("method", method));
        ret.add(new BasicNameValuePair("token", mToken));
        ret.add(new BasicNameValuePair("userId", mUserID));
        ret.add(new BasicNameValuePair("timestamp", Long.toString(new Date().getTime())));
        ret.add(new BasicNameValuePair("mobOpenUDID", OpenUDIDProxy.getOpenUDID()));

        ret.add(new BasicNameValuePair("mobDeviceType", Build.MANUFACTURER + "," + Build.MODEL));
        ret.add(new BasicNameValuePair("mobOSVer", Build.VERSION.RELEASE));
        ret.add(new BasicNameValuePair("mobLibVer", LibraryPreferences.VERSION_STRING));
        ret.add(new BasicNameValuePair("mobAndroidID", mRequestParameters.ANDROID_ID));

        if(mGoogleAID != null) {
            ret.add(new BasicNameValuePair("mobGoogleAid", mGoogleAID));
        }

        if (mRequestParameters.GOOGLE_ACCOUNT_HASH != null) {
            ret.add(new BasicNameValuePair("psUserIdsGC", mRequestParameters.GOOGLE_ACCOUNT_HASH));
        }

        if (mRequestParameters.MAC_ADDRESS != null) {
            ret.add(new BasicNameValuePair("mobMac", mRequestParameters.MAC_ADDRESS));
        }

        if (mRequestParameters.BUILD_SERIAL != null) {
            ret.add(new BasicNameValuePair("mobBuildSerial", mRequestParameters.BUILD_SERIAL));
        }

        if (mRequestParameters.DEVICE_ID != null) {
            ret.add(new BasicNameValuePair("mobTmDevId", mRequestParameters.DEVICE_ID));
        }

        return ret;
    }

    protected void pullRemoteCommands() {
        if (mThreadExecutor == null) {
            mPreferences.setPullCommandsOnResume(true);
        } else {
            mPreferences.setPullCommandsOnResume(false);
            Runnable task = new Runnable() {
                public void run() {
                    sentQueryRemoteCommandList();
                }
            };

            mThreadExecutor.execute(task);
        }
    }

    protected void sentQueryRemoteCommandList() {
        try {
            mCommandsManager.sentQueryRemoteCommandList(getRequestParameters(METHOD_GET_COMMANDS), mWebServiceRequest);
        } catch (final Throwable t) {
            Log.e(TAG, "#sentQueryRemoteCommandList failed s", t);
        }
    }

    /**
     * Processing the remote command, received from server.
     *
     * @return The number of successful processed commands
     */
    @Deprecated
    public void processRemoteCommands() {
        mCommandsManager.processCommands();
    }

    /**
     * Sets the listener for this object
     *
     * @param lister The new listener object or null
     * @see #getListener()
     */
    public void setListener(AppMetrListener lister) {
        mCommandsManager.setListener(lister);
    }

    /**
     * @return The current AppMetr listener object or null
     * @see #setListener(com.appmetr.android.AppMetrListener)
     */
    public AppMetrListener getListener() {
        return mCommandsManager.getListener();
    }

    /**
     * @return The manager or remote commands
     */
    public CommandsManager getCommandsManager() {
        return mCommandsManager;
    }

    protected boolean verifyPaymentAndCheck(String purchaseInfo, String receipt, String privateKey) {
        try {
            String salt = Utils.md5("123567890:" + System.currentTimeMillis());

            List<NameValuePair> parameters = getRequestParameters(METHOD_VERIFY_PAYMENT);
            parameters.add((new BasicNameValuePair("purchase", purchaseInfo)));
            parameters.add((new BasicNameValuePair("receipt", receipt)));
            parameters.add((new BasicNameValuePair("salt", salt)));

            JSONObject response = mWebServiceRequest.sendRequest(parameters);

            boolean ret = response.getString("status").compareTo("valid") == 0;
            if (ret) {
                JSONObject purchase = new JSONObject(purchaseInfo);
                String purchaseToken = purchase.getString("purchaseToken");
                ret = response.getString("sig").equals(Utils.md5(purchaseToken + ":" + salt + ":" + privateKey));
            }
            return ret;
        } catch (final Throwable t) {
            Log.e(TAG, "Failed to validate payment", t);
            return false;
        }
    }

    /**
     * Sets GoogleAID
     */
    private class GetGAIDTask extends AsyncTask<Context, Integer, String> {
        @Override
        protected String doInBackground(Context... contexts)
        {
            try {
                AdvertisingIdClient.Info info = AdvertisingIdClient.getAdvertisingIdInfo(contexts[0]);

                if(info.isLimitAdTrackingEnabled()) {
                    return "";
                } else {
                    return info.getId();
                }
            } catch (final Throwable t) {
                if(BuildConfig.DEBUG) {
                    Log.e(TAG, "Failed to retrieve GOOGLE_AID", t);
                }
                return "";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            mGoogleAID = result;
        }
    }
}
