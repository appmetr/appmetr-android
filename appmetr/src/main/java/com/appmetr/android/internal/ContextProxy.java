/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.internal;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import com.appmetr.android.BuildConfig;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Proxy class for application context
 */
public class ContextProxy {
    private static final String TAG = "ContextProxy";

    /**
     * The version name of application
     */
    public static String AppVersion = "not initialized";

    private final Context mContext;

    /**
     * Creating context proxy
     *
     * @param context The android application context
     */
    public ContextProxy(Context context) {
        mContext = context;
        AppVersion = getVersion(context);
    }

    /**
     * @return The android application context
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * Retrieves the version name for application
     *
     * @param context The application context
     * @return The version name for application
     */
    private static String getVersion(Context context) {
        String ret = null;
        try {
            ret = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (final NameNotFoundException error) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to retrieve app version.", error);
            }
        }

        return (ret == null ? "unknown" : ret);
    }

    /**
     * Delete the given private file associated with this Context's application
     * package.
     *
     * @param fileName Delete the given private file associated with this Context's
     *                 application package.
     * @return Delete the given private file associated with this Context's
     *         application package.
     */
    public boolean deleteFile(String fileName) {
        return mContext.deleteFile(fileName);
    }

    /**
     * Private method which returns content of file with filename.
     *
     * @param fileName file name.
     * @return content of file.
     * @throws IOException
     */
    public byte[] getFileContent(String fileName) throws IOException {
        FileInputStream inputFile = mContext.openFileInput(fileName);

        byte[] streamBuffer = new byte[inputFile.available()];
        inputFile.read(streamBuffer);
        inputFile.close();

        return streamBuffer;
    }
}
