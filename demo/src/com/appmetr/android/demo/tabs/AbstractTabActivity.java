package com.appmetr.android.demo.tabs;

import android.app.Activity;
import com.appmetr.android.demo.DemoActivity;

public abstract class AbstractTabActivity extends Activity {
    protected DemoActivity getParentActivity() {
        return (DemoActivity) this.getParent();
    }

    protected void logMessage(String message) {
        getParentActivity().logMessage(message);
    }
}
