/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.internal;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;

import androidx.annotation.NonNull;

/**
 * Utility class for AppMetr
 */
public class Utils {
    /**
     * Methods encode event stack into Base64 string
     *
     * @param eventStack - array of event from last saving
     * @return - Base64 string with event description
     * @throws JSONException
     */
    public static String getEncodedString(ArrayList<JSONObject> eventStack, int batchId) throws JSONException {
        JSONObject batch = new JSONObject();

        batch.put("batchId", batchId);
        batch.put("batch", new JSONArray(eventStack));

        String data = batch.toString();
        return data;
    }

    /**
     * Methods check fields in payment object
     *
     * @param payment - object with fields that describe payment event
     * @return - means all fields are valid
     * @throws DataFormatException - function throws exception if fields are not valid
     */
    public static void validatePayments(JSONObject payment) throws DataFormatException {
        if (!payment.has("psUserSpentCurrencyCode") || !payment.has("psUserSpentCurrencyAmount")) {
            throw new DataFormatException("Not full information");
        }
    }

    /**
     * Method which converts data into Unix-way format
     *
     * @param event - input JSONObject which described event
     * @return - event with converted dates
     * @throws JSONException
     */
    public static JSONObject convertDateToLong(JSONObject event) throws JSONException {
        Iterator<?> keysIterator = event.keys();
        while (keysIterator.hasNext()) {
            String key = (String) keysIterator.next();
            Object value = event.get(key);

            Class<?> valueClass = value.getClass();
            if (valueClass.equals(Date.class)) {
                event.put(key, Long.valueOf(((Date) value).getTime()));
            } else if (valueClass.equals(JSONObject.class)) {
                convertDateToLong((JSONObject) value);
            }
        }

        return event;
    }

    public static byte[] compressData(byte[] data) {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
        byte[] buffer = new byte[data.length];
        int length;
        try {
            deflater.setInput(data);
            deflater.finish();
            length = deflater.deflate(buffer);
        } finally {
            deflater.end();
        }

        byte[] result = new byte[length];
        System.arraycopy(buffer, 0, result, 0, length);
        return result;
    }

    public static FileOutputStream getFileOutputStream(@NonNull final Context context, String fileName) throws FileNotFoundException {
        ApplicationInfo appInfo = context.getApplicationInfo();
        String batchSubfolder = appInfo.metaData.getString("appmetrSubfolder");
        File dir;
        if (!TextUtils.isEmpty(batchSubfolder)) {
            dir = new File(context.getFilesDir(), batchSubfolder);

            dir.mkdirs();
        }
        else {
            dir = new File(context.getFilesDir().getPath());
        }

        return new FileOutputStream(new File(dir, fileName));
    }

    public static FileInputStream getFileInputStream(@NonNull final Context context, String fileName) throws FileNotFoundException {
        ApplicationInfo appInfo = context.getApplicationInfo();
        String batchSubfolder = appInfo.metaData.getString("appmetrSubfolder");
        File dir;
        if (!TextUtils.isEmpty(batchSubfolder)) {
            dir = new File(context.getFilesDir(), batchSubfolder);

            dir.mkdirs();
        }
        else {
            dir = new File(context.getFilesDir().getPath());
        }

        return new FileInputStream(new File(dir, fileName));
    }
}
