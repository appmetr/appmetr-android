package com.appmetr.android.demo.tabs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import com.appmetr.android.AppMetr;
import com.appmetr.android.demo.R;

public class InfoTabActivity extends AbstractTabActivity {
    private static final String TAG = "TestStub";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info);

        String customUrl = "";
        try {
            ApplicationInfo appInfo = this.getPackageManager().getApplicationInfo(this.getPackageName(), PackageManager.GET_META_DATA);

            if (appInfo != null && appInfo.metaData != null) {
                if (appInfo.metaData.containsKey("appmetrUrl")) {
                    customUrl = appInfo.metaData.getString("appmetrUrl");
                }
            }
        } catch (final Throwable t) {
            Log.e(TAG, "Failed to read meta-data from manifest", t);
        }

        String separator = getString(R.string.info_tab_prop_separator);
        ((TextView) findViewById(R.id.userIdLabel)).setText(getString(R.string.info_tab_user_id) + separator + AppMetr.getInstanceIdentifier());
        ((TextView) findViewById(R.id.urlLabel)).setText(getString(R.string.info_tab_url) + separator + customUrl);
        ((TextView) findViewById(R.id.tokenLabel)).setText(getString(R.string.info_tab_token) + separator + getParentActivity().getToken());
        loadDeviceKey();
    }

    @SuppressLint("StaticFieldLeak")
    private void loadDeviceKey() {
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground (Void...voids){
                return AppMetr.getInstance().getDeviceKey();
            }

            @Override
            protected void onPostExecute (String deviceKey){
                ((TextView) findViewById(R.id.deviceKeyLabel)).setText(deviceKey == null ? "" : deviceKey);
            }
        }.execute();
    }
}
