/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.internal;

import android.util.Log;
import com.appmetr.android.BuildConfig;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

/**
 * Class which sends requests to web server.
 */
public class WebServiceRequest {
    final static String TAG = "WebServiceRequest";
    final String mUrlPath;

    /**
     * Default constructor
     *
     * @param URLPath - path for web service.
     */
    public WebServiceRequest(String URLPath) {
        mUrlPath = URLPath;
    }

    /**
     * Method which sends string request to web service.
     *
     * @return - "true" if server response equal to kPositiveServerResponse.
     *         Else returns "false".
     */
    public boolean sendRequest(List<HttpNameValuePair> parameters, byte[] batches) throws IOException {
        URL url = new URL(getUrlPath(parameters));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            // Add body data
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/octet-stream");

            connection.setFixedLengthStreamingMode(batches.length);
            OutputStream out = connection.getOutputStream();
            out.write(batches);
            out.close();

            // Execute HTTP Post Request
            BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder result = new StringBuilder();
            try {
                String inputLine;
                while ((inputLine = input.readLine()) != null) {
                    result.append(inputLine);
                }
            } finally {
                input.close();
            }

            try {
                String status = new JSONObject(result.toString()).getJSONObject("response").getString("status");
                if (status != null && status.compareTo("OK") == 0) {
                    return true;
                }
            } catch (JSONException jsonError) {
                // nothing to do
            }

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Invalid server response: " + result);
            }
        } catch (Exception error) {
            Log.e(TAG, "Server error", error);
            if (BuildConfig.DEBUG) {
                Log.d(TAG,
                        "Please, check rights for the app in AndroidManifest.xml."
                                + " For the app to have access to the network the uses permission \"android.permission.INTERNET\" "
                                + "must be set. You can find a detailed description here: http://developer.android.com/reference/android/Manifest.permission.html#INTERNET");
            }
        } finally {
            connection.disconnect();
        }

        return false;
    }

    protected String getUrlPath(List<HttpNameValuePair> parameters) {
        String res = "";
        for (HttpNameValuePair pair : parameters) {
            if (res.length() > 0) {
                res += "&";
            }

            String value = pair.getValue();
            if (value != null) {

                res += pair.toString();
            } else {
                Log.e(TAG, "Invalid parameter " + pair.getName());
            }
        }

        return mUrlPath + "?" + res;
    }

    public JSONObject sendRequest(List<HttpNameValuePair> parameters) throws IOException, JSONException, HttpException {
        URL url = new URL(getUrlPath(parameters));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder result = new StringBuilder();
            try {
                String inputLine;
                while ((inputLine = input.readLine()) != null) {
                    result.append(inputLine);
                }
            } finally {
                input.close();
            }

            if (connection.getResponseCode() >= 400) {
                throw new HttpException("Invalid response code: " + connection.getResponseCode());
            }

            if (!connection.getContentType().contains("application/json")) {
                throw new HttpException("Invalid content type: " + connection.getContentType());
            }

            return new JSONObject(result.toString());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
