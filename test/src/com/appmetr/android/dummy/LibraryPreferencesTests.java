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
        editor.commit();

        // create test instance
        mTestInstance = new LibraryPreferences(mPreferences);
    }

    protected void tearDown() throws Exception {
        // restore command list
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString("AppMetr-Processed-Command-List", mSavedCommandList);
        editor.commit();

        super.tearDown();
    }

    public void testEmptyCommandList() {
        assertEquals("Command aready test-cmd-1 exist O_o", false, mTestInstance.hasCommandProcessd("test-cmd-1"));
    }

    public void testCommandList() {
        mTestInstance.setCommandProcessed("test-cmd-1");
        assertEquals("Command test-cmd-1 does not exist", true, mTestInstance.hasCommandProcessd("test-cmd-1"));

        LibraryPreferences instance1 = new LibraryPreferences(mPreferences);
        assertEquals("Command test-cmd-1 does not exist", true, instance1.hasCommandProcessd("test-cmd-1"));

        mTestInstance.setCommandProcessed("test-cmd-2");
        LibraryPreferences instance2 = new LibraryPreferences(mPreferences);
        assertEquals("Command test-cmd-2 does not exist", true, instance2.hasCommandProcessd("test-cmd-2"));
        assertEquals("Command test-cmd-2 already exist", false, instance1.hasCommandProcessd("test-cmd-2"));
    }
}
