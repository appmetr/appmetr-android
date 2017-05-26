/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.dummy;

import com.appmetr.android.dummy.utils.BaseAppMetrDummyActivityTest;
import com.appmetr.android.internal.command.data.RemoteCommand;
import org.json.JSONException;
import org.json.JSONObject;

public class RemoteCommandTest extends BaseAppMetrDummyActivityTest {
    public void testFirstObject() throws JSONException {
        JSONObject object = new JSONObject("{\"commandId\":\"cmd20120824112718\", \"sendDate\":0, \"type\" : \"promo.realMoneyPurchaseBonus\", "
                + "\"status\":\"sent\",\"conditions\": { \"validTo\":10}, "
                + "\"properties\": { \"prop1\" : 10, \"prop4\" : {\"sub1\":1, \"sub2\":2}}}");

        RemoteCommand command = new RemoteCommand(object);

        assertEquals("Invalid commandID", "cmd20120824112718", command.uniqueIdentifier);
        assertEquals("Invalid type", "promo.realMoneyPurchaseBonus", command.type);
//		assertEquals("Invalid validTo", (new Date().getTime() + 10), command.validTo.getTime());
        assertNotNull("Invalid properties", command.properties);

        // test properties
        assertTrue("Invalid prop1", command.properties.getJSONObject("properties").has("prop1"));
        assertTrue("Invalid prop4", command.properties.getJSONObject("properties").has("prop4"));
    }

    public void testSecondObject() throws JSONException {
        JSONObject object = new JSONObject("{\"commandId\":\"cmd30120824112718\",\"sendDate\":0, \"type\":\"promo.spentCurrencyDiscount\", "
                + "\"status\":\"sent\",\"conditions\": {\"validTo\":20}}");

        RemoteCommand command = new RemoteCommand(object);

        assertEquals("Invalid commandID", "cmd30120824112718", command.uniqueIdentifier);
        assertEquals("Invalid type", "promo.spentCurrencyDiscount", command.type);
//		assertEquals("Invalid validTo", (new Date().getTime() + 20), command.validTo.getTime());
        assertNull("Invalid properties", command.properties.optJSONObject("properties"));
    }

    public void testInvalidType() throws JSONException {
        JSONObject object = new JSONObject("{\"commandId\":\"cmd30120824112718\",\"status\":\"sent\",\"conditions\": {\"validTo\":1445792143}}}");

        RemoteCommand command = null;
        String message = "";
        try {
            command = new RemoteCommand(object);
        } catch (final JSONException e) {
            message = e.getMessage();
        }

        assertNull("Invalid command", command);
        assertTrue("Invalid error", message.indexOf("type") >= 0);
    }

    public void testInvalidCondition() throws JSONException {
        JSONObject object = new JSONObject("{\"commandId\":\"cmd30120824112718\",\"status\":\"sent\",\"sendDate\":0, \"type\":\"promo.spentCurrencyDiscount\"}");


        RemoteCommand command = null;
        String message = "";
        try {
            command = new RemoteCommand(object);
        } catch (final JSONException e) {
            message = e.getMessage();
        }

        assertNull("Invalid command", command);
        assertTrue("Invalid error", message.indexOf("conditions") >= 0);
    }

    public void testInvalidValidTo() throws JSONException {
        JSONObject object = new JSONObject("{\"commandId\":\"cmd30120824112718\",\"status\":\"sent\", \"sendDate\":0, \"type\":\"promo.spentCurrencyDiscount\", "
                + "\"conditions\":{}}");


        RemoteCommand command = null;
        String message = "";
        try {
            command = new RemoteCommand(object);
        } catch (final JSONException e) {
            message = e.getMessage();
        }

        assertNull("Invalid command", command);
        assertTrue("Invalid error", message.indexOf("validTo") >= 0);
    }
}
