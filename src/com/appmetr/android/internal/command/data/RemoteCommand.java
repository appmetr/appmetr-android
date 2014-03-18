/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.internal.command.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public final class RemoteCommand {

    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_SKIP = "skip";
    public static final String STATUS_FAIL = "fail";
    public static final String STATUS_NOT_SENT = "not_sent";
    public static final String STATUS_SENT = "sent";
    public static final String STATUS_RE_SENT = "re_sent";

    /**
     * the unique identifier of this command
     */
    public final String uniqueIdentifier;
    /**
     * status of this command
     */
    public final String status;
    /**
     * type of this command
     */
    public final String type;
    /**
     * date, the latest of which this command can run
     */
    public final Date validTo;
    /**
     * parameter of this command
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
        status = object.getString("status");
        type = object.getString("type");

        long sendDate = object.getLong("sendDate");
        long validToValue = object.getJSONObject("conditions").getLong("validTo");

        Date now = new Date();
        validTo = new Date(now.getTime() + (validToValue - sendDate));

        // attach command object instead of properties
        properties = object;
    }
}
