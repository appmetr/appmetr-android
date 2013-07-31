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
     * Method for tracking options
     */
    public static void trackOptions(String commandId, String serializedOptions) {
        try {
            AppMetr.trackOptions(commandId, new JSONArray(serializedOptions));
        } catch (final Throwable error) {
            Log.e(TAG, "trackOptions failed", error);
        }
    }

    /**
     * Method for tracking options with error
     */
    public static void trackOptions(String commandId, String serializedOptions, String errorCode, String errorMessage) {
        try {
            AppMetr.trackOptionsError(commandId, new JSONArray(serializedOptions), errorCode, errorMessage);
        } catch (final Throwable error) {
            Log.e(TAG, "trackOptions failed", error);
        }
    }

    /**
     * Sets whether a sandbox mode enabled or not
     *
     * @param enabled On/off a sandbox mode
     * @since 1.4
     * @deprecated in 1.5
     */
    @Deprecated
    public static void setSandboxModeEnabled(boolean enabled) {
        // nothing to do
    }
}
