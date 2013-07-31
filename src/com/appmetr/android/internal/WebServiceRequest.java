/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.internal;

import android.net.http.AndroidHttpClient;
import android.util.Log;
import com.appmetr.android.BuildConfig;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
    public boolean sendRequest(List<NameValuePair> parameters, byte[] batches) {
        AndroidHttpClient httpclient = AndroidHttpClient.newInstance("AppMetr for Android");
        String urlPath = getUrlPath(parameters);
        HttpPost httppost = new HttpPost(urlPath);

        try {
            httppost.setHeader("Content-Type", "application/octet-stream");

            // Add body data
            byte[] fixedBatch = new byte[batches.length + 1];
            System.arraycopy(batches, 0, fixedBatch, 0, batches.length);
            fixedBatch[batches.length] = 0;

            ByteArrayEntity entity = new ByteArrayEntity(fixedBatch);
            entity.setContentType("application/octet-stream");

            httppost.setEntity(entity);

            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);
            BufferedReader input = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            String inputLine, result = "";
            while ((inputLine = input.readLine()) != null) {
                result += inputLine;
            }
            input.close();

            try {
                String status = new JSONObject(result).getJSONObject("response").getString("status");
                if (status != null && status.compareTo("OK") == 0) {
                    httpclient.close();
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
        }

        httpclient.close();
        return false;
    }

    protected String getUrlPath(List<NameValuePair> parameters) {
        String res = "";
        for (NameValuePair pair : parameters) {
            if (res.length() > 0) {
                res += "&";
            }

            String value = pair.getValue();
            if (value != null) {

                res += URLEncoder.encode(pair.getName()) + "=" + URLEncoder.encode(value);
            } else {
                Log.e(TAG, "Invalid parameter " + pair.getName());
            }
        }

        return mUrlPath + "?" + res;
    }

    public JSONObject sendRequest(List<NameValuePair> parameters) throws IOException, JSONException, HttpException {
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
