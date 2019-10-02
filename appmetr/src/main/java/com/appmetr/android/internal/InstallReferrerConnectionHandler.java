package com.appmetr.android.internal;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;

/**
 * Communications between Appmetr and Google install referrer service
 */

public class InstallReferrerConnectionHandler implements InstallReferrerStateListener {
    private final static String TAG = "InstallReferrerConnect";
    private InstallReferrerClient mReferrerClient;
    private LibraryPreferences mPreferences;
    private volatile Runnable onFinishAction;

    private static boolean isConnected;
    private static final Object mMutex = new Object();

    void connect(Context context, LibraryPreferences libraryPreferences) {
        synchronized (mMutex) {
            if (isConnected)
                return;
            onFinishAction = null;
            isConnected = true;
        }

        mPreferences = libraryPreferences;
        mReferrerClient = InstallReferrerClient.newBuilder(context).build();
        mReferrerClient.startConnection(this);
    }

    @Override
    public void onInstallReferrerSetupFinished(int responseCode) {
        if (mPreferences == null || mReferrerClient == null) {
            Log.e(TAG, "Connection not setup correctly");
        } else {
            switch (responseCode) {
                case InstallReferrerResponse.OK:
                    try {
                        Log.v(TAG, "InstallReferrer connected");
                        ReferrerDetails response = mReferrerClient.getInstallReferrer();
                        mPreferences.setInstallReferrer(response.getInstallReferrer());
                        mPreferences.setInstallReferrerClickTimestampSeconds(response.getReferrerClickTimestampSeconds());
                        mPreferences.setInstallBeginTimestampSeconds(response.getInstallBeginTimestampSeconds());
                        mPreferences.setIsInstallReferrerTrackSent(true);
                        mReferrerClient.endConnection();
                    } catch (RemoteException e) {
                        Log.e(TAG, "Failed to get install referrer data", e);
                    }
                    break;
                case InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                    Log.w(TAG, "InstallReferrer not supported");
                    break;
                case InstallReferrerResponse.SERVICE_UNAVAILABLE:
                    Log.w(TAG, "Unable to connect to the service");
                    break;
                default:
                    Log.w(TAG, "Response code not recognized");
            }
        }

        mReferrerClient = null;
        mPreferences = null;
        synchronized (mMutex) {
            isConnected = false;
        }

        if (onFinishAction != null) {
            onFinishAction.run();
            onFinishAction = null;
        }
    }

    @Override
    public void onInstallReferrerServiceDisconnected() {
        mReferrerClient = null;
        mPreferences = null;
        synchronized (mMutex) {
            if (!isConnected)
                return;
            isConnected = false;
        }

        if (onFinishAction != null) {
            onFinishAction.run();
            onFinishAction = null;
        }
    }

    boolean onFinishConnection(Runnable callback) {
        synchronized (mMutex) {
            if (isConnected) {
                if (onFinishAction != null) // only first setup is valid
                    return false;
                onFinishAction = callback;
                return true;
            }
        }
        callback.run();
        return true;
    }
}
