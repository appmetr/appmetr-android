package com.appmetr.android.demo.tabs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.appmetr.android.AppMetr;
import com.appmetr.android.demo.R;

import java.util.List;

public class EventsTabActivity extends AbstractTabActivity {
    private static final String TAG = "TestStub";

    private EditText editEventName;
    private TextView textViewLog;

    private LogListener logListener = null;
    private boolean logListenerRegistered = false;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.events);

        logListener = new LogListener();

        editEventName = (EditText) findViewById(R.id.editEventName);
        textViewLog = (TextView) findViewById(R.id.textViewLog);

        initializeListeners();
    }

    @Override protected void onPause() {
        super.onPause();

        if (logListenerRegistered) {
            unregisterReceiver(logListener);
            logListenerRegistered = false;
        }
    }

    @Override protected void onResume() {
        super.onResume();

        pullMessages();

        if (!logListenerRegistered) {
            registerReceiver(logListener, new IntentFilter("com.appmetr.android.demo.LOG_MESSAGE"));
            logListenerRegistered = true;
        }
    }

    private void initializeListeners() {
        textViewLog.setText("");

        Button buttonFlush = (Button) findViewById(R.id.buttonFlush);
        buttonFlush.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                AppMetr.flush();
                logMessage("Events flushed");
            }
        });

        Button buttonPull = (Button) findViewById(R.id.buttonPull);
        buttonPull.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                AppMetr.pullCommands();
                logMessage("Commands pulled");
            }
        });

        Button buttonTrackSession = (Button) findViewById(R.id.buttonTrackSession);
        buttonTrackSession.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                AppMetr.trackSession();
                logMessage("Session tracked");
            }
        });

        Button buttonTrackEvent = (Button) findViewById(R.id.buttonTrack);
        buttonTrackEvent.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                String eventName = editEventName.getText().toString();

                if (eventName.length() > 0) {
                    AppMetr.trackEvent(eventName);
                    logMessage(String.format("Event \"%1$s\" tracked", eventName));
                }
            }
        });

        Button buttonAttachProperties = (Button) findViewById(R.id.buttonAttach);
        buttonAttachProperties.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppMetr.attachProperties();
                logMessage("Properties attached");
            }
        });

        Button buttonCls = (Button) findViewById(R.id.buttonClear);
        buttonCls.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                textViewLog.setText("");
            }
        });
    }

    @Override
    protected void logMessage(String message) {
        textViewLog.setText(textViewLog.getText() + message + "\n");
        Log.d(TAG, message);
    }

    protected void pullMessages() {
        List<String> messages = getParentActivity().pullMessages();
        for (String message : messages) {
            logMessage(message);
        }
    }

    private class LogListener extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.appmetr.android.demo.LOG_MESSAGE")) {
                pullMessages();
            }
        }
    }
}
