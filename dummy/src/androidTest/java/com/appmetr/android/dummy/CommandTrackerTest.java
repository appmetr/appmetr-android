/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.dummy;

import android.app.Activity;
import android.content.SharedPreferences;
import com.appmetr.android.dummy.utils.AppMetrDirtyHack;
import com.appmetr.android.dummy.utils.BaseAppMetrDummyActivityTest;

import java.util.zip.DataFormatException;

public class CommandTrackerTest extends BaseAppMetrDummyActivityTest {
    private String mSavedCommandList;

    protected void setUp() throws Exception {
        super.setUp();

        // save command list
        SharedPreferences preferences = getActivity().getSharedPreferences("AppMetr", Activity.MODE_PRIVATE);
        mSavedCommandList = preferences.getString("AppMetr-Processed-Command-List", "");

        // clear command list
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("AppMetr-Processed-Command-List", "");
        editor.apply();
    }

    protected void tearDown() throws Exception {
        // restore command list
        SharedPreferences preferences = getActivity().getSharedPreferences("AppMetr", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("AppMetr-Processed-Command-List", mSavedCommandList);
        editor.apply();

        super.tearDown();
    }
}
