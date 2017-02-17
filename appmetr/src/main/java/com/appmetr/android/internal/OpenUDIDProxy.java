/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.internal;

import android.util.Log;
import org.OpenUDID.OpenUDID_manager;

/**
 * Proxy class for OpenUDID
 */
public class OpenUDIDProxy {
    private static final String TAG = "OpenUDIDProxy";

    /**
     * The Method to call to get OpenUDID
     *
     * @return the OpenUDID
     */
    public static String getOpenUDID() {
        try {
            OpenUDID_manager.waitOpenUDIDInitialized(15000);
            String ret = OpenUDID_manager.getOpenUDID();
            return (ret == null ? "unknown" : ret);
        } catch (final Throwable t) {
            Log.e(TAG, "Failed to get Open UDID", t);
            return null;
        }
    }
}
