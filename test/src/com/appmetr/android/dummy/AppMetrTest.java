/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.dummy;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Base64;
import com.appmetr.android.AppMetrListener;
import com.appmetr.android.dummy.utils.AppMetrDirtyHack;
import com.appmetr.android.dummy.utils.BaseAppMetrDummyActivityTest;
import com.appmetr.android.internal.command.data.RemoteCommandPacket;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.zip.DataFormatException;

public class AppMetrTest extends BaseAppMetrDummyActivityTest {
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

    public void notestSomething() throws Exception {
        AppMetrDirtyHack testLibrary;
        testLibrary = new AppMetrDirtyHack(getActivity());
        testLibrary.initialize("TestThisLibrary");

        assertTrue(testLibrary.webServiceTest());
        assertTrue(testLibrary.trackMethodsTest());
        assertTrue(testLibrary.eventListTimestampTest());
        assertTrue(testLibrary.eventListValidationCompleteTest());
        assertTrue(testLibrary.eventListValidationDateTest());

        assertTrue(testLibrary.savingLargePacketsTest());
    }

    public void testBatchCounter() throws DataFormatException {
        AppMetrDirtyHack testLibrary = createTestApi();

        int firstValue = testLibrary.getDirtyCurrentBatchID();
        int secondValue = testLibrary.getDirtyNextBatchID();
        assertEquals("Counter incrementation is wrong", firstValue, secondValue);

        int finalValue = testLibrary.getDirtyCurrentBatchID();
        assertEquals("Invalid counter value returned by object", finalValue - 1, secondValue);
    }

    public void testBatchSaver() throws DataFormatException {
        AppMetrDirtyHack testLibrary = createTestApi();
        int value = testLibrary.getDirtyNextBatchID() + 1;

        testLibrary = createTestApi();
        int restoredValue = testLibrary.getDirtyCurrentBatchID();
        assertEquals("Wrong batch id value saved/restored", value, restoredValue);
    }

    public void testFileCounter() throws DataFormatException {
        AppMetrDirtyHack testLibrary = createTestApi();

        int firstValue = testLibrary.getDirtyCurrentFileIndex();
        int secondValue = testLibrary.getDirtyNextFileIndex();
        assertEquals("File index incrementation is wrong", firstValue + 1, secondValue);

        int finalValue = testLibrary.getDirtyCurrentFileIndex();
        assertEquals("Invalid file index returned by object", finalValue, secondValue);
    }

    public void testFileIndexSaver() throws DataFormatException {
        AppMetrDirtyHack testLibrary = createTestApi();
        int value = testLibrary.getDirtyNextFileIndex();

        testLibrary = createTestApi();
        int restoredValue = testLibrary.getDirtyCurrentFileIndex();
        assertEquals("Wrong batch id value saved/restored", value, restoredValue);
    }

    public void testFileListBasicSaver() throws Exception {
        AppMetrDirtyHack testLibrary = createTestApi();
        ArrayList<String> fileList = testLibrary.getDirtyFileList();
        synchronized (fileList) {
            fileList.clear();
            fileList.add("TestFileName");
            testLibrary.dirtyFlushFileList();

            ArrayList<String> restoredList = testLibrary.dirtyRestoreFileList();
            assertEquals("Wrong file list size", 1, restoredList.size());
            if (restoredList.size() != 0) {
                assertEquals("Wrong file name saved to preferences", "TestFileName", restoredList.get(0));
            }

            fileList.clear();
            testLibrary.dirtyFlushFileList();
        }
    }

    public void testRestoreFileList() throws Exception {
        AppMetrDirtyHack testLibrary = createTestApi();

        int fileListSize = testLibrary.getDirtyFileList().size();
        AppMetrDirtyHack.trackEvent("test event");
        testLibrary.dirtyFlushDataImpl();
        AppMetrDirtyHack.onPause();

        testLibrary = createTestApi();

        assertEquals("Wrong file list restored.", fileListSize + 1, testLibrary.getDirtyFileList().size());
    }

    public void testNetwork() throws DataFormatException {
        AppMetrDirtyHack testLibrary = createTestApi();
        AppMetrDirtyHack.onPause();

        ArrayList<String> fileList = testLibrary.getDirtyFileList();
        fileList.clear();

        AppMetrDirtyHack.trackEvent("testevent");
        testLibrary.dirtyFlushDataImpl();
        AppMetrDirtyHack.onPause();

        assertEquals("Failed to upload batch file", 1, testLibrary.durtyUploadBatches());
    }

    public void testDeadLoacks() throws DataFormatException {
        AppMetrDirtyHack testLibrary = createTestApi();
        for (int i = 0; i < 100; i++) {
            testLibrary.dirtyFlushData();
            testLibrary.dirtyUploadCache();
        }
        testLibrary.dirtySleepLibrary();

        // fake assertion
        testLibrary = createTestApi();
        assertNotNull(testLibrary);
    }

    public void testGameState() throws DataFormatException {
        AppMetrDirtyHack testLibrary = createTestApi();
        AppMetrDirtyHack.onPause();

        ArrayList<String> fileList = testLibrary.getDirtyFileList();
        fileList.clear();

        AppMetrDirtyHack.trackGameState("testState", new JSONObject());
        testLibrary.dirtyFlushDataImpl();
        AppMetrDirtyHack.onPause();

        assertEquals("Failed to upload batch file", 1, testLibrary.durtyUploadBatches());
    }

    public void testBase64() {
        String data = "So?This 4, 5, 6, 7, 8, 9, z, {, |, } tests Base64 encoder. Show me: @, A,"
                + " B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z, [, \\, ], ^, _, `"
                + ", a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s.";
        String res = Base64.encodeToString(data.getBytes(), Base64.NO_WRAP | Base64.URL_SAFE);
        assertEquals("U28_VGhpcyA0LCA1LCA2LCA3LCA4LCA5LCB6LCB7LCB8LCB9IHRlc3RzIEJhc2U2NCBlbmNvZGVyLiBTaG93IG1l"
                + "OiBALCBBLCBCLCBDLCBELCBFLCBGLCBHLCBILCBJLCBKLCBLLCBMLCBNLCBOLCBPLCBQLCBRLCBSLCBTLCBULCBVLCBWL"
                + "CBXLCBYLCBZLCBaLCBbLCBcLCBdLCBeLCBfLCBgLCBhLCBiLCBjLCBkLCBlLCBmLCBnLCBoLCBpLCBqLCBrLCBsLCBtLC"
                + "BuLCBvLCBwLCBxLCByLCBzLg==", res);
    }

    public void testTrackInstallURL() throws DataFormatException {
        AppMetrDirtyHack testLibrary = createTestApi();
//		AppMetrDirtyHack.onPause();

        ArrayList<String> fileList = testLibrary.getDirtyFileList();
        fileList.clear();

        AppMetrDirtyHack.trackInstallURL("http://android.appmetr.com/unit/test");
        testLibrary.dirtyFlushDataImpl();
        AppMetrDirtyHack.onPause();

        assertEquals("Failed to track installation URL", 0, testLibrary.getDirtyFileList().size());
    }

    public void testRemoteCommandPacket() throws JSONException, DataFormatException {
        AppMetrDirtyHack testLibrary = createTestApi();

        Date now = new Date();
        JSONObject object = new JSONObject(
                "{\"status\":\"OK\", \"commands\":["
                        + "{\"commandId\":\"cmd20120824112718\",\"sendDate\":0, \"type\":\"promo.realMoneyPurchaseBonus\","
                        + "\"conditions\":{\"validTo\":"
                        + (now.getTime() + 100000)
                        + "},"
                        + "\"properties\":{\"prop1\":10, \"prop2\":[1,2,3], \"prop3\":true, \"prop4\" : {\"sub1\":1, \"sub2\":2}}},"
                        + "{\"commandId\":\"cmd30120824112718\",\"sendDate\":0, \"type\":\"promo.spentCurrencyDiscount\",\"conditions\": {\"validTo\":"
                        + (now.getTime() + 100000) + "}}]," + "\"isLastCommandsBatch\":true}");

        testLibrary.dirtyProcessPacket(new RemoteCommandPacket(object, null));

        assertEquals("Invalid numbers o commands", 2, testLibrary.dirtyGetCommandsManager().getNumCommands());
        assertEquals("Event list must be empty", 0, testLibrary.getDirtyEventList().size());
    }

    public void testSendQueryRemoteCommands() throws DataFormatException {
        AppMetrDirtyHack testLibrary = createTestApi();

        testLibrary.setListener(new AppMetrListener() {
            @Override
            public void executeCommand(JSONObject command) throws Throwable {
            }
        });

        testLibrary.dirtySentQueryRemoteCommandList();
        assertTrue("Command list is not empty", testLibrary.dirtyGetCommandsManager().getNumCommands() == 0);
    }
}
