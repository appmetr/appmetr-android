/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android;

import org.json.JSONObject;

public interface AppMetrListener {
    /**
     * Executing a remote command from server
     *
     * @param command A command received from server or null
     * @throws Throwable if fail
     */
    public void executeCommand(JSONObject command) throws Throwable;
}
