/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.dummy;

import android.app.Activity;
import android.content.SharedPreferences;
import com.appmetr.android.dummy.utils.AppMetrDirtyHack;
import com.appmetr.android.dummy.utils.BaseAppMetrDummyActivityTest;
import com.appmetr.android.internal.command.CommandTracker;

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
        editor.commit();
    }

    protected void tearDown() throws Exception {
        // restore command list
        SharedPreferences preferences = getActivity().getSharedPreferences("AppMetr", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("AppMetr-Processed-Command-List", mSavedCommandList);
        editor.commit();

        super.tearDown();
    }

    public void testTrackCommand() throws DataFormatException {
        AppMetrDirtyHack testLibrary = createTestApi();

        CommandTracker.trackCommand("test-1");
        assertEquals("Failed to add trackCommand(success)", 1, testLibrary.getDirtyEventList().size());

        CommandTracker.trackCommandSkip("test-2", "unit-test");
        assertEquals("Failed to add trackCommand(skip)", 2, testLibrary.getDirtyEventList().size());

        Throwable throwable = new Throwable("unit-test");
        throwable.fillInStackTrace();

        CommandTracker.trackCommandFail("test-3", throwable);
        assertEquals("Failed to add trackCommand(fail)", 3, testLibrary.getDirtyEventList().size());
    }

    public void testTrackCommandBatch() throws DataFormatException {
        AppMetrDirtyHack testLibrary = createTestApi();

        CommandTracker.trackCommandBatch("test-batch-1", "unit-test", "igrone, just init test");
        assertEquals("Failed to add testTrackCommandBatch", 1, testLibrary.getDirtyEventList().size());
    }
}
