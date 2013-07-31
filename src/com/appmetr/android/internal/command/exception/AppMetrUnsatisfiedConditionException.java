/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.internal.command.exception;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

public class AppMetrUnsatisfiedConditionException extends java.lang.Exception implements ICommandException {
    private static final long serialVersionUID = 1L;
    private static final String TAG = "AppMetrUnsatisfiedConditionException";

    public AppMetrUnsatisfiedConditionException(String detailMessage) {
        super(detailMessage);
    }

    public JSONObject getProperties() {
        JSONObject properties = new JSONObject();
        try {
            properties.put("reason", getMessage());
        } catch (final JSONException e) {
            // impossible situation
            Log.e(TAG, "Something strange with JSON!", e);
        }

        return properties;
    }
}
