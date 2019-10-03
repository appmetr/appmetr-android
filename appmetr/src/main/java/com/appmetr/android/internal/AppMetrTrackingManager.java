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

import androidx.annotation.WorkerThread;

import com.appmetr.android.AppMetr;
import com.appmetr.android.AppmetrConstants;
import com.appmetr.android.BuildConfig;
import com.appmetr.android.UploadJobService;
import com.appmetr.android.UploadService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
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
    private final static int UPLOAD_IN_MEMORY_COUNT = 30;

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
    protected final ArrayList<String> mUploadList = new ArrayList<String>();

    protected Lock mFileWritterLock = new ReentrantLock();
    protected StringFileWriter mCurrentFileWriter;

    private final Lock mFlushCacheLock = new ReentrantLock();
    private final Lock mUploadCacheLock = new ReentrantLock();

    protected final static String METHOD_TRACK = "server.track";
    protected final static String METHOD_VERIFY_PAYMENT = "server.verifyPayment";

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
        mUploadInterval = Math.max(mCacheInterval, LibraryPreferences.DEFAULT_UPLOAD_TIME);
        mMaxFileSize = LibraryPreferences.DEFAULT_BATCH_SIZE;

        mFileList = mPreferences.getFileList();

        if(!mPreferences.getIsInstallReferrerTrackSent()) {
            trackInstallReferrer();
        }
    }

    private void createTimers() {
        if (mTimer == null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "#createTimers");
            }
            mTimer = new Timer();

            // starting timer for flush methods
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    flushData();
                }
            }, mCacheInterval, mCacheInterval);

            // starting timer for upload methods
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    uploadCache();
                }
            }, mUploadInterval, mUploadInterval);
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
            closeCurrentFileWriter();

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
            //if user sets "timestamp" property we set "userTime" property of the event object
            if (event.has("properties")) {
                JSONObject properties = event.optJSONObject("properties");
                if (properties != null && properties.has(AppmetrConstants.PROPERTY_TIMESTAMP)) {
                    Object timestamp = properties.remove(AppmetrConstants.PROPERTY_TIMESTAMP);
                    if (timestamp instanceof Date || timestamp instanceof Long) {
                        event.put(AppmetrConstants.PROPERTY_USER_TIME, timestamp);
                    }
                }
            }

            event.put(AppmetrConstants.PROPERTY_TIMESTAMP, System.currentTimeMillis());
            JSONObject convertedEvent = Utils.convertDateToLong(event);
            synchronized (mEventList) {
                mEventList.add(convertedEvent);
            }
        } catch (JSONException error) {
            Log.e(TAG, "track failed", error);
        }
    }

    /**
     * Public method for requesting device identity
     * It may executed long time, call it only from background thread!
     * @return a set of device ids, encoded in a query string
     */
    @WorkerThread
    public String getDeviceKey() {
        if(mRequestParameters == null)
            throw new IllegalStateException("Call initialize() first");
        return mRequestParameters.getDeviceKey(mContextProxy.getContext());
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
            if(properties == null) {
                properties = new JSONObject();
            }

            long duration = mPreferences.getSessionDuration() / 1000;
            mPreferences.setSessionDuration(0);

            if(!mPreferences.getIsFirstTrackSessionSent()) { // track install
                properties.put("$duration", -1);
                action.put("properties", properties);
                track(action);
                flushAndUploadAllEventsAsync();
                mPreferences.setIsFirstTrackSessionSent(true);
                return;
            } else if(duration <= 0)
                return;// not first launch and session duration is empty, ignoring

            properties.put("$duration", duration);
            action.put("properties", properties);
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
            String encodedString = null;
            int batchId = 0;
            try {
                batchId = mPreferences.getNextBatchID();
                encodedString = Utils.getEncodedString(copyEvent, batchId);

                if (mCurrentFileWriter != null
                        && encodedString.length() + mCurrentFileWriter.getCurrentFileSize() > mMaxFileSize) {
                    closeCurrentFileWriter();
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
                // without closing file writer we have no guarantee that we save this data in future
                closeCurrentFileWriter();
            } catch (Exception error) {
                Log.e(TAG, "Failed to save the data to disc.", error);
                if(!TextUtils.isEmpty(encodedString)) {
                    synchronized (mUploadList) {
                        if(mUploadList.size() < UPLOAD_IN_MEMORY_COUNT)
                            mUploadList.add(encodedString);
                        else
                            Log.e(TAG, "Skip uploading batch " + batchId + " due to in-memory size limit");
                    }
                }
                try {
                    trackErrorEvent(error);
                } catch (JSONException e) {
                    Log.e(TAG, "Json parsing error", e);
                }
            } finally {
                mFileWritterLock.unlock();
            }
        }
    }

    /**
     * Private method which closes current pointer to file and saves file in
     * file list for upload.
     */
    protected void closeCurrentFileWriter() throws IOException {
        mFileWritterLock.lock();

        try {
            if (mCurrentFileWriter != null) {
                mCurrentFileWriter.close();
                synchronized (mFileList) {
                    mFileList.add(mCurrentFileWriter.getFileName());
                }

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "[closeCurrentFileWriter] Close batch file " + mCurrentFileWriter.getFileName());
                }
            }
        } catch (final IOException e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to close stream.", e);
            }
            throw e;
        } finally {
            mCurrentFileWriter = null;
            mFileWritterLock.unlock();
            mPreferences.setFileList(mFileList);
        }
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
            // upload files with flushed data
            uploadBatches();
            // upload in-memory data
            uploadData();

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
        try {
            closeCurrentFileWriter();
        } catch (IOException e) {
            Log.e(TAG, "[uploadBatches] failed to close current batch file");
        }

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


    protected void uploadData() {
        ArrayList<String> uploadList;
        synchronized (mUploadList) {
            if(mUploadList.size() == 0)
                return;
            uploadList = new ArrayList<String>(mUploadList);
        }

        try {
            ByteArrayOutputStream uploadDataStream = null;
            int index = 0;
            while(index < uploadList.size()) {
                if(uploadDataStream == null) {
                    uploadDataStream = new ByteArrayOutputStream();
                    uploadDataStream.write("[".getBytes());
                }
                String uploadElem = uploadList.get(index++);
                if(TextUtils.isEmpty(uploadElem))
                    continue;
                if(uploadDataStream.size() > 1)
                    uploadDataStream.write(",".getBytes());
                uploadDataStream.write(uploadElem.getBytes());
                if(index == uploadList.size() || uploadDataStream.size() > mMaxFileSize) {
                    uploadDataStream.write("]".getBytes());
                    uploadDataStream.close();
                    UploadCacheTask uploadCacheTask = new UploadCacheTask(mContextProxy, mWebServiceRequest, mRequestParameters);
                    if(uploadCacheTask.uploadData(Utils.compressData(uploadDataStream.toByteArray()))) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "[uploadData] Direct events uploaded successfully");
                        }
                        List<String> successfullyUploaded = uploadList.subList(0, index);
                        synchronized (mUploadList) {
                            mUploadList.removeAll(successfullyUploaded);
                        }
                        successfullyUploaded.clear();
                        index = 0;
                    } else {
                        Log.e(TAG, "Failed to upload events directly. Will be retry later");
                        return;
                    }
                    uploadDataStream = null;
                }
            }
        } catch(IOException e) {
            Log.e(TAG, "[uploadData] error writing to memory stream", e);
        }
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

    private void trackInstallReferrer() {
        InstallReferrerConnectionHandler installReferrerConnectionHandler = new InstallReferrerConnectionHandler();
        installReferrerConnectionHandler.connect(mContextProxy.getContext(), mPreferences);
        if (!installReferrerConnectionHandler.onFinishConnection(new Runnable() {

            @Override
            public void run() {
                try {
                    JSONObject referrerProperties = new JSONObject();
                    String installReferrer = mPreferences.getInstallReferrer();
                    if(!TextUtils.isEmpty(installReferrer))
                        referrerProperties.put("referrer", installReferrer);
                    long referrerClickTimestamp = mPreferences.getInstallReferrerClickTimestampSeconds();
                    if(referrerClickTimestamp > 0)
                        referrerProperties.put("referrer_click_timestamp_seconds", referrerClickTimestamp);
                    long  installBeginTimestamp = mPreferences.getInstallBeginTimestampSeconds();
                    if(installBeginTimestamp > 0)
                        referrerProperties.put("install_begin_timestamp_seconds", installBeginTimestamp);

                    track(new JSONObject()
                            .put("action", "trackEvent")
                            .put("event", "install_referrer")
                            .put("properties", referrerProperties));
                } catch (JSONException error) {
                    Log.e(TAG, "track referrer event failed", error);
                }
            }
        })) {
            Log.w(TAG, "Install referrer already send install event");
        }
    }

    private void trackErrorEvent(Exception error) throws JSONException {
        String message = error.getClass().getName() + ": " + error.getMessage();
        AppMetr.trackEvent("appmetr_error", new JSONObject().put("message", message));
        int nextId = mPreferences.getNextBatchID();
        String encodedString;
        synchronized (mEventList) {
            encodedString = Utils.getEncodedString(mEventList, nextId);
            mEventList.clear();
        }
        if(!TextUtils.isEmpty(encodedString)) {
            synchronized (mUploadList) {
                if(mUploadList.size() < UPLOAD_IN_MEMORY_COUNT)
                   mUploadList.add(encodedString);
                else
                    Log.e(TAG, "Skip uploading batch " + nextId + " due to in-memory size limit");

            }
        }
    }
}
