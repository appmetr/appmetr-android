package com.appmetr.android.internal;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class LibraryPreferences {
    private final static String TAG = "LibraryPreferences";
    protected final static String LIBRARY_NAME = "AppMetrAndroid";

    /**
     * Time in milliseconds by default after which data is stored to disk. Also
     * known as T1.
     */
    public static final int DEFAULT_CACHE_TIME = 60000;

    /**
     * Time in milliseconds by default after which data is sent to server. Also
     * known as T2.
     */
    public static final int DEFAULT_UPLOAD_TIME = 90000;

    /**
     * Max size of a batch on default. Size of created files should not exceed
     * this value.
     */
    public static final int DEFAULT_BATCH_SIZE = 1;
    /**
     * Max size of token for application.
     */
    public static final int TOKEN_MAX_SIZE = 50;
    /** URL address of web service. */

    /**
     * Default server address which use in production mode
     */
    public static final String DEFAULT_SERVICE_ADDRESS = "https://appmetr.com/api";

    /**
     * The salt for MD5 hashes
     */
    public static final String MD5_SALT = "frADufaQaYE4rep";

    /**
     * The maximum time in seconds to wait before shutdown ThreadPoolExecutor
     */
    public static final int THREAD_POOL_TERMINATION_TIMEOUT_IN_SEC = 5; // 5 seconds

    /**
     * The version string of AppMetrAndroid library
     */
    public static final String VERSION_STRING = "1.8.0";

    /**
     * Time in milliseconds to query remote commands
     */
    public static final int DEFAULT_REMOTE_COMMAND_TIME = 70000;

    protected static final String BATCH_ID_KEY = "AppMetr-BatchID";
    protected static final String FILE_INDEX_PROP_NAME = "AppMetr-FileIndex";
    protected static final String FILE_LIST_PROP_NAME = "AppMetr-FileList";
    protected static final String INSTALL_URL_PROP_NAME = "AppMetr-InstallURLTracked";
    protected static final String FIRST_TRACK_SESSION_SENTPROP_NAME = "AppMetr-FirstTrackSessionSent";
    protected static final String PULL_COMMANDS_ON_REQUEST_PROP_NAME = "AppMetr-PullCommandsOnResume";
    protected static final String LAST_PROCESSED_COMMAND_PROP_NAME = "AppMetr-LastProcessedCommandID";
    protected static final String SESSION_DURATION_PROP_NAME = "AppMetr-SessionDuration";

    /**
     * An application shred preferences
     */
    private final SharedPreferences mPreference;

    protected Integer mCurrentBatchID;
    protected Integer mLastFileIndex;
    protected boolean mIsFirstTrackSessionSent;

    protected boolean mPullCommandsOnResume = false;
    protected String mLastProcessedCommandID;
    protected long mSessionDuration;

    public LibraryPreferences(Context context) {
        this(context.getSharedPreferences(LIBRARY_NAME, Activity.MODE_PRIVATE));
    }

    public LibraryPreferences(SharedPreferences preference) {
        mPreference = preference;
        mCurrentBatchID = Integer.valueOf(getFirstBatchID());
        mLastFileIndex = Integer.valueOf(mPreference.getInt(FILE_INDEX_PROP_NAME, 0));
        mIsFirstTrackSessionSent = getIsFirstTrackSessionSent();
        mPullCommandsOnResume = mPreference.getBoolean(PULL_COMMANDS_ON_REQUEST_PROP_NAME, false);
        mLastProcessedCommandID = mPreference.getString(LAST_PROCESSED_COMMAND_PROP_NAME, null);
        mSessionDuration = mPreference.getLong(SESSION_DURATION_PROP_NAME, 0);
    }

    /**
     * Get previous saved batchID
     *
     * @return - saved batchID
     */
    public int getFirstBatchID() {
        return mPreference.getInt(BATCH_ID_KEY, 0);
    }

    /**
     * Generate new batchID when file is full
     *
     * @return - new batchID
     */
    public int getNextBatchID() {
        int ret;
        synchronized (mCurrentBatchID) {
            ret = mCurrentBatchID++;
            SharedPreferences.Editor editor = mPreference.edit();
            editor.putInt(BATCH_ID_KEY, mCurrentBatchID.intValue());
            editor.commit();
        }
        return ret;
    }

    public int getCurrentBatchID() {
        int ret;
        synchronized (mCurrentBatchID) {
            ret = mCurrentBatchID.intValue();
        }

        return ret;
    }

    /**
     * Returns whether this application running first time or not
     *
     * @return True if the next batch will be the first batch, otherwise returns
     *         false
     */
    public boolean isFirstTimeRunning() {
        return (getCurrentBatchID() == 0);
    }

    /**
     * Internal use only
     */
    public void _resetBatchID() {
        synchronized (mCurrentBatchID) {
            mCurrentBatchID = 0;
        }
    }

    /**
     * Method which returns file's number.
     *
     * @return - number of file to be written.
     */
    public int getNextFileIndex() {
        int ret;
        synchronized (mLastFileIndex) {
            ret = ++mLastFileIndex;
            SharedPreferences.Editor editor = mPreference.edit();
            editor.putInt(FILE_INDEX_PROP_NAME, ret);
            editor.commit();
        }
        return ret;
    }

    public int getCurrentFileIndex() {
        return mLastFileIndex.intValue();
    }

    /**
     * Retrieves a list of files which are not uploaded.
     *
     * @return - list of files.
     */
    public ArrayList<String> getFileList() {
        ArrayList<String> ret;
        try {
            ret = getStringArrayList(FILE_LIST_PROP_NAME);
        } catch (JSONException error) {
            Log.d(TAG, "Failed to read the file list from SharedPreferenses.", error);
            ret = new ArrayList<String>();
        }

        return ret;
    }

    /**
     * Saves list with not uploaded files into SharedPreferences on device.
     */
    public void setFileList(ArrayList<String> fileList) {
        putArrayList(FILE_LIST_PROP_NAME, fileList);
    }

    private <T> void putArrayList(String key, ArrayList<T> list) {
        synchronized (list) {
            SharedPreferences.Editor editor = mPreference.edit();
            String data = new JSONArray(list).toString();

            editor.putString(key, Base64.encodeToString(data.getBytes(), Base64.NO_WRAP));
            editor.commit();
        }
    }

    private ArrayList<String> getStringArrayList(String key) throws JSONException {
        ArrayList<String> ret = new ArrayList<String>();
        String loadedData = mPreference.getString(key, "");
        if (loadedData.length() > 0) {
            String decodedData = new String(Base64.decode(loadedData, Base64.NO_WRAP));
            JSONArray list = new JSONArray(decodedData);

            int numItems = list.length();
            for (int i = 0; i < numItems; i++) {
                ret.add(list.getString(i));
            }
        }

        return ret;
    }

    /**
     * Returns whether install URL is tracked or not
     *
     * @return True of install URL is already tracked, otherwise returns false
     */
    public boolean getIsInstallURLTracked() {
        return mPreference.getBoolean(INSTALL_URL_PROP_NAME, false);
    }

    /**
     * Sets whether install URL is tracked or not
     */
    public void setIsInstallURLTracked(boolean tracked) {
        SharedPreferences.Editor editor = mPreference.edit();
        editor.putBoolean(INSTALL_URL_PROP_NAME, tracked);
        editor.commit();
    }

    /**
     * Returns whether first trackSession already sent, otherwise returns false
     */
    public boolean getIsFirstTrackSessionSent() {
        return mPreference.getBoolean(FIRST_TRACK_SESSION_SENTPROP_NAME, false);
    }

    /**
     * Sets whether first trackSession already sent or not
     */
    public void setIsFirstTrackSessionSent(boolean sent) {
        mIsFirstTrackSessionSent = sent;
        SharedPreferences.Editor editor = mPreference.edit();
        editor.putBoolean(FIRST_TRACK_SESSION_SENTPROP_NAME, mIsFirstTrackSessionSent);
        editor.commit();
    }

    /**
     * Returns whether first trackSession already sent, otherwise returns false
     */
    public boolean getIsPullCommandsOnResume() {
        return mPullCommandsOnResume;
    }

    /**
     * Sets whether first trackSession already sent or not
     */
    public void setPullCommandsOnResume(boolean value) {
        if (mPullCommandsOnResume != value) {
            mPullCommandsOnResume = value;
            SharedPreferences.Editor editor = mPreference.edit();
            editor.putBoolean(PULL_COMMANDS_ON_REQUEST_PROP_NAME, mPullCommandsOnResume);
            editor.commit();

        }
    }

    /**
     * @return An unique identifier of last processed command
     */
    public String getLastProcessedCommandID() {
        return mLastProcessedCommandID;
    }

    /**
     * Sets an unique identifier of last processed command
     *
     * @param value An unique identifier of last processed command
     */
    public void setLastProcessedCommandID(String value) {
        if (mLastProcessedCommandID == null || !mLastProcessedCommandID.equals(value)) {
            mLastProcessedCommandID = value;
            SharedPreferences.Editor editor = mPreference.edit();
            editor.putString(LAST_PROCESSED_COMMAND_PROP_NAME, mLastProcessedCommandID);
            editor.commit();
        }
    }

    /**
     * @return session duration time
     */
    public long getSessionDuration() { return mSessionDuration; }

    /**
     * Set session duration
     *
     * @param value - time in seconds
     */
    public void setSessionDuration(long value) {
        mSessionDuration = value;
        SharedPreferences.Editor editor = mPreference.edit();
        editor.putLong(SESSION_DURATION_PROP_NAME, mSessionDuration);
        editor.commit();
    }
}
