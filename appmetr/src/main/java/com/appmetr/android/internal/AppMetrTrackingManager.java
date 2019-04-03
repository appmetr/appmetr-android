/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.internal;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.util.Log;

import com.appmetr.android.AppmetrConstants;
import com.appmetr.android.BuildConfig;
import com.appmetr.android.UploadJobService;
import com.appmetr.android.UploadService;

import org.json.JSONException;
import org.json.JSONObject;

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
    private final static int UPLOAD_JOB_ID = 1001;

    protected RequestParameters mRequestParameters;
    protected WebServiceRequest mWebServiceRequest;

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
    protected final static String METHOD_VERIFY_PAYMENT = "server.verifyPayment";

    private InstallReferrerConnectionHandler mInstallReferrerConnectionHandler;
    private boolean mFlushEventsOnResume;
    private boolean mFlushAndUploadEventsOnResume;

    /**
     * Standard constructor. Initializes library with a specified activity.
     *
     * @param context Application context
     */
    protected AppMetrTrackingManager(Context context) {
        mContextProxy = new ContextProxy(context);

        // load preferences
        mPreferences = createLibraryPreferences(context);

        mCacheInterval = LibraryPreferences.DEFAULT_CACHE_TIME;
        mUploadInterval = LibraryPreferences.DEFAULT_UPLOAD_TIME;
        mMaxFileSize = LibraryPreferences.DEFAULT_BATCH_SIZE;

        mFileList = mPreferences.getFileList();

        if(!mPreferences.getIsFirstTrackSessionSent()) {
            mInstallReferrerConnectionHandler = new InstallReferrerConnectionHandler();
            mInstallReferrerConnectionHandler.connect(context, mPreferences);
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
        }
    }

    protected LibraryPreferences createLibraryPreferences(Context context) {
        return new LibraryPreferences(context);
    }

    protected void initThreadExecutor() {
        if (mThreadExecutor == null) {
            mThreadExecutor = Executors.newSingleThreadExecutor();
        }
        if(mFlushAndUploadEventsOnResume)
            flushAndUploadAllEventsAsync();
        else if(mFlushEventsOnResume)
            flushAllEventsAsync();
        mFlushEventsOnResume = false;
        mFlushAndUploadEventsOnResume = false;
    }

    /**
     * Method for additional setup libraries data. Must be valid parameter
     * token.
     *
     * @param token - parameter which is needed for data upload.
     * @throws DataFormatException - library throws exception if token is not valid
     */
    protected void initialize(String token) throws DataFormatException, SecurityException {
        String wesServiceUrl = mContextProxy.webServiceUrl;

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "webServiceUrl: " + wesServiceUrl);
        }

        mWebServiceRequest = new WebServiceRequest(wesServiceUrl);

        if (token == null || (token.length() > LibraryPreferences.TOKEN_MAX_SIZE && token.length() == 0)) {
            throw new DataFormatException("Not valid token!");
        }

        mRequestParameters = new RequestParameters(mContextProxy.getContext(), token);

        mStartLock.lock();
        if (!mStarted) {
            initThreadExecutor();
            createTimers();
            startSession();

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

            mPreferences.setSessionDurationCurrent(mPreferences.getSessionDurationCurrent() + (System.currentTimeMillis() - mStartTime));

            // saves sleep time for calculating pause duration in future
            mStartTime = System.currentTimeMillis();

            unloadLibrary();
            flushDataImpl();
            closeCurrentFileWritter();

            mStarted = false;
            mStartLock.unlock();
        } catch (final Throwable t) {
            Log.e(TAG, "sleepLibrary failed", t);
        }
        uploadCacheDeferred();
    }

    /**
     * Methods which must be called when applications exits background mode
     */
    protected void restoreLibrary() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler jobScheduler = (JobScheduler) mContextProxy.getContext().getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler != null) {
                jobScheduler.cancel(UPLOAD_JOB_ID);
            }
        }
        mStartLock.lock();
        if (mRequestParameters != null && !mStarted) {
            initThreadExecutor();
            createTimers();

            // If application was paused more than MAX time
            if((System.currentTimeMillis() - mStartTime) >= LibraryPreferences.SESSION_MAX_PAUSE_DURATION) {
                // start new session
                startSession();
            }
            mStartTime = System.currentTimeMillis();
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
            if (event.has("properties")) {
                JSONObject properties = event.optJSONObject("properties");
                if (properties != null && properties.has(AppmetrConstants.PROPERTY_TIMESTAMP)) {
                    Object timestamp = properties.get(AppmetrConstants.PROPERTY_TIMESTAMP);
                    if (timestamp instanceof Date || timestamp instanceof Long) {
                        event.put(AppmetrConstants.PROPERTY_TIMESTAMP, timestamp);
                        properties.remove(AppmetrConstants.PROPERTY_TIMESTAMP);
                    }
                }

            }
            if (!event.has(AppmetrConstants.PROPERTY_TIMESTAMP) || (!(event.get(AppmetrConstants.PROPERTY_TIMESTAMP) instanceof Long) && !(event.get(AppmetrConstants.PROPERTY_TIMESTAMP) instanceof Date)))
                event.put(AppmetrConstants.PROPERTY_TIMESTAMP, new Date());
            JSONObject convertedEvent = Utils.convertDate(event);
            synchronized (mEventList) {
                mEventList.add(convertedEvent);
            }
        } catch (JSONException error) {
            Log.e(TAG, "track failed", error);
        }
    }

    /**
     * Method for tracking game event as "track session" with properties
     * This method sends properties of current session with duration of
     * previous session or -1 duration, if previous session not preset
     *
     * @param properties properties for current session
     */
    protected void trackSessionImpl(JSONObject properties) {
        try {
            final JSONObject action = new JSONObject().put("action", "trackSession");
            final JSONObject finalProperties = properties != null ? properties : new JSONObject();

            long duration = mPreferences.getSessionDuration() / 1000;
            mPreferences.setSessionDuration(0);

            if(!mPreferences.getIsFirstTrackSessionSent()) { // track install
                if(mInstallReferrerConnectionHandler == null) { // must be false always
                    Log.w(TAG, "Install referrer not initialized on first launch. Creating new...");
                    mInstallReferrerConnectionHandler = new InstallReferrerConnectionHandler();
                    mInstallReferrerConnectionHandler.connect(mContextProxy.getContext(), mPreferences);
                }
                if (!mInstallReferrerConnectionHandler.onFinishConnection(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            finalProperties.put("$duration", -1); // first launch, track install
                            String installReferrer = mPreferences.getInstallReferrer();
                            if(!TextUtils.isEmpty(installReferrer))
                                finalProperties.put("install_referrer", installReferrer);
                            long referrerClickTimestamp = mPreferences.getInstallReferrerClickTimestampSeconds();
                            if(referrerClickTimestamp > 0)
                                finalProperties.put("referrer_click_timestamp_seconds", referrerClickTimestamp);
                            long  installBeginTimestamp = mPreferences.getInstallBeginTimestampSeconds();
                            if(installBeginTimestamp > 0)
                                finalProperties.put("install_begin_timestamp_seconds", installBeginTimestamp);
                            action.put("properties", finalProperties);
                            track(action);
                            flushAndUploadAllEventsAsync();
                            mPreferences.setIsFirstTrackSessionSent(true);
                        } catch (JSONException error) {
                            Log.e(TAG, "trackSession on first launch failed", error);
                        }
                    }
                })) {
                    Log.w(TAG, "Install referrer already send install event");
                }
                return;
            } else if(duration <= 0)
                return;// not first launch and session duration is empty, ignoring

            finalProperties.put("$duration", duration);
            action.put("properties", finalProperties);
            track(action);
        } catch (JSONException error) {
            Log.e(TAG, "trackSession failed", error);
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
     * Flushing all events to the disk in new thread
     */
    protected void flushAllEventsAsync() {
        if(mThreadExecutor != null && !mThreadExecutor.isShutdown()) {
            mThreadExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    flushDataImpl();
                }
            });
        } else {
            mFlushEventsOnResume = true;
        }

    }

    /**
     * Flushing all events to the disk and uploading them to server in new thread
     */
    protected void flushAndUploadAllEventsAsync() {
        if(mThreadExecutor != null && !mThreadExecutor.isShutdown()) {
            mThreadExecutor.execute(new Runnable() {
                @Override public void run() {
                    flushDataImpl();
                    uploadCache();
                }
            });
        } else {
            mFlushAndUploadEventsOnResume = true;
        }

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

        UploadCacheTask uploadCacheTask = new UploadCacheTask(mContextProxy, mWebServiceRequest, mRequestParameters);
        if ((res = uploadCacheTask.upload(copyFileList)) > 0) {
            synchronized (mFileList) {
                mFileList.removeAll(copyFileList);
            }
            mPreferences.setFileList(mFileList);
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[uploadBatches] " + res + " batches of " + copyFileList.size() + " uploaded successfully");
            }
        }

        return res;
    }

    private void uploadCacheDeferred() {
        if(mFileList.size() == 0)
            return;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            PersistableBundle extras = new PersistableBundle();
            extras.putString(UploadService.EXTRA_PARAMS_TOKEN, mRequestParameters.getToken());
            JobInfo jobInfo = new JobInfo.Builder(UPLOAD_JOB_ID, new ComponentName(mContextProxy.getContext(), UploadJobService.class))
                    .setExtras(extras)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .build();
            JobScheduler jobScheduler = (JobScheduler) mContextProxy.getContext().getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if(jobScheduler != null && jobScheduler.schedule(jobInfo) == JobScheduler.RESULT_SUCCESS) {
                if(BuildConfig.DEBUG) {
                    Log.d(TAG, "Upload batch was scheduled");
                }
            }
        } else {
            Intent intent = new Intent(mContextProxy.getContext(), UploadService.class);
            intent.setAction(UploadService.ACTION_APPMETR_UPLOAD);
            intent.putExtra(UploadService.EXTRA_PARAMS_TOKEN, mRequestParameters.getToken());
            mContextProxy.getContext().startService(intent);
        }
    }

    private void startSession() {
        if(mPreferences.getSessionDuration() > 0)
            trackSessionImpl(null);
        long currentDuration = mPreferences.getSessionDurationCurrent();
        mPreferences.setSessionDuration(currentDuration);
        mPreferences.setSessionDurationCurrent(0);
    }



    protected boolean verifyPaymentAndCheck(String purchaseInfo, String receipt, String privateKey) {
        try {
            String salt = Utils.md5("123567890:" + System.currentTimeMillis());

            List<HttpNameValuePair> parameters = mRequestParameters.getForMethod(mContextProxy.getContext(), METHOD_VERIFY_PAYMENT);
            parameters.add((new HttpNameValuePair("purchase", purchaseInfo)));
            parameters.add((new HttpNameValuePair("receipt", receipt)));
            parameters.add((new HttpNameValuePair("salt", salt)));

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
}
