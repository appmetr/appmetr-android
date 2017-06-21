/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.dummy;

import android.app.Activity;
import android.content.SharedPreferences;
import com.appmetr.android.dummy.utils.BaseAppMetrDummyActivityTest;
import com.appmetr.android.internal.LibraryPreferences;

public class LibraryPreferencesTests extends BaseAppMetrDummyActivityTest {
    private String mSavedCommandList;
    private SharedPreferences mPreferences;
    private LibraryPreferences mTestInstance;

    protected void setUp() throws Exception {
        super.setUp();

        mPreferences = getActivity().getSharedPreferences("AppMetr", Activity.MODE_PRIVATE);

        // save command list
        mSavedCommandList = mPreferences.getString("AppMetr-Processed-Command-List", "");

        // clear command list
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString("AppMetr-Processed-Command-List", "");
        editor.apply();

        // create test instance
        mTestInstance = new LibraryPreferences(mPreferences);
    }

    protected void tearDown() throws Exception {
        // restore command list
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString("AppMetr-Processed-Command-List", mSavedCommandList);
        editor.apply();

        super.tearDown();
    }
}
