/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.dummy;

import com.appmetr.android.dummy.utils.AppMetrDirtyHack;
import com.appmetr.android.dummy.utils.BaseAppMetrDummyActivityTest;
import org.json.JSONObject;

public class AcceptanceTest extends BaseAppMetrDummyActivityTest {
    public void testShortTestWithoutPayment() throws Exception {
        AppMetrDirtyHack testLibrary = createTestApi();
        AppMetrDirtyHack.onPause();
        testLibrary.getDirtyFileList().clear();

        AppMetrDirtyHack.attachProperties(new JSONObject().put("abGroup", "red").put("$adRef", "test"));
        AppMetrDirtyHack.trackSession(new JSONObject().put("$level", 80));
        AppMetrDirtyHack.trackEvent("test/test");

        testLibrary.dirtyFlushDataImpl();
        AppMetrDirtyHack.onPause();

        AppMetrDirtyHack.attachProperties(new JSONObject().put("abGroup", "red").put("$adRef", "test"));
        AppMetrDirtyHack.trackSession();
        AppMetrDirtyHack.trackEvent("test/test2");

        testLibrary.dirtyFlushDataImpl();
        AppMetrDirtyHack.onPause();

        assertEquals("Failed to upload 2 batch files", 2, testLibrary.durtyUploadBatches());
    }

    public void testHundredEvents() throws Exception {
        AppMetrDirtyHack testLibrary = createTestApi();
        for (int i = 0; i < 100; i++) {
            AppMetrDirtyHack.trackEvent("test");
        }
        assertEquals("Failed to create 100 events", 100, testLibrary.getDirtyEventList().size());
    }

    public void testThreeFilesWithAllocation() throws Exception // allocation 2 events - 1 event - 1 event
    {
        AppMetrDirtyHack testLibrary = createTestApi();
        testLibrary.setDirtySizeLimitOfCache(12);
        int beginTestFilesCount = testLibrary.getDirtyFileList().size();

        AppMetrDirtyHack.trackEvent("First test event.");
        AppMetrDirtyHack.trackEvent("Second test event.");

        testLibrary.dirtyFlushDataImpl();
        testLibrary.dirtyCloseCurrentFileWritter();

        AppMetrDirtyHack.trackEvent("Third test event with length > S.");
        testLibrary.dirtyFlushDataImpl();
        testLibrary.dirtyCloseCurrentFileWritter();

        AppMetrDirtyHack.trackEvent("Fourth test event");
        testLibrary.dirtyFlushDataImpl();
        testLibrary.dirtyCloseCurrentFileWritter();

        int endTestFilesCount = testLibrary.getDirtyFileList().size();
        assertEquals("Failed to create three files", 3, (endTestFilesCount - beginTestFilesCount));
    }

    public void _testFourFiles() throws Exception {
        AppMetrDirtyHack testLibrary = createTestApi();
        testLibrary.setDirtySizeLimitOfCache(133 * 3);

        int beginTestFilesCount = testLibrary.getDirtyFileList().size() + 1;

        for (int i = 0; i < 10; i++) {
            AppMetrDirtyHack.trackEvent("test");
            testLibrary.dirtyFlushDataImpl();
        }
        AppMetrDirtyHack.onPause();

        int endTestFilesCount = testLibrary.getDirtyFileList().size() + 1;

        String content = "";
        for (int i = beginTestFilesCount; i < endTestFilesCount; i++) {
            content += testLibrary.getDirtyFileContent("batch" + i);
        }
        int lastIndex = 0;
        int count = 0;
        lastIndex = content.indexOf("$", 0);
        while (lastIndex != -1) {
            lastIndex = content.indexOf("$", lastIndex + 1);
            count++;
        }
        boolean result = (4 == (endTestFilesCount - beginTestFilesCount)) && (count == 6); // 4 - number of files; 6 - number of "$" in this files
        assertTrue("Failed to flush 10 events", result);
    }

    public void notestFullTestWithPayment() throws Exception {
        AppMetrDirtyHack testLibrary = createTestApi();
        AppMetrDirtyHack.onPause();
        testLibrary.getDirtyFileList().clear();

        AppMetrDirtyHack.attachProperties(new JSONObject().put("abGroup", "red").put("adRef", "test"));
        AppMetrDirtyHack.trackSession(new JSONObject().put("level", 80));
        AppMetrDirtyHack.trackEvent("test/test");

        AppMetrDirtyHack.trackPayment(new JSONObject().put("processor", "mm")
                .put("psUserSpentCurrencyCode", "MAILIKI").put("psUserSpentCurrencyAmount", 10)
                .put("psReceivedCurrencyCode", "MAILIKI").put("psReceivedCurrencyAmount", 10)
                .put("appCurrencyCode", "Totem").put("appCurrencyAmount", 2));

        testLibrary.dirtyFlushDataImpl();
        AppMetrDirtyHack.onPause();

        AppMetrDirtyHack.attachProperties(new JSONObject().put("abGroup", "red").put("adRef", "test"));
        AppMetrDirtyHack.trackSession();
        AppMetrDirtyHack.trackEvent("test/test2");

        AppMetrDirtyHack.trackPayment(new JSONObject().put("processor", "mm")
                .put("psUserSpentCurrencyCode", "MAILIKI").put("psUserSpentCurrencyAmount", 40)
                .put("psReceivedCurrencyCode", "MAILIKI").put("psReceivedCurrencyAmount", 40)
                .put("appCurrencyCode", "Totem").put("appCurrencyAmount", 8));

        testLibrary.dirtyFlushDataImpl();
        AppMetrDirtyHack.onPause();

        assertEquals("Failed to upload 2 batch files", 2, testLibrary.durtyUploadBatches());
    }

}
