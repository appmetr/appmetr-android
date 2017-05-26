/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.dummy;

import com.appmetr.android.dummy.utils.AppMetrDirtyHack;
import com.appmetr.android.dummy.utils.BaseAppMetrDummyActivityTest;
import org.json.JSONObject;

import java.util.ArrayList;

public class TrackTest extends BaseAppMetrDummyActivityTest {
    final static double EPSILON = 0.000001;

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    protected JSONObject anyProperties() throws Exception {
        return new JSONObject().put("prop-key", "prop-value");
    }

    protected JSONObject anyPayment() throws Exception {
        JSONObject paymentProperties = new JSONObject();
        paymentProperties.put("processor", "mm");
        paymentProperties.put("psUserSpentCurrencyCode", "MAILIKI");
        paymentProperties.put("psUserSpentCurrencyAmount", 10);
        paymentProperties.put("psReceivedCurrencyCode", "MAILIKI");
        paymentProperties.put("psReceivedCurrencyAmount", 10);
        paymentProperties.put("appCurrencyCode", "Totem");
        paymentProperties.put("appCurrencyAmount", 2);

        return paymentProperties;
    }

    protected Boolean equalsPrecision(float value1, float value2) throws Exception {
        if (value1 == value2) {
            return true;
        }

        return (Math.abs(value1 - value2) < EPSILON);
    }

    public void testAttachProperties() throws Exception {
        AppMetrDirtyHack testLibrary;
        testLibrary = new AppMetrDirtyHack(getActivity());
        testLibrary.initialize("TestThisLibrary");
        ArrayList<JSONObject> eventList = testLibrary.getDirtyEventList();

        AppMetrDirtyHack.attachProperties(this.anyProperties());
        JSONObject event = eventList.get(eventList.size() - 1);

        String actionName = event.getString("action");
        assertNotNull("Invalid action", actionName.compareTo("attachproperties"));
        assertNotNull("Missing timestamp", event.get("timestamp"));

        JSONObject propertiesObject = (JSONObject) event.get("properties");
        String propertyValue = (String) propertiesObject.get("prop-key");

        assertNotNull("Invalid property value", propertyValue.compareTo("prop-value"));
    }

    public void testTrackSession() throws Exception {
        AppMetrDirtyHack testLibrary;
        testLibrary = new AppMetrDirtyHack(getActivity());
        testLibrary.initialize("TestThisLibrary");
        ArrayList<JSONObject> eventList = testLibrary.getDirtyEventList();

        // test 1
        AppMetrDirtyHack.trackSession();
        JSONObject sessionOne = eventList.get(eventList.size() - 1);
        String actionName = sessionOne.getString("action");
        assertNotNull("Invalid action", actionName.compareTo("trackSession"));
        assertNotNull("Missing timestamp", sessionOne.get("timestamp"));

        // test 2
        AppMetrDirtyHack.trackSession(this.anyProperties());
        JSONObject sessionTwo = eventList.get(eventList.size() - 1);

        actionName = sessionTwo.getString("action");
        assertNotNull("Invalid action", actionName.compareTo("trackSession"));
        assertNotNull("Missing timestamp", sessionOne.get("timestamp"));

        JSONObject propertiesObject = (JSONObject) sessionTwo.get("properties");
        String propertyValue = (String) propertiesObject.get("prop-key");
        assertNotNull("Invalid property value", propertyValue.compareTo("prop-value"));
    }

    public void testTrackLevel() throws Exception {
        AppMetrDirtyHack testLibrary;
        testLibrary = new AppMetrDirtyHack(getActivity());
        testLibrary.initialize("TestThisLibrary");
        ArrayList<JSONObject> eventList = testLibrary.getDirtyEventList();

        // test 1
        int level = 81;
        AppMetrDirtyHack.trackLevel(level);
        JSONObject actionOne = eventList.get(eventList.size() - 1);
        String actionName = actionOne.getString("action");

        assertNotNull("Invalid action", actionName.compareTo("trackLevel"));
        assertNotNull("Missing timestamp", actionOne.get("timestamp"));

        assertTrue("Invalid level", actionOne.getInt("level") == level);

        // test 2
        level = 42;
        AppMetrDirtyHack.trackLevel(level, this.anyProperties());
        JSONObject actionTwo = eventList.get(eventList.size() - 1);

        actionName = actionTwo.getString("action");
        assertNotNull("Invalid action", actionName.compareTo("trackLevel"));
        assertNotNull("Missing timestamp", actionTwo.get("timestamp"));

        assertTrue("Invalid level", actionTwo.getInt("level") == level);

        JSONObject properties = (JSONObject) actionTwo.get("properties");
        String propertyValue = properties.getString("prop-key");
        assertNotNull("Invalid property value", propertyValue.compareTo("prop-value"));
    }

    public void testTrackEvent() throws Exception {
        AppMetrDirtyHack testLibrary;
        testLibrary = new AppMetrDirtyHack(getActivity());
        testLibrary.initialize("TestThisLibrary");
        ArrayList<JSONObject> eventList = testLibrary.getDirtyEventList();

        // test 1
        AppMetrDirtyHack.trackEvent("event1");
        JSONObject actionOne = eventList.get(eventList.size() - 1);
        String actionName = actionOne.getString("action");
        String eventName = actionOne.getString("event");
        float value = actionOne.has("value") ? Float.parseFloat(actionOne.getString("value")) : 1.0f;

        assertNotNull("Invalid action", actionName.compareTo("trackEvent"));
        assertNotNull("Missing timestamp", actionOne.get("timestamp"));
        assertNotNull("Invalid event name", eventName.compareTo("event1"));
        assertTrue("Invalid value of action", this.equalsPrecision(1.0f, value));

        // test 2
        AppMetrDirtyHack.trackEvent("event2");

        JSONObject actionTwo = eventList.get(eventList.size() - 1);
        actionName = actionTwo.getString("action");
        eventName = actionTwo.getString("event");

        assertNotNull("Invalid action", actionName.compareTo("trackEvent"));
        assertNotNull("Missing timestamp", actionTwo.get("timestamp"));
        assertNotNull("Invalid event name", eventName.compareTo("event2"));

        // test 3
        AppMetrDirtyHack.trackEvent("event3", this.anyProperties());

        JSONObject actionThree = eventList.get(eventList.size() - 1);
        actionName = actionThree.getString("action");
        eventName = actionThree.getString("event");

        assertNotNull("Invalid action", actionName.compareTo("trackEvent"));
        assertNotNull("Missing timestamp", actionThree.get("timestamp"));
        assertNotNull("Invalid event name", eventName.compareTo("event2"));

        JSONObject properties = (JSONObject) actionThree.get("properties");
        String propertyValue = properties.getString("prop-key");
        assertNotNull("Invalid property value", propertyValue.compareTo("prop-value"));
    }

    public void testTrackPayment() throws Exception {
        AppMetrDirtyHack testLibrary;
        testLibrary = new AppMetrDirtyHack(getActivity());
        testLibrary.initialize("TestThisLibrary");
        ArrayList<JSONObject> eventList = testLibrary.getDirtyEventList();

        // test 1
        AppMetrDirtyHack.trackPayment(this.anyPayment(), this.anyProperties());
        JSONObject actionOne = eventList.get(eventList.size() - 1);
        String actionName = actionOne.getString("action");
        String processor = actionOne.getString("processor");

        assertNotNull("Invalid action", actionName.compareTo("trackPayment"));
        assertNotNull("Missing timestamp", actionOne.get("timestamp"));
        assertNotNull("Invalid payment", processor.compareTo("mm"));

        // test 2
        JSONObject actionTwo = eventList.get(eventList.size() - 1);
        actionName = actionTwo.getString("action");
        processor = actionTwo.getString("processor");

        assertNotNull("Invalid action", actionName.compareTo("trackPayment"));
        assertNotNull("Missing timestamp", actionTwo.get("timestamp"));
        assertNotNull("Invalid payment", processor.compareTo("mm"));

        JSONObject properties = (JSONObject) actionTwo.get("properties");
        String propertyValue = properties.getString("prop-key");
        assertNotNull("Invalid property value", propertyValue.compareTo("prop-value"));
    }
}