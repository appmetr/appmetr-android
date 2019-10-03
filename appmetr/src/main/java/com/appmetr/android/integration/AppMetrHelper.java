package com.appmetr.android.integration;

import android.util.Log;
import com.appmetr.android.AppMetr;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AppMetrHelper {
    private static final String TAG = "AppMetrHelper";

    public static final String GOOGLE_CHECKOUT = "google_checkout";
    private static String msDefaultPaymentProcessor = GOOGLE_CHECKOUT;

    /**
     * Method for tracking game event as "track session" without parameters
     */
    public static void trackSession() {
        try {
            AppMetr.trackSession();
        } catch (final Throwable error) {
            Log.e(TAG, "trackSession failed", error);
        }
    }

    /**
     * Method for tracking game event as "track session" with parameters
     */
    public static void trackSession(String serializedProperties) {
        try {
            AppMetr.trackSession(new JSONObject(serializedProperties));
        } catch (final Throwable error) {
            Log.e(TAG, "trackSession failed", error);
        }
    }

    /**
     * Method for tracking game event as "track level" without parameters
     */
    public static void trackLevel(int level) {
        try {
            AppMetr.trackLevel(level);
        } catch (final Throwable error) {
            Log.e(TAG, "trackLevel failed", error);
        }
    }

    /**
     * Method for tracking game event as "track level" with parameters
     */
    public static void trackLevel(int level, String serializedProperties) {
        try {
            AppMetr.trackLevel(level, new JSONObject(serializedProperties));
        } catch (final Throwable error) {
            Log.e(TAG, "trackLevel failed", error);
        }
    }

    /**
     * Methods for tracking game event as "attach properties"
     */
    public static void attachProperties() {
        try {
            AppMetr.attachProperties();
        } catch (final Throwable error) {
            Log.e(TAG, "attachProperties failed", error);
        }
    }

    /**
     * Methods for tracking game event as "attach properties"
     *
     * @param serializedProperties - a String that contains JSON object
     */
    public static void attachProperties(String serializedProperties) {
        try {
            AppMetr.attachProperties(new JSONObject(serializedProperties));
        } catch (final Throwable error) {
            Log.e(TAG, "attachProperties failed", error);
        }
    }

    /**
     * Method must be called when player does any payment
     */
    public static void trackPayment(String serializedPayment) {
        try {
            AppMetr.trackPayment(paymentWithPaymentProcessor(serializedPayment));
        } catch (final Throwable error) {
            Log.e(TAG, "trackPayment failed", error);
        }
    }

    /**
     * Method must be called when player does any payment
     */
    public static void trackPayment(String serializedPayment, String serializedProperties) {
        try {
            AppMetr.trackPayment(paymentWithPaymentProcessor(serializedPayment), new JSONObject(serializedProperties));
        } catch (final Throwable error) {
            Log.e(TAG, "trackPayment failed", error);
        }
    }

    /**
     * Sets the default payment
     *
     * @param processor The value of default processor
     */
    public static void setDefaultPaymentProcessor(String processor) {
        msDefaultPaymentProcessor = processor;
    }

    private static JSONObject paymentWithPaymentProcessor(String serializedPayment) throws JSONException {
        JSONObject ret = new JSONObject(serializedPayment);
        if (msDefaultPaymentProcessor != null && !ret.has("processor")) {
            ret.put("processor", msDefaultPaymentProcessor);
        }
        return ret;
    }

    /**
     * Method for tracking arbitrary events
     */
    public static void trackEvent(String event) {
        try {
            AppMetr.trackEvent(event);
        } catch (final Throwable error) {
            Log.e(TAG, "trackEvent failed", error);
        }
    }

    /**
     * Method for tracking arbitrary events
     */
    public static void trackEvent(String event, String serializedProperties) {
        try {
            AppMetr.trackEvent(event, new JSONObject(serializedProperties));
        } catch (final Throwable error) {
            Log.e(TAG, "trackEvent failed", error);
        }
    }

    /**
     * Method for tracking experiment start
     */
    public static void trackExperimentStart(String experiment, String group) {
        try {
            AppMetr.trackExperimentStart(experiment, group);
        } catch (final Throwable error) {
            Log.e(TAG, "trackExperiment failed", error);
        }
    }

    /**
     * Method for tracking experiment end
     */
    public static void trackExperimentEnd(String experiment) {
        try {
            AppMetr.trackExperimentEnd(experiment);
        } catch (final Throwable error) {
            Log.e(TAG, "trackExperiment failed", error);
        }
    }

    /**
     * Method for track user state
     */
    public static void trackState(String state) {
        try {
            AppMetr.trackState(new JSONObject(state));
        } catch (final Throwable error) {
            Log.e(TAG, "trackState failed", error);
        }
    }

    /**
     * Method for identify
     */
    public static void identify(String userId) {
        try {
            AppMetr.identify(userId);
        } catch (final Throwable error) {
            Log.e(TAG, "identify failed", error);
        }
    }

    /**
     * Method for getting device key
     * @return device key
     */
    public static String getDeviceKey() {
        try {
            return AppMetr.getInstance().getDeviceKey();
        } catch(final Throwable error) {
            Log.e(TAG, "getDeviceKey failed", error);
        }
        return null;
    }

    /**
     * Attach properties to separate entity instead of user
     */
    public static void attachEntityAttributes(String entityName, String entityValue, String serializedProperties) {
        try {
            JSONObject properties = new JSONObject(serializedProperties);
            AppMetr.attachEntityAttributes(entityName, entityValue, properties);
        } catch (final Throwable error) {
            Log.e(TAG, "attachEntityAttributes failed", error);
        }
    }
}
