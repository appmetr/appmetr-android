/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.internal.command.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class RemoteCommandPacket {
    /**
     * The list of command from server
     */
    public final ArrayList<RemoteCommand> commandList;

    /**
     * flag means this latest batch of commands
     */
    public final boolean isLastCommandsBatch;

    /**
     * Listener with error callback
     */
    public interface Listener {
        boolean onRemoteCommandError(Throwable error);
    }

    /**
     * Creating RemoteCommandPacket from JSON object
     *
     * @param object A JSON object received from the server
     * @throws JSONException if required parameters is missing
     */
    public RemoteCommandPacket(JSONObject object, Listener listener) throws JSONException {
        // every packet must have status OK
        assert object.getString("status").compareTo("OK") == 0 : "Invalid status";

        JSONArray commands = object.getJSONArray("commands");
        commandList = new ArrayList<RemoteCommand>(commands.length());

        for (int i = 0; i < commands.length(); i++) {
            try {
                commandList.add(new RemoteCommand(commands.getJSONObject(i)));
            } catch (final JSONException e) {
                if (listener == null || !listener.onRemoteCommandError(e)) {
                    // re-throw
                    throw e;
                }
            }
        }

        isLastCommandsBatch = object.getBoolean("isLastCommandsBatch");
    }
}
