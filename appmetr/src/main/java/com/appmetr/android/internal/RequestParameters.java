/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.internal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.appmetr.android.BuildConfig;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    private final String MAC_ADDRESS;
    private final String DEVICE_ID;
    private final String BUILD_SERIAL;
    private final String ANDROID_ID;
    private String GOOGLE_AID;
    private String FIRE_OS_ID;
    private final String USER_ID;
    private final String TOKEN;

    /**
     * Retrieve request parameters from context
     */
    public RequestParameters(Context context, String token) {
        TOKEN = token;
        MAC_ADDRESS = getMacAddress(context);
        DEVICE_ID = getDeviceID(context);
        BUILD_SERIAL = getBuildSerial();
        ANDROID_ID = getAndroidID(context);
        USER_ID = getUserID();
        GOOGLE_AID = null; // lazy init
        FIRE_OS_ID = null; // lazy init
    }

    public String getToken() {
        return TOKEN;
    }
    public String getUID() {
        return USER_ID;
    }


    /**
     * Method which generates HTTP header in a required format
     * Call it only in background thread
     *
     * @return - HTTP header
     */
    protected List<HttpNameValuePair> getForMethod(Context context, String method) {
        List<HttpNameValuePair> ret = new ArrayList<HttpNameValuePair>();
        ret.add(new HttpNameValuePair("method", method));
        ret.add(new HttpNameValuePair("token", TOKEN));
        ret.add(new HttpNameValuePair("userId", USER_ID));
        ret.add(new HttpNameValuePair("timestamp", Long.toString(new Date().getTime())));

        ret.add(new HttpNameValuePair("mobDeviceType", Build.MANUFACTURER + "," + Build.MODEL));
        ret.add(new HttpNameValuePair("mobOSVer", Build.VERSION.RELEASE));
        ret.add(new HttpNameValuePair("mobLibVer", LibraryPreferences.VERSION_STRING));
        ret.add(new HttpNameValuePair("mobAndroidID", ANDROID_ID));

        /* Lazy Google AID requesting */
        if(GOOGLE_AID == null) {
            GOOGLE_AID = getGoogleId(context);
        }
        if(!TextUtils.isEmpty(GOOGLE_AID)) {
            ret.add(new HttpNameValuePair("mobGoogleAid", GOOGLE_AID));
        } else {
            // may be it's Amazon?
            if(FIRE_OS_ID == null) {
                FIRE_OS_ID = getFireOsId(context);
            }
            if(!TextUtils.isEmpty(FIRE_OS_ID)) {
                ret.add(new HttpNameValuePair("mobFireOsAid", FIRE_OS_ID));
            }
        }

        if (MAC_ADDRESS != null) {
            ret.add(new HttpNameValuePair("mobMac", MAC_ADDRESS));
        }

        if (BUILD_SERIAL != null) {
            ret.add(new HttpNameValuePair("mobBuildSerial", BUILD_SERIAL));
        }

        if (DEVICE_ID != null) {
            ret.add(new HttpNameValuePair("mobTmDevId", DEVICE_ID));
        }

        return ret;
    }

    private static String getMacAddress(Context context) {
        String ret = null;
        try {
            WifiManager wifiMgr = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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
                Log.w(TAG, "Failed to retrieve wifi MAC address: " + t.getMessage());
            }
        }
        return ret;
    }

    @SuppressLint("MissingPermission")
    private static String getDeviceID(Context context) {
        String ret = null;
        try {
            TelephonyManager telephonyMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyMgr != null) {
                ret = telephonyMgr.getDeviceId();

            }
        } catch (final Throwable t) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Failed to retrieve device ID: " + t.getMessage());
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
                Log.e(TAG, "Failed to retrieve ANDROID_ID: " + t.getMessage());
            }
        }

        return (ret == null ? "" : ret);
    }

    /**
     * Retrieves the current User ID
     *
     * @return The ID of current user or "null"
     */
    private String getUserID() {
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

    /**
     * Retrieves Google Advertising Id if it preset
     * If operation failed, returns empty string,
     * doesn't return null in any situation.
     * Call it only in background thread!
     *
     * @param context Current context
     * @return Google Advertising Id
     */
    private String getGoogleId(Context context) {
        String advertisingId;
        try {
            AdvertisingIdClient.Info info = AdvertisingIdClient.getAdvertisingIdInfo(context);
            advertisingId = info.getId() == null ? "" : info.getId();
        } catch(final Throwable t) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to retrieve GOOGLE_AID", t);
            }
            advertisingId = "";
        }
        return advertisingId;
    }

    /**
     * Retrieves Amazon FireOS Id if it preset (if it's Amazon)
     * If operation failed, returns empty string,
     * doesn't return null in any situation.
     *
     * @param context Current context
     * @return Google Advertising Id
     */
    private String getFireOsId(Context context) {
        String fireOsId;
        try {
            fireOsId = Settings.Secure.getString(context.getContentResolver(), "advertising_id");
            if(fireOsId == null)
                fireOsId = "";
        } catch(Throwable t) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to retrieve FIREOS_AID", t);
            }
            fireOsId = "";
        }
        return fireOsId;
    }
}
