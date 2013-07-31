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
     * Waiting for OpenUDID initialization
     *
     * @return True if OpenUDID initialized, otherwise return false
     */
    public static boolean waitOpenUDIDInitialized() {
        boolean ret;
        int attempts = 1500;
        while (!(ret = OpenUDID_manager.isInitialized()) && attempts > 0) {
            attempts--;
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // nothing to do
            }
        }

        return ret;
    }

    /**
     * The Method to call to get OpenUDID
     *
     * @return the OpenUDID
     */
    public static String getOpenUDID() {
        try {
            waitOpenUDIDInitialized();
            String ret = OpenUDID_manager.getOpenUDID();
            return (ret == null ? "unknown" : ret);
        } catch (final Throwable t) {
            Log.e(TAG, "Failed to get Open UDID", t);
            return null;
        }
    }
}
