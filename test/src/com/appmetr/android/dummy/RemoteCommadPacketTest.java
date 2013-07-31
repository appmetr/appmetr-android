/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.dummy;

import com.appmetr.android.dummy.utils.BaseAppMetrDummyActivityTest;
import com.appmetr.android.internal.command.data.RemoteCommandPacket;
import org.json.JSONException;
import org.json.JSONObject;

public class RemoteCommadPacketTest extends BaseAppMetrDummyActivityTest {
    public void testParsing() throws JSONException {
        JSONObject object1 = new JSONObject(
                "{\"status\":\"OK\", \"commands\":["
                        + "{\"commandId\":\"cmd20120824112718\",\"sendDate\":0, \"type\":\"promo.realMoneyPurchaseBonus\","
                        + "\"conditions\":{\"validTo\":1345790143},"
                        + "\"properties\":{\"prop1\":10, \"prop2\":[1,2,3], \"prop3\":true, \"prop4\" : {\"sub1\":1, \"sub2\":2}}},"
                        + "{\"commandId\":\"cmd30120824112718\",\"sendDate\":0, \"type\":\"promo.spentCurrencyDiscount\",\"conditions\": {\"validTo\":1345792143}}],"
                        + "\"isLastCommandsBatch\":true}");

        RemoteCommandPacket packet = new RemoteCommandPacket(object1, null);

        assertEquals("Invalid commands", 2, packet.commandList.size());
        assertTrue("Invalid isLastCommandsBatch", packet.isLastCommandsBatch);

        JSONObject object2 = new JSONObject(
                "{\"status\":\"OK\", \"commands\":["
                        + "{\"commandId\":\"cmd20120824112718\",\"sendDate\":0, \"type\":\"promo.realMoneyPurchaseBonus\","
                        + "\"conditions\":{\"validTo\":1345790143},"
                        + "\"properties\":{\"prop1\":10, \"prop2\":[1,2,3], \"prop3\":true, \"prop4\" : {\"sub1\":1, \"sub2\":2}}}],"
                        + "\"isLastCommandsBatch\":false}");

        RemoteCommandPacket packet2 = new RemoteCommandPacket(object2, null);
        assertEquals("Invalid commands", 1, packet2.commandList.size());
        assertFalse("Invalid isLastCommandsBatch", packet2.isLastCommandsBatch);
    }
}
