/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import com.appmetr.android.internal.AppMetrTrackingManager;
import com.appmetr.android.internal.ContextProxy;
import com.appmetr.android.internal.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.DataFormatException;

/**
 * Main library class.
 */
public class AppMetr extends AppMetrTrackingManager {
    private final static String TAG = "AppMetr";

    protected static AppMetr msInstance;

    private final static Lock msLibraryInitializationLock = new ReentrantLock();

    /**
     * Static method. It returns an instance of library.
     * This method throws exception if it's called before the setup method is
     * called.
     *
     * @return - instance of library.
     */
    public static AppMetr getInstance() {
        if (msInstance == null) {
            throw new RuntimeException("Can not return instance before setup a token.");
        }
        return msInstance;
    }

    /**
     * Standard constructor. Initializes library with a specified activity.
     *
     * @param context Application context
     */
    protected AppMetr(Context context) {
        super(context);
    }

    /**
     * Static method for additional setup libraries data. Must be valid
     * parameter token.
     *
     * @param token    parameter which is needed for data upload.
     * @param context  The application context to initialize AppMetr library
     * @throws DataFormatException - library throws exception if token is not valid
     */
    public static void setup(String token, Context context) throws DataFormatException, SecurityException {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "#setup");
        }

        // Locking this thread so prevent conflict while initializing from several thread
        msLibraryInitializationLock.lock();

        if (msInstance == null) {
            msInstance = new AppMetr(context);
            msInstance.initialize(token);
        } else if (BuildConfig.DEBUG) {
            Log.d(TAG, "setup failed. Library already initialized.");
        }

        // releasing thread lock
        msLibraryInitializationLock.unlock();
    }

    /**
     * Public method which is called when activity goes into background mode.
     */
    public static void onPause() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "#onPause");
        }

        // Lock this thread
        msLibraryInitializationLock.lock();

        if (msInstance != null) {
            msInstance.sleepLibrary();
        }

        // releasing thread lock
        msLibraryInitializationLock.unlock();
    }

    /**
     * Public method which is called when application goes into foreground mode.
     */
    public static void onResume() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "#onResume");
        }

        // Lock this thread
        msLibraryInitializationLock.lock();

        if (msInstance != null) {
            msInstance.restoreLibrary();
        }

        // releasing thread lock
        msLibraryInitializationLock.unlock();
    }

    /**
     * Methods for attaching only built-in user properties.
     */
    public static void attachProperties() {
        attachProperties(null);
    }

    /**
     * Methods for attaching user properties
     *
     * @param properties - user properties
     */
    public static void attachProperties(JSONObject properties) {
        try {
            if (properties == null) {
                properties = new JSONObject();
            }

            JSONObject action = new JSONObject().put("action", "attachProperties");
            action.put("properties", properties);
            properties.put("$version", ContextProxy.AppVersion);

            if (!properties.has("$country")) {
                properties.put("$country", Locale.getDefault().getCountry());
            }

            if (!properties.has("$language")) {
                properties.put("$language", Locale.getDefault().getLanguage());
            }

            if(!properties.has("$locale")) {
                properties.put("$locale", Locale.getDefault().toString());
            }

            getInstance().track(action);
        } catch (JSONException error) {
            Log.e(TAG, "attachProperties failed", error);
        }
    }

    /**
     * Method for tracking game event as "track session" without parameters
     */
    public static void trackSession() {
        getInstance().trackSessionImpl(null);
    }

    /**
     * Method for tracking game event as "track session" with parameters
     *
     * @param properties - properties for event
     */
    public static void trackSession(JSONObject properties) {
        getInstance().trackSessionImpl(properties);
    }

    /**
     * Method for tracking game event as "track level" with parameters.
     *
     * @param level - parameter required for this event. Displays player's level.
     */
    public static void trackLevel(int level) {
        trackLevel(level, null);
    }

    /**
     * Method for tracking game event as "track level" with parameter "level"
     * and additional parameters.
     *
     * @param level      - parameter required for this event. Displays player's level.
     * @param properties - additional parameter for this event.
     */
    public static void trackLevel(int level, JSONObject properties) {
        try {
            JSONObject action = new JSONObject().put("action", "trackLevel");
            action.put("level", level);
            if (properties != null) {
                action.put("properties", properties);
            }

            getInstance().track(action);
        } catch (JSONException error) {
            Log.e(TAG, "trackLevel failed", error);
        }
    }

    /**
     * Method for tracking game event as "track event" with parameter "event".
     *
     * @param event - required field. Displays event's name
     */
    public static void trackEvent(String event) {
        trackEvent(event, null);
    }

    /**
     * Method for tracking game event as "track event" with parameters
     * "event","value" and "properties".
     *
     * @param event      - required field. Displays event's name
     * @param properties - additional parameters for event
     */
    public static void trackEvent(String event, JSONObject properties) {
        try {
            JSONObject action = new JSONObject().put("action", "trackEvent");
            action.put("event", event);

            if (properties != null) {
                action.put("properties", properties);
            }

            getInstance().track(action);
        } catch (JSONException error) {
            Log.e(TAG, "trackEvent failed", error);
        }
    }

    /**
     * Method must be called when player does any payment
     *
     * @param payment - JSONObject with required fields "psUserSpentCurrencyCode",
     *                "psUserSpentCurrencyAmount",
     *                "psReceivedCurrencyCode" and "psReceivedCurrencyAmount".
     * @throws DataFormatException
     */
    public static void trackPayment(JSONObject payment) throws DataFormatException {
        trackPayment(payment, null);
    }

    /**
     * Method must be called when player does any payment
     *
     * @param payment    - JSONObject with required fields "psUserSpentCurrencyCode",
     *                   "psUserSpentCurrencyAmount",
     *                   "psReceivedCurrencyCode" and "psReceivedCurrencyAmount". If
     *                   these fields are not found library throws an exception
     * @param properties - JSONObject with additional information for event
     * @throws DataFormatException
     */
    public static void trackPayment(JSONObject payment, JSONObject properties) throws DataFormatException {
        Utils.validatePayments(payment);
        try {
            payment.put("action", "trackPayment");
            if (properties != null) {
                payment.put("properties", properties);
            }
            getInstance().track(payment);
        } catch (JSONException error) {
            Log.e(TAG, "trackPayment failed", error);
        }
    }

    /**
     * Registering advertising event track
     * @param event Event to track
     */
    public static void trackAdsEvent(String event) {
        try {
            JSONObject action = new JSONObject().put("action", "adsEventBroadcast");
            action.put("event", event);

            getInstance().track(action);
        } catch (JSONException error) {
            Log.e(TAG, "adsEventBroadcast failed", error);
        }
    }

    /**
     * Registering the URL of game installation
     *
     * @param url The string with URL to register
     */
    public static void trackInstallURL(String url) {
        try {
            if (!getInstance().mPreferences.getIsInstallURLTracked()) {
                JSONObject action = new JSONObject().put("action", "trackInstallURL");
                action.put("installURL", url);

                msInstance.track(action);
                msInstance.flushAndUploadAllEventsAsync();
                msInstance.mPreferences.setIsInstallURLTracked(true);
            }
        } catch (JSONException error) {
            Log.e(TAG, "trackInstallURL failed", error);
        }
    }

    /**
     * Track experiment
     *
     * @param experiment Experiment name, which is to be started
     * @param group      Group name for experiment
     */
    public static void trackExperimentStart(String experiment, String group) {
        try {
            JSONObject action = new JSONObject().put("action", "trackExperiment");
            action.put("status", "ON");
            action.put("experiment", experiment);
            action.put("group", group);

            getInstance().track(action);
        } catch (JSONException error) {
            Log.e(TAG, "trackExperiment failed", error);
        }
    }

    /**
     * Track experiment
     *
     * @param experiment Experiment name, which is to be ended
     */
    public static void trackExperimentEnd(String experiment) {
        try {
            JSONObject action = new JSONObject().put("action", "trackExperiment");
            action.put("status", "END");
            action.put("experiment", experiment);

            getInstance().track(action);
        } catch (JSONException error) {
            Log.e(TAG, "trackExperiment failed", error);
        }
    }

    /**
     * Track user state
     *
     * @param state - key-value data with user state
     */
    public static void trackState(JSONObject state) {
        try {
            JSONObject action = new JSONObject().put("action", "trackState");
            action.put("state", state);

            getInstance().track(action);
        } catch (JSONException error) {
            Log.e(TAG, "trackState failed", error);
        }
    }

    /**
     * Identify user
     *
     * @param userId - user id
     */
    public static void identify(String userId) {
        try {
            JSONObject action = new JSONObject().put("action", "identify");
            action.put("userId", userId);

            getInstance().track(action);
            getInstance().flushAndUploadAllEventsAsync();
        } catch (JSONException error) {
            Log.e(TAG, "Identify failed", error);
        }
    }

    /**
     * Verify payment
     *
     * @param purchaseInfo - getOriginalJson().toString() for Purchase object
     * @param signature - getSignature() for Purchase object
     * @param privateKey - set in deploy setting on appmetr server
     * @return true - if payment is valid, and false otherwise
     */
    public static boolean verifyPayment(String purchaseInfo, String signature, String privateKey) {
        return getInstance().verifyPaymentAndCheck(purchaseInfo, signature, privateKey);
    }

    /**
     * Attach attributes to separate entity instead of user
     *
     * @param entityName - name of entity to attach
     * @param entityValue - identity value of entity
     * @param properties - attributes to attach
     */
    public static void attachEntityAttributes(String entityName, String entityValue, JSONObject properties) {
        try {
            if(TextUtils.isEmpty(entityName))
                throw new IllegalArgumentException("entityName is null or empty");
            if(TextUtils.isEmpty(entityValue))
                throw new IllegalArgumentException("entityValue is null or empty");
            if (properties == null || properties.length() == 0)
                throw new IllegalArgumentException("properties is null or empty");

            JSONObject action = new JSONObject().put("action", "attachEntityAttributes");
            action.put("entityName", entityName);
            action.put("entityValue", entityValue);
            action.put("properties", properties);
            getInstance().track(action);
        } catch (JSONException error) {
            Log.e(TAG, "AttachEntityAttributes failed", error);
        }
    }

    /**
     * Force flush events to file. Flushing execute in new thread
     */
    public static void flushLocal() {
        getInstance().flushAllEventsAsync();
    }

    /**
     * Force flush events on server. Flushing execute in new thread
     */
    public static void flush() {
        getInstance().flushAndUploadAllEventsAsync();
    }

    /**
     * @return The user unique identifier used by this library.
     */
    public static String getUserId() {
        return getInstance().mRequestParameters.getUID();
    }

    /**
     * @return an unique identifier of current installation instance
     */
    public static String getInstanceIdentifier() {
        String token = getInstance().mRequestParameters.getToken();
        String result = "";
        if (token != null) {
            int len = Math.min(token.length(), 8);
            if (len > 0) {
                result = token.substring(0, len);
            }
        }

        return result + ":" + getUserId();
    }
}
