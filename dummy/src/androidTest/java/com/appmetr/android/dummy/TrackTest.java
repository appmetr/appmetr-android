/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.dummy;

import com.appmetr.android.dummy.utils.AppMetrDirtyHack;
import com.appmetr.android.dummy.utils.BaseAppMetrDummyActivityTest;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

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

    public void testSetTimestamp() throws Exception {
        AppMetrDirtyHack testLibrary = new AppMetrDirtyHack(getActivity());
        testLibrary.initialize("TestThisLibrary");
        ArrayList<JSONObject> eventList = testLibrary.getDirtyEventList();

        // test Date as timestamp
        Date testDate1 = new GregorianCalendar(2017, Calendar.FEBRUARY, 17).getTime();
        JSONObject propertiesLong = new JSONObject().put("timestamp", testDate1.getTime());
        AppMetrDirtyHack.trackEvent("customTimestamp1", propertiesLong);
        JSONObject resultsLong = eventList.get(eventList.size() - 1);
        assertEquals("Invalid action", resultsLong.getString("action"), "trackEvent");
        assertEquals("Invalid event name", resultsLong.getString("event"), "customTimestamp1");
        assertEquals("Invalid custom date", resultsLong.getLong("timestamp"), testDate1.getTime());

        // test Date as Date
        Date testDate2 = new GregorianCalendar(2018, Calendar.MARCH, 1).getTime();
        JSONObject propertiesDate = new JSONObject().put("timestamp", testDate2);
        AppMetrDirtyHack.trackLevel(5, propertiesDate);
        JSONObject resultsDate = eventList.get(eventList.size() - 1);
        assertEquals("Invalid action", resultsDate.getString("action"), "trackLevel");
        assertEquals("Invalid level", resultsDate.getInt("level"), 5);
        assertEquals("Invalid custom date", resultsDate.getLong("timestamp"), testDate2.getTime());

        // test Date as wrong argument
        Date testDate3 = new GregorianCalendar(2018, Calendar.APRIL, 10).getTime();
        JSONObject propertiesWrong = new JSONObject().put("timestamp", new SimpleDateFormat("yyyy.MM.dd hh:mm").format(testDate3));
        AppMetrDirtyHack.trackEvent("customTimestamp3", propertiesWrong);
        JSONObject resultsWrong = eventList.get(eventList.size() - 1);
        assertEquals("Invalid action", resultsWrong.getString("action"), "trackEvent");
        assertEquals("Invalid event name", resultsWrong.getString("event"), "customTimestamp3");
        assertTrue("Invalid custom date", System.currentTimeMillis() - resultsWrong.getLong("timestamp") < 50);
    }
}