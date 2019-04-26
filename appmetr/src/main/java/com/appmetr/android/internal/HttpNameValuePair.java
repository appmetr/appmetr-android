/**
 * Copyright (c) 2017 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.internal;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Class holds name-value string pair and outputs
 * URL-encoded string as GET parameter
 */

public class HttpNameValuePair {
    private final static String TAG = "HttpNameValuePair";

    private final String name;
    private final String value;

    /**
     * Default Constructor taking a name and a value. The value may be null.
     *
     * @param name The name.
     * @param value The value.
     */
    public HttpNameValuePair(String name, String value) {
        this.name = name == null? "Name" : name;
        this.value = value;
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        try {
            res.append(URLEncoder.encode(this.name, "UTF-8"));
            res.append("=");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Failed to encode http param name in " + this.getName());
        }

        if (this.value != null) {
            try {
                res.append(URLEncoder.encode(this.value, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Failed to encode http param value in " + this.getName());
            }
        }
        return res.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HttpNameValuePair that = (HttpNameValuePair) o;

        if (!name.equals(that.name)) return false;
        return value != null ? value.equals(that.value) : that.value == null;

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
