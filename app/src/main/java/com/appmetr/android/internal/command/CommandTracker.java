/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.internal.command;

import android.util.Base64;
import android.util.Log;
import com.appmetr.android.AppMetr;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Specialized class to track commands
 */
public class CommandTracker {
    private static final String TAG = "CommanTracker";

    private static void trackCommand(String commandID, String status, JSONObject properties) {
        try {
            JSONObject action = (properties != null ? properties : new JSONObject());
            action.put("action", "trackCommand");
            action.put("commandId", commandID);
            action.put("status", status);

            AppMetr.getInstance().track(action);
        } catch (final JSONException e) {
            Log.e(TAG, "trackCommand (" + status + ") failed", e);
        }
    }

    /**
     * Tracks a succeeded commands
     *
     * @param commandID An unique identifier of successful processed command
     */
    public static void trackCommand(String commandID) {
        trackCommand(commandID, "success", null);
    }

    /**
     * Tracks a skipped command
     *
     * @param commandID  An unique identifier of skipped command
     * @param properties A reason of skipping command
     */
    public static void trackCommandSkip(String commandID, JSONObject properties) {
        trackCommand(commandID, "skip", properties);
    }

    /**
     * Tracks a skipped command
     *
     * @param commandID  An unique identifier of skipped command
     * @param skipReason A reason of skipping command
     */
    public static void trackCommandSkip(String commandID, String skipReason) {
        try {
            trackCommand(commandID, "skip", new JSONObject().put("reason", skipReason));
        } catch (final JSONException e) {
            Log.e(TAG, "trackCommand (skip) failed", e);
        }
    }

    /**
     * Tracks any error while processing command
     *
     * @param commandID  A unique identifier of processed command
     * @param properties A JSON object that describe error
     */
    public static void trackCommandFail(String commandID, JSONObject properties) {
        trackCommand(commandID, "fail", properties);
    }

    /**
     * Tracks any error while processing command
     *
     * @param commandID A unique identifier of processed command
     * @param error     An error
     */
    public static void trackCommandFail(String commandID, Throwable error) {
        try {
            JSONObject properties = new JSONObject();

            properties.put("errorCode", "invalidFormat");
            properties.put("errorDescription", error.getMessage());
            properties.put("backtrace", throwableToEncodedBase64(error));

            trackCommandFail(commandID, properties);
        } catch (final JSONException e) {
            Log.e(TAG, "trackCommand (fail) failed", e);
        }
    }

    protected static String throwableToEncodedBase64(Throwable error) {
        String encodedData = null;
        StackTraceElement[] stackTraceElements = error.getStackTrace();

        if (stackTraceElements != null) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < stackTraceElements.length; i++) {
                stringBuilder.append("#").append(i).append(" ").append(stackTraceElements[i].toString()).append('\n');
            }
            encodedData = Base64.encodeToString(stringBuilder.toString().getBytes(), Base64.NO_WRAP);
        }

        return encodedData;
    }

    public static void trackCommandBatch(String lastCommandID, String error, String errorDescription) {
        try {
            JSONObject action = new JSONObject().put("action", "trackCommandBatch");
            if (lastCommandID != null) {
                action.put("lastCommandId", lastCommandID);
            }
            action.put("status", "fail");
            action.put("error", error);
            action.put("errorDescription", errorDescription);

            AppMetr.getInstance().track(action);
        } catch (JSONException e) {
            Log.e(TAG, "trackCommand failed", e);
        }
    }
}
