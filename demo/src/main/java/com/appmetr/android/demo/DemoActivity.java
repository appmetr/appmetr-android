package com.appmetr.android.demo;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TabHost;
import com.appmetr.android.AppMetr;
import com.appmetr.android.AppMetrListener;
import com.appmetr.android.demo.tabs.CasesTabActivity;
import com.appmetr.android.demo.tabs.CustomTabActivity;
import com.appmetr.android.demo.tabs.EventsTabActivity;
import com.appmetr.android.demo.tabs.InfoTabActivity;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.DataFormatException;

public class DemoActivity extends TabActivity {
    private final static String TAG = "DemoActivity";

    private final String token = "demo_token";

    private Map<String, Object> optionValues = new HashMap<String, Object>();
    private List<String> messages = new ArrayList<String>();

    private final Lock logLock = new ReentrantLock();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        try {
            initializeAppMetr();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        TabHost tabHost = getTabHost();

        TabHost.TabSpec eventsSpec = tabHost.newTabSpec("Events");
        eventsSpec.setIndicator(getString(R.string.event_tab_header));
        Intent eventsIntent = new Intent(this, EventsTabActivity.class);
        eventsSpec.setContent(eventsIntent);

        TabHost.TabSpec casesSpec = tabHost.newTabSpec("Cases");
        casesSpec.setIndicator(getString(R.string.cases_tab_header));
        Intent casesIntent = new Intent(this, CasesTabActivity.class);
        casesSpec.setContent(casesIntent);

        TabHost.TabSpec customSpec = tabHost.newTabSpec("Custom");
        customSpec.setIndicator(getString(R.string.custom_tab_header));
        Intent customIntent = new Intent(this, CustomTabActivity.class);
        customSpec.setContent(customIntent);

        TabHost.TabSpec infoSpec = tabHost.newTabSpec("Info");
        infoSpec.setIndicator(getString(R.string.info_tab_header));
        Intent infoIntent = new Intent(this, InfoTabActivity.class);
        infoSpec.setContent(infoIntent);

        tabHost.addTab(eventsSpec);
        tabHost.addTab(casesSpec);
        tabHost.addTab(customSpec);
        tabHost.addTab(infoSpec);
    }

    @Override protected void onResume() {
        super.onResume();
        AppMetr.onResume();
    }

    @Override protected void onPause() {
        super.onPause();
        AppMetr.onPause();
    }

    private void initializeAppMetr() throws DataFormatException {
        AppMetr.setup(token, this, getAppMetrListener());
    }

    private AppMetrListener getAppMetrListener() {
        return new AppMetrListener() {
            @Override public void executeCommand(JSONObject command) throws Throwable {
                String type = command.getString("type");
                String commandId = command.getString("commandId");
                JSONObject properties = command.getJSONObject("properties");

                logMessage(String.format("Receive command \"%1$s\"", type));

                if ("setOptions".equals(type)) {
                    JSONArray options = properties.has("options") ? properties.getJSONArray("options") : new JSONArray();

                    JSONArray resultOptions = new JSONArray();
                    for (int optionIndex = 0; optionIndex < options.length(); optionIndex++) {
                        JSONObject option = options.getJSONObject(optionIndex);

                        String key = option.keys().next().toString();   //Cause 1 array element has only 1 option
                        Object value = option.get(key);

                        optionValues.put(key, value);

                        JSONObject resultOption = new JSONObject();
                        resultOption.put("option", key);
                        resultOption.put("oldValue", value);
                        resultOption.put("requestedValue", value);
                        resultOption.put("currentValue", value);

                        resultOptions.put(resultOption);
                    }

                    AppMetr.trackOptions(commandId, resultOptions);
                }
            }
        };
    }

    public void logMessage(String message) {
        logLock.lock();

        messages.add(message);

        logLock.unlock();

        Intent intent = new Intent();
        intent.setAction("com.appmetr.android.demo.LOG_MESSAGE");
        sendBroadcast(intent);
    }

    public List<String> pullMessages() {
        List<String> pulledMessages = new ArrayList<String>();

        logLock.lock();

        pulledMessages.addAll(messages);
        messages.clear();

        logLock.unlock();

        return pulledMessages;
    }

    public String getToken() {
        return token;
    }
}
