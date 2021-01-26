package com.appmetr.android;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;

import com.appmetr.android.internal.ContextProxy;
import com.appmetr.android.internal.LibraryPreferences;
import com.appmetr.android.internal.UploadCacheTask;

import java.util.ArrayList;

/**
 * Copyright (c) 2019 AppMetr.
 * All rights reserved.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public final class UploadJobService extends JobService {
    private AsyncTask<String, Void, Boolean> uploadTask;

    @SuppressLint("StaticFieldLeak")
    @Override
    public boolean onStartJob(final JobParameters params) {
        if (params.getExtras().containsKey(UploadService.EXTRA_PARAMS_TOKEN)) {
            String token = params.getExtras().getString(UploadService.EXTRA_PARAMS_TOKEN);
            if (!TextUtils.isEmpty(token)) {
                uploadTask = new AsyncTask<String, Void, Boolean>() {

                    @Override
                    protected Boolean doInBackground(String... tokens) {
                        return uploadImpl(tokens[0]);
                    }

                    @Override
                    protected void onPostExecute(Boolean success) {
                        jobFinished(params, !success);
                        uploadTask = null;
                    }
                };
                uploadTask.execute(token);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (uploadTask != null) {
            uploadTask.cancel(false);
            return true;
        }
        return false;
    }

    private boolean uploadImpl(String token) {
        LibraryPreferences preferences = new LibraryPreferences(getBaseContext());
        ArrayList<String> fileList = preferences.getFileList();
        UploadCacheTask uploadCacheTask = new UploadCacheTask(new ContextProxy(getBaseContext()), token);
        for(String fileName : fileList) {
            uploadCacheTask.uploadFile(fileName);
            // only if network error, we retry later
            if(uploadCacheTask.getStatus() == UploadCacheTask.UploadStatus.NetworkError)
                return false;
        }
        return true;
    }
}
