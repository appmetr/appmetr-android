/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.internal.command.exception;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

public class AppMetrInvalidCommandException extends java.lang.Exception implements ICommandException {
    private static final long serialVersionUID = 1L;
    private static final String TAG = "AppMetrInvalidCommandException";

    public AppMetrInvalidCommandException(String detailMessage) {
        super(detailMessage);
    }

    public JSONObject getProperties() {
        JSONObject properties = new JSONObject();
        try {
            properties.put("errorCode", "invalidFormat");
            properties.put("errorDescription", getMessage());
        } catch (final JSONException e) {
            // impossible situation
            Log.e(TAG, "Something strange with JSON!", e);
        }

        return properties;
    }
}
