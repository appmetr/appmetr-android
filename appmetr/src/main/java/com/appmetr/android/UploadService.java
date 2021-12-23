package com.appmetr.android;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.appmetr.android.internal.ContextProxy;
import com.appmetr.android.internal.LibraryPreferences;
import com.appmetr.android.internal.UploadCacheTask;

import java.util.ArrayList;

/**
 * Copyright (c) 2019 AppMetr.
 * All rights reserved.
 */
public class UploadService extends IntentService {
    public static final String ACTION_APPMETR_UPLOAD = "com.appmetr.actions.ACTION_UPLOAD";
    public static final String EXTRA_PARAMS_TOKEN = "appmetrToken";
    public static final String EXTRA_PARAMS_MACADDRESS = "appmetrMacAddress";

    public UploadService() {
        super("AppmetrUploadService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            if (ACTION_APPMETR_UPLOAD.equals(intent.getAction())) {
                final String token = intent.getStringExtra(EXTRA_PARAMS_TOKEN);
                final String macAddress = intent.getStringExtra(EXTRA_PARAMS_MACADDRESS);
                if (!TextUtils.isEmpty(token)) {
                    executeWithWakeLock(new Runnable() {
                        @Override
                        public void run() {
                            uploadImpl(token, macAddress);
                        }
                    });
                }
            }
        }
    }

    private void uploadImpl(String token, String macAddress) {
        LibraryPreferences preferences = new LibraryPreferences(getBaseContext());
        ArrayList<String> fileList = preferences.getFileList();
        UploadCacheTask uploadCacheTask = new UploadCacheTask(new ContextProxy(getBaseContext()), token, macAddress);
        uploadCacheTask.upload(fileList);
    }

    private void executeWithWakeLock(Runnable runnable) {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            PowerManager.WakeLock wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getCanonicalName());
            try {
                wakelock.acquire(180000);
                runnable.run();
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                try {
                    if(wakelock != null && wakelock.isHeld())
                        wakelock.release();
                } catch(Throwable t) {
                    t.printStackTrace();
                }
            }
        } else { // this is very strange situation
            runnable.run();
        }
    }
}
