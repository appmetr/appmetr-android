/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.internal.command.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public final class RemoteCommand {
    /**
     * the unique identifier of this command
     */
    public final String uniqueIdentifier;
    /**
     * the type of this command
     */
    public final String type;
    /**
     * the date, the latest of which this command can run
     */
    public final Date validTo;
    /**
     * the parameter of this command
     */
    public final JSONObject properties;

    /**
     * Creating the AppMetr remote command
     *
     * @param object - A JSON object of command received from the server
     * @throws JSONException if JSON object does'nt have required parameters
     */
    public RemoteCommand(JSONObject object) throws JSONException {
        uniqueIdentifier = object.getString("commandId");
        type = object.getString("type");

        long sendDate = object.getLong("sendDate");
        long validToValue = object.getJSONObject("conditions").getLong("validTo");

        Date now = new Date();
        validTo = new Date(now.getTime() + (validToValue - sendDate));

        // attach command object instead of properties
        properties = object;
    }
}
