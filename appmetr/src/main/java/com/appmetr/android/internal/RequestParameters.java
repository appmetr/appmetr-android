/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.internal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.appmetr.android.BuildConfig;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Parameters, passed to http request on events upload
 */
public class RequestParameters {
    private static final String TAG = "RequestParameters";

    /**
     * This device ID appearing in ALL Droid 2 devices, and other Froyo builds
     */
    private static final String MAGIC_ANDROID_ID = "9774d56d682e549c";
    private static String googleAid = null;
    private static String fireOsId = null;

    private final String macAddress;
    private final String deviceId;
    private final String buildSerial;
    private final String androidId;
    private final String userId;
    private final String token;

    /**
     * Retrieve request parameters from context
     */
    public RequestParameters(@NonNull final Context context, @NonNull String token) {
        this.token = token;
        macAddress = getMacAddress(context);
        deviceId = getDeviceID(context);
        buildSerial = getBuildSerial();
        androidId = getAndroidID(context);
        userId = getUserID();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                googleAid = getGoogleId(context);
            }
        });
    }

    public String getToken() {
        return token;
    }

    public String getUID() {
        return userId;
    }

    public String getDeviceKey(Context context) {
        List<HttpNameValuePair> nameValuePairs = new ArrayList<HttpNameValuePair>();
        nameValuePairs.add(new HttpNameValuePair("token", getToken().toLowerCase(Locale.US)));
        nameValuePairs.add(new HttpNameValuePair("mobDeviceType", getDeviceType()));
        nameValuePairs.add(new HttpNameValuePair("mobMac", getHash(getMacAddress(context))));
        nameValuePairs.add(new HttpNameValuePair("mobTmDevId", getHash(getDeviceID(context))));
        nameValuePairs.add(new HttpNameValuePair("mobAndroidID", getHash(getAndroidID(context))));
        nameValuePairs.add(new HttpNameValuePair("mobGoogleAid", getHash(googleAid)));
                                                // use googleAid only if we already have it
        nameValuePairs.add(new HttpNameValuePair("mobFireOsAid", getHash(getFireOsId(context))));
        StringBuilder res = new StringBuilder();
        for (HttpNameValuePair pair : nameValuePairs) {
            if(TextUtils.isEmpty(pair.getValue()))
                continue;
            if (res.length() > 0) {
                res.append("&");
            }
            res.append(pair.toString());
        }
        return res.toString();
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
        ret.add(new HttpNameValuePair("token", token));
        ret.add(new HttpNameValuePair("userId", userId));
        ret.add(new HttpNameValuePair("timestamp", Long.toString(new Date().getTime())));

        ret.add(new HttpNameValuePair("mobDeviceType", getDeviceType()));
        ret.add(new HttpNameValuePair("mobOSVer", Build.VERSION.RELEASE));
        ret.add(new HttpNameValuePair("mobLibVer", LibraryPreferences.VERSION_STRING));
        ret.add(new HttpNameValuePair("mobAndroidID", androidId));

        String aid = getGoogleId(context);
        if (!TextUtils.isEmpty(aid)) {
            ret.add(new HttpNameValuePair("mobGoogleAid", aid));
        } else {
            // may be it's Amazon?
            aid = getFireOsId(context);
            if (!TextUtils.isEmpty(aid)) {
                ret.add(new HttpNameValuePair("mobFireOsAid", aid));
            }
        }

        if (macAddress != null) {
            ret.add(new HttpNameValuePair("mobMac", macAddress));
        }

        if (buildSerial != null) {
            ret.add(new HttpNameValuePair("mobBuildSerial", buildSerial));
        }

        if (deviceId != null) {
            ret.add(new HttpNameValuePair("mobTmDevId", deviceId));
        }

        return ret;
    }

    private static String getDeviceType() {
        return Build.MANUFACTURER + "," + Build.MODEL;
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
                        ret = ret.replaceAll("\\W", "").toUpperCase(Locale.US);
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
                Log.e(TAG, "Failed to retrieve androidId: " + t.getMessage());
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
        String ret = androidId;
        if (ret == null || ret.length() == 0 || ret.equals(MAGIC_ANDROID_ID)) {
            if (deviceId != null) {
                ret = deviceId;
            } else if (macAddress != null) {
                ret = macAddress;
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
    private static String getGoogleId(Context context) {
        /* Lazy Google AID requesting */
        if(googleAid == null) {
            try {
                AdvertisingIdClient.Info info = AdvertisingIdClient.getAdvertisingIdInfo(context);
                googleAid = info.getId() == null ? "" : info.getId();
            } catch (final Throwable t) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Failed to retrieve googleAid", t);
                }
                googleAid = "";
            }
        }
        return googleAid;
    }

    /**
     * Retrieves Amazon FireOS Id if it preset (if it's Amazon)
     * If operation failed, returns empty string,
     * doesn't return null in any situation.
     *
     * @param context Current context
     * @return FireOS Advertising Id
     */
    private static String getFireOsId(Context context) {
        if(fireOsId == null) {
            try {
                fireOsId = Settings.Secure.getString(context.getContentResolver(), "advertising_id");
                if (fireOsId == null)
                    fireOsId = "";
            } catch (Throwable t) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Failed to retrieve FIREOS_AID", t);
                }
                fireOsId = "";
            }
        }
        return fireOsId;
    }

    private static String getHash(String data) {
        if (TextUtils.isEmpty(data))
            return data;
        return new MurmurHash3().putString(data.toLowerCase(Locale.US)).hashString();
    }
}
