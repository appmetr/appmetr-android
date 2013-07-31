package com.appmetr.android.demo.tabs;

import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.*;
import com.appmetr.android.AppMetr;
import com.appmetr.android.demo.R;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CustomTabActivity extends AbstractTabActivity {
    private final static String TAG = "DemoActivity";

    private ListView listViewProperties;
    private ArrayAdapter<String> propertyAdapter;

    private Spinner spinnerEventTypes;
    private String[] eventTypes = new String[]{"trackEvent", "attachProperties"};

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom);

        listViewProperties = (ListView) findViewById(R.id.listViewProperties);
        propertyAdapter = new ArrayAdapter<String>(this, R.layout.property_list);
        listViewProperties.setAdapter(propertyAdapter);

        spinnerEventTypes = (Spinner) findViewById(R.id.spinnerEventType);
        ArrayAdapter<String> eventTypeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, eventTypes);
        spinnerEventTypes.setAdapter(eventTypeAdapter);

        initializeListeners();
    }

    private void initializeListeners() {
        spinnerEventTypes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                EditText editEventName = (EditText) findViewById(R.id.editEventName);
                if ("trackEvent".equals(eventTypes[i])) {
                    editEventName.setVisibility(View.VISIBLE);
                } else {
                    editEventName.setVisibility(View.INVISIBLE);
                }
            }

            @Override public void onNothingSelected(AdapterView<?> adapterView) {
                EditText editEventName = (EditText) findViewById(R.id.editEventName);
                editEventName.setVisibility(View.INVISIBLE);
            }
        });

        Button buttonAddProperty = (Button) findViewById(R.id.buttonAddProperty);
        buttonAddProperty.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                EditText editPropertyName = (EditText) findViewById(R.id.editPropertyName);
                EditText editPropertyValue = (EditText) findViewById(R.id.editPropertyValue);

                String propertyName = editPropertyName.getText().toString();
                String propertyValue = editPropertyValue.getText().toString();

                if (propertyName.length() > 0 && propertyValue.length() > 0) {
                    propertyAdapter.add(propertyName + ":" + propertyValue);

                    editPropertyName.setText("");
                    editPropertyValue.setText("");
                }
            }
        });

        Button buttonDeleteProperty = (Button) findViewById(R.id.buttonDeleteProperty);
        buttonDeleteProperty.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                SparseBooleanArray chosen = listViewProperties.getCheckedItemPositions();
                List<String> itemsToDelete = new ArrayList<String>();
                for (int i = 0; i < chosen.size(); i++) {
                    if (chosen.valueAt(i)) {
                        itemsToDelete.add(listViewProperties.getItemAtPosition(chosen.keyAt(i)).toString());
                        listViewProperties.setItemChecked(chosen.keyAt(i), false);
                    }
                }

                for (String item : itemsToDelete) {
                    propertyAdapter.remove(item);
                }

            }
        });

        Button buttonFlush = (Button) findViewById(R.id.buttonFlush);
        buttonFlush.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                AppMetr.flush();
                logMessage("Events flushed");
            }
        });

        Button buttonTrack = (Button) findViewById(R.id.buttonTrack);
        buttonTrack.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                String actionType = spinnerEventTypes.getSelectedItem().toString();

                if ("trackEvent".equals(actionType)) {
                    String eventName = ((EditText) findViewById(R.id.editEventName)).getText().toString();

                    if (eventName.length() > 0) {
                        AppMetr.trackEvent(eventName, getProperties());
                        logMessage(String.format("Event \"%1$s\"tracked", eventName));
                    }
                } else if ("attachProperties".equals(actionType)) {
                    JSONObject properties = getProperties();

                    if (properties != null) {
                        AppMetr.attachProperties(getProperties());
                        logMessage("Properties attached");
                    }
                }
            }
        });
    }

    private JSONObject getProperties() {
        JSONObject result = new JSONObject();
        for (int i = 0; i < propertyAdapter.getCount(); i++) {
            String item = propertyAdapter.getItem(i);
            int splitIndex;
            if ((splitIndex = item.indexOf(":")) > 0) {
                try {
                    result.put(item.substring(0, splitIndex), item.substring(splitIndex + 1));
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
        return result.length() > 0 ? result : null;
    }
}
