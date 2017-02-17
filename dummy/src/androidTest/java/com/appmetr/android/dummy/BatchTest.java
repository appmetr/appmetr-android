/**
 * Copyright (c) 2013 AppMetr
 * All rights reserved.
 */
package com.appmetr.android.dummy;

import com.appmetr.android.dummy.utils.AppMetrDirtyHack;
import com.appmetr.android.dummy.utils.BaseAppMetrDummyActivityTest;

public class BatchTest extends BaseAppMetrDummyActivityTest {
    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testAmountOfBatchFiles() throws Exception {
        AppMetrDirtyHack testLibrary;
        testLibrary = new AppMetrDirtyHack(getActivity());
        testLibrary.initialize("TestThisLibrary");

        testLibrary.dirtyFlushDataImpl();
        testLibrary.dirtyCloseCurrentFileWritter();
        testLibrary.getDirtyFileList().clear();

        testLibrary.setDirtySizeLimitOfCache(300);

        AppMetrDirtyHack.trackEvent("testBatchFile1");
        testLibrary.dirtyFlushDataImpl();
        AppMetrDirtyHack.trackEvent("testBatchFile2");
        testLibrary.dirtyFlushDataImpl();
        AppMetrDirtyHack.trackEvent("testBatchFile3");
        testLibrary.dirtyFlushDataImpl();
        AppMetrDirtyHack.trackEvent("testBatchFile4");
        testLibrary.dirtyFlushDataImpl();
        AppMetrDirtyHack.trackEvent("testBatchFile5");
        testLibrary.dirtyFlushDataImpl();
        AppMetrDirtyHack.trackEvent("testBatchFile6");
        testLibrary.dirtyFlushDataImpl();

        assertEquals("Wrong file numbers", 1, testLibrary.getDirtyFileList().size());

        testLibrary.dirtyCloseCurrentFileWritter();
        assertEquals("Wrong file numbers", 2, testLibrary.getDirtyFileList().size());
    }

}
