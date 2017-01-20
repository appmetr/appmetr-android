package com.appmetr.android.dummy.utils;

import android.test.ActivityInstrumentationTestCase2;
import com.appmetr.android.dummy.AppMetrDummyActivity;

import java.util.zip.DataFormatException;

public class BaseAppMetrDummyActivityTest extends ActivityInstrumentationTestCase2<AppMetrDummyActivity> {

    public BaseAppMetrDummyActivityTest() {
        super("com.appmetr.android.dummy", AppMetrDummyActivity.class);
    }

    public AppMetrDirtyHack createTestApi() throws DataFormatException {
        return new AppMetrDirtyHack(getActivity());
    }

    public AppMetrDirtyHack createTestApi(String token) throws DataFormatException {
        return new AppMetrDirtyHack(token, getActivity());
    }

}
