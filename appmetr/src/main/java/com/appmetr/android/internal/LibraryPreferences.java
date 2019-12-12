package com.appmetr.android.internal;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.appmetr.android.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class LibraryPreferences {
    private final static String TAG = "LibraryPreferences";
    private final static String LIBRARY_NAME = "AppMetrAndroid";

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

    /**
     * The maximum time in seconds to wait before shutdown ThreadPoolExecutor
     */
    public static final int THREAD_POOL_TERMINATION_TIMEOUT_IN_SEC = 5; // 5 seconds

    /**
     * The version string of AppMetrAndroid library
     */
    public static final String VERSION_STRING = BuildConfig.VERSION_NAME;

    /**
     * Time in milliseconds for max pause state, after which starts new session
     */
    public static final int SESSION_MAX_PAUSE_DURATION = 10 * 60 * 1000;

    private static final String BATCH_ID_KEY = "AppMetr-BatchID";
    private static final String FILE_INDEX_PROP_NAME = "AppMetr-FileIndex";
    private static final String FILE_LIST_PROP_NAME = "AppMetr-FileList";
    private static final String FIRST_TRACK_SESSION_SENTPROP_NAME = "AppMetr-FirstTrackSessionSent";
    private static final String SESSION_DURATION_PROP_NAME = "AppMetr-SessionDuration";
    private static final String SESSION_DURATION_CURRENT_PROP_NAME = "AppMetr-SessionDurationCurrent";
    private static final String INSTALL_REFERRER_PROP_NAME = "AppMetr-InstallReferrer";
    private static final String INSTALL_REFERRER_CLICK_TIMESTAMP_SECONDS_PROP_NAME = "AppMetr-InstallReferrerClickTimestampSeconds";
    private static final String INSTALL_BEGIN_TIMESTAMP_SECONDS_PROP_NAME = "AppMetr-InstallBeginTimestampSeconds";
    private static final String INSTALL_REFERRER_TRACK_NAME = "AppMetr-InstallReferrerSent";
    private static final String USER_IDENTITY = "AppMetr-UserIdentity";

    /**
     * An application shred preferences
     */
    private final SharedPreferences mPreference;

    private Integer mCurrentBatchID;
    private final Object mCurrentBatchIDMutex = new Object();
    private Integer mLastFileIndex;
    private final Object mLastFileIndexMutex = new Object();
    private boolean mIsFirstTrackSessionSent;
    private String mUserIdentity;
    private long mSessionDuration;
    private long mSessionDurationCurrent;

    public LibraryPreferences(Context context) {
        this(context.getSharedPreferences(LIBRARY_NAME, Activity.MODE_PRIVATE));
    }

    public LibraryPreferences(SharedPreferences preference) {
        mPreference = preference;
        mCurrentBatchID = Integer.valueOf(getFirstBatchID());
        mLastFileIndex = Integer.valueOf(mPreference.getInt(FILE_INDEX_PROP_NAME, 0));
        mIsFirstTrackSessionSent = getIsFirstTrackSessionSent();
        mUserIdentity = mPreference.getString(USER_IDENTITY, null);
        mSessionDuration = mPreference.getLong(SESSION_DURATION_PROP_NAME, 0);
        mSessionDurationCurrent = mPreference.getLong(SESSION_DURATION_CURRENT_PROP_NAME, 0);
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
        synchronized (mCurrentBatchIDMutex) {
            ret = mCurrentBatchID++;
            SharedPreferences.Editor editor = mPreference.edit();
            editor.putInt(BATCH_ID_KEY, mCurrentBatchID.intValue());
            editor.apply();
        }
        return ret;
    }

    public int getCurrentBatchID() {
        int ret;
        synchronized (mCurrentBatchIDMutex) {
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
        synchronized (mCurrentBatchIDMutex) {
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
        synchronized (mLastFileIndexMutex) {
            ret = ++mLastFileIndex;
            SharedPreferences.Editor editor = mPreference.edit();
            editor.putInt(FILE_INDEX_PROP_NAME, ret);
            editor.apply();
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
            editor.apply();
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
        editor.apply();
    }

    public String getUserIdentity() {
        return mUserIdentity;
    }

    public void setUserIdentity(String userIdentity) {
        if(mUserIdentity != null && mUserIdentity.equals(userIdentity)) return;
        mUserIdentity = userIdentity;
        SharedPreferences.Editor editor = mPreference.edit();
        if(!TextUtils.isEmpty(mUserIdentity)) {
            editor.putString(USER_IDENTITY, mUserIdentity);
        } else {
            editor.remove(USER_IDENTITY);
            mUserIdentity = null;
        }
        editor.apply();
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
        editor.apply();
    }

    /**
     * Current session duration time
     *
     * @return current session duration time
     */
    public long getSessionDurationCurrent() { return mSessionDurationCurrent; }

    /**
     * Set current session duration
     *
     * @param value - time in seconds
     */
    public void setSessionDurationCurrent(long value) {
        mSessionDurationCurrent = value;
        SharedPreferences.Editor editor = mPreference.edit();
        editor.putLong(SESSION_DURATION_CURRENT_PROP_NAME, mSessionDurationCurrent);
        editor.apply();
    }

    public String getInstallReferrer() {
        return mPreference.getString(INSTALL_REFERRER_PROP_NAME, "");
    }

    public void setInstallReferrer(String installReferrer) {
        SharedPreferences.Editor editor = mPreference.edit();
        editor.putString(INSTALL_REFERRER_PROP_NAME, installReferrer);
        editor.apply();
    }

    public long getInstallReferrerClickTimestampSeconds() {
        return mPreference.getLong(INSTALL_REFERRER_CLICK_TIMESTAMP_SECONDS_PROP_NAME, 0);
    }

    public void setInstallReferrerClickTimestampSeconds(long timestampSeconds) {
        SharedPreferences.Editor editor = mPreference.edit();
        editor.putLong(INSTALL_REFERRER_CLICK_TIMESTAMP_SECONDS_PROP_NAME, timestampSeconds);
        editor.apply();
    }

    public long getInstallBeginTimestampSeconds() {
        return mPreference.getLong(INSTALL_BEGIN_TIMESTAMP_SECONDS_PROP_NAME, 0);
    }

    public void setInstallBeginTimestampSeconds(long timestampSeconds) {
        SharedPreferences.Editor editor = mPreference.edit();
        editor.putLong(INSTALL_BEGIN_TIMESTAMP_SECONDS_PROP_NAME, timestampSeconds);
        editor.apply();
    }

    /**
     * Returns whether install referrer already sent, otherwise returns false
     */
    public boolean getIsInstallReferrerTrackSent() {
        return mPreference.getBoolean(INSTALL_REFERRER_TRACK_NAME, false);
    }

    /**
     * Sets whether install referrer already sent or not
     */
    public void setIsInstallReferrerTrackSent(boolean sent) {
        SharedPreferences.Editor editor = mPreference.edit();
        editor.putBoolean(INSTALL_REFERRER_TRACK_NAME, sent);
        editor.apply();
    }
}
