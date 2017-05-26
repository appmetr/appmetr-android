package com.appmetr.android.dummy.utils;

import android.app.Activity;
import com.appmetr.android.AppMetr;
import com.appmetr.android.internal.Utils;
import com.appmetr.android.internal.command.CommandsManager;
import com.appmetr.android.internal.command.data.RemoteCommandPacket;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.DataFormatException;

public class AppMetrDirtyHack extends AppMetr {
    public static final String TEST_TOKEN = "demo_token";

    public AppMetrDirtyHack(Activity activity) throws DataFormatException {
        super(activity, null);
        msInstance = this;

        initialize(TEST_TOKEN);

        mCacheInterval = 32000;
        mUploadInterval = 32000;
        mMaxFileSize = 2000000;
    }

    public AppMetrDirtyHack(String tocken, Activity activity) throws DataFormatException {
        super(activity, null);

        msInstance = this;

        initialize(TEST_TOKEN);

        mCacheInterval = 32000;
        mUploadInterval = 32000;
        mMaxFileSize = 2000000;
    }

    public void initialize(String token) throws DataFormatException {
        super.initialize(token);
    }

    public static void dirtyDestroySingletonInstance() {
        msInstance = null;
    }

    public boolean webServiceTest() {
        boolean result = false;
//		WebServiceRequest request = new WebServiceRequest(LibraryPreferences.DEFAULT_SERVICE_ADDRESS);
//		result = request.sendRequest("something wrong");
//		result = !request.sendRequest("method=server%2Etrack&token=demo%23demo&userId=3133731337aa&batches=WwogICA...geoW%3D");
        return result;
    }

    public boolean trackMethodsTest() {
        JSONObject properties = new JSONObject();
        try {
            properties.put("test", "test");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        AppMetrDirtyHack.trackLevel(10, properties);
        AppMetrDirtyHack.trackEvent("firstEvent");
        boolean res = (mEventList.size() == 2);
        return res;
    }

    public boolean eventListTimestampTest() {
        synchronized (mEventList) {

            mEventList.clear();
            JSONObject properties = new JSONObject();
            try {
                properties.put("test", "test");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            AppMetr.trackLevel(1, properties);
            AppMetr.trackEvent("firstEvent");
            AppMetr.trackEvent("firstEvent");

            JSONObject obj = mEventList.get(0);
            try {
                JSONObject resProperties = (JSONObject) obj.get("properties");
                String res = (String) resProperties.get("test");
                if (res.compareTo("test") == 0)
                    return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean eventListValidationDateTest() {
        mEventList.clear();
        JSONObject properties = new JSONObject();

        try {
            properties.put("test", "test");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Date nowDate = new Date();
        long prevTime = nowDate.getTime();
        AppMetrDirtyHack.trackLevel(10, properties);

        JSONObject obj = mEventList.get(0);

        Date afterDate = new Date();
        long afterTime = afterDate.getTime();

        try {
            long testTime = obj.getLong("timestamp");
            if ((prevTime < testTime) && (testTime < afterTime))
                return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean eventListValidationCompleteTest() {
        JSONObject testObject = new JSONObject();
        boolean res = false;

        try {
            Utils.validatePayments(testObject);
        } catch (DataFormatException e) {
            e.printStackTrace();
            res = true;
        }

        try {
            testObject.put("psUserSpentCurrencyCode", "test");
            testObject.put("psUserSpentCurrencyAmount", "test");
            testObject.put("psReceivedCurrencyCode", "test");
            testObject.put("psReceivedCurrencyAmount", "test");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        boolean secondTestRes = false;
        try {
            Utils.validatePayments(testObject);
            secondTestRes = true;
        } catch (DataFormatException e) {
            e.printStackTrace();
        }

        return res && secondTestRes;
    }

    public boolean savingLargePacketsTest() {
        mEventList.clear();
        mPreferences._resetBatchID();
        String testText = "testingEventForSaveMaxSize";
        for (int i = 0; i < 1000; i++) {
            trackEvent(testText);
        }
        flushData();

        File testFile = new File("batch" + mPreferences.getCurrentBatchID());
        long testLength = testText.length() * 1000 + "1000".length() * 1000;

        return testFile.length() > testLength;

    }

    public ArrayList<JSONObject> getDirtyEventList() {
        return mEventList;
    }

    public int getDirtyNextBatchID() {
        return mPreferences.getNextBatchID();
    }

    public int getDirtyCurrentBatchID() {
        return mPreferences.getCurrentBatchID();
    }

    public int getDirtyNextFileIndex() {
        return mPreferences.getNextFileIndex();
    }

    public Integer getDirtyCurrentFileIndex() {
        return mPreferences.getCurrentFileIndex();
    }

    public ArrayList<String> getDirtyFileList() {
        return mFileList;
    }

    public void dirtyFlushFileList() {
        mPreferences.setFileList(mFileList);
    }

    public ArrayList<String> dirtyRestoreFileList() {
        return mPreferences.getFileList();
    }

    public void dirtyFlushDataImpl() {
        flushDataImpl();
    }

    public int durtyUploadBatches() {
        return uploadBatches();
    }

    public byte[] getDirtyFileContent(String fileName) throws IOException {
        return mContextProxy.getFileContent(fileName);
    }

    public void setDirtySizeLimitOfCache(int size) {
        mMaxFileSize = size;
    }

    public void dirtyCloseCurrentFileWritter() throws Exception {
        closeCurrentFileWritter();
    }

    public String getDirtyBatchData() throws Exception {
        ArrayList<JSONObject> copyEvent;
        synchronized (mEventList) {
            copyEvent = new ArrayList<JSONObject>(mEventList);
            mEventList.clear();
        }

        if (copyEvent.size() > 0) {
            return Utils.getEncodedString(copyEvent, mPreferences.getNextBatchID());
        }

        return null;
    }

    public void dirtyFlushData() {
        flushData();
    }

    public void dirtyUploadCache() {
        uploadCache();
    }

    public void dirtyUnloadLibrary() {
        unloadLibrary();
    }

    public void dirtySleepLibrary() {
        sleepLibrary();
    }

    public void dirtyProcessPacket(RemoteCommandPacket packet) {
        mCommandsManager.processPacket(packet);
    }

    public CommandsManager dirtyGetCommandsManager() {
        return mCommandsManager;
    }

    public void dirtySentQueryRemoteCommandList() {
        sentQueryRemoteCommandList();
    }
}