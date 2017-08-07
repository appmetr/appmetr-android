/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.internal;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.appmetr.android.BuildConfig;

import java.lang.reflect.Field;
import java.util.Locale;

/**
 *
 */
public class RequestParameters {
    private static final String TAG = "RequestParameters";

    /**
     * This device ID appearing in ALL Droid 2 devices, and other Froyo builds
     */
    private static final String MAGIC_ANDROID_ID = "9774d56d682e549c";

    public final String MAC_ADDRESS;
    public final String DEVICE_ID;
    public final String BUILD_SERIAL;
    public final String ANDROID_ID;

    /**
     * Retrieve request parameters from context
     */
    public RequestParameters(Context context) {
        MAC_ADDRESS = getMacAddress(context);
        DEVICE_ID = getDeviceID(context);
        BUILD_SERIAL = getBuildSerial();
        ANDROID_ID = getAndroidID(context);
    }

    private static String getMacAddress(Context context) {
        String ret = null;
        try {
            WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiMgr != null) {
                WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
                if (wifiInfo != null) {
                    ret = wifiInfo.getMacAddress();
                    if (ret != null) {
                        ret = ret.replace(":", "").toLowerCase(Locale.getDefault());
                    }
                }
            }
        } catch (final Throwable t) {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "Failed to retrieve wifi MAC address.", t);
            }
        }
        return ret;
    }

    private static String getDeviceID(Context context) {
        String ret = null;
        try {
            TelephonyManager telephonyMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyMgr != null) {
                ret = telephonyMgr.getDeviceId();

            }
        } catch (final Throwable t) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to retrieve device ID.", t);
            }
        }
        return ret;
    }

    private static String getBuildSerial() {
        try {
            Field serialField = Build.class.getDeclaredField("SERIAL");
            String serial = (String) serialField.get(null);
            if (serial != null && !serial.equalsIgnoreCase("unknown")) {
                return serial;
            }
        } catch (final Throwable t) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to retrieve Build.SERIAL", t);
            }
        }
        return null;
    }

    /**
     * Methods return android ID;
     *
     * @return - android id
     */
    private static String getAndroidID(Context context) {
        String ret = null;
        try {
            ret = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (final Throwable t) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to retrieve ANDROID_ID", t);
            }
        }

        return (ret == null ? "" : ret);
    }

    /**
     * Retrieves the current User ID
     *
     * @return The ID of current user or "null"
     */
    public String getUserID() {
        // User ID is very important. DO NOT CHANGE IT!!!
        String ret = ANDROID_ID;
        if (ret == null || ret.length() == 0 || ret.equals(MAGIC_ANDROID_ID)) {
            if (DEVICE_ID != null) {
                ret = DEVICE_ID;
            } else if (MAC_ADDRESS != null) {
                ret = MAC_ADDRESS;
            }
        }

        return ret;
    }
}
