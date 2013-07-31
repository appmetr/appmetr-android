/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.internal.command;

import android.os.Handler;
import android.util.Log;
import com.appmetr.android.AppMetr;
import com.appmetr.android.AppMetrListener;
import com.appmetr.android.BuildConfig;
import com.appmetr.android.internal.LibraryPreferences;
import com.appmetr.android.internal.WebServiceRequest;
import com.appmetr.android.internal.command.data.RemoteCommand;
import com.appmetr.android.internal.command.data.RemoteCommandPacket;
import com.appmetr.android.internal.command.exception.AppMetrInvalidCommandException;
import com.appmetr.android.internal.command.exception.AppMetrUnsatisfiedConditionException;
import org.apache.http.HttpException;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class to manage remote commands
 */
public class CommandsManager {
    private static final String TAG = "CommandsManager";
    private final LibraryPreferences mPreferences;

    protected final ArrayList<RemoteCommand> mRemoteCommandList = new ArrayList<RemoteCommand>();

    private final Lock mCommandTaskLock = new ReentrantLock();
    private Runnable mCommandTask;

    protected volatile AppMetrListener mListener;
    protected String mLastReceivedCommandID = null;

    private final Runnable mProcessCommandTask;

    public CommandsManager(LibraryPreferences preferences) {
        mPreferences = preferences;
        mLastReceivedCommandID = mPreferences.getLastProcessedCommandID();
        mProcessCommandTask = new Runnable() {
            public void run() {
                processCommands();
            }
        };
    }

    public void addRemoteCommand(RemoteCommand command) {
        synchronized (mRemoteCommandList) {
            mRemoteCommandList.add(command);
        }
    }

    public RemoteCommand getNextCommand() {
        RemoteCommand ret = null;
        synchronized (mRemoteCommandList) {
            if (mRemoteCommandList.size() > 0) {
                ret = mRemoteCommandList.get(0);
                mRemoteCommandList.remove(0);
            }
        }
        return ret;
    }

    public int getNumCommands() {
        return mRemoteCommandList.size();
    }

    /**
     * Processing the remote command, received from server.
     */
    public void processCommands() {
        processCommands(mListener);
    }

    protected void processCommands(final AppMetrListener listener) {
        if (listener != null) {
            RemoteCommand command;
            while ((command = getNextCommand()) != null) {
                try {
                    executeCommand(listener, command);
                } catch (final Throwable t) {
                    Log.e(TAG, "Failed to execute remote command id:" + command.uniqueIdentifier, t);
                    CommandTracker.trackCommandFail(command.uniqueIdentifier, t);
                }

                mPreferences.setLastProcessedCommandID(command.uniqueIdentifier);
            }

            try {
                AppMetr.pullCommands();
            } catch (final Throwable t) {
                Log.e(TAG, "Failed to pull commands.", t);
            }
        }
    }

    private boolean validateCommand(RemoteCommand command) {
        if (mPreferences.hasCommandProcessd(command.uniqueIdentifier)) {
            CommandTracker.trackCommandSkip(command.uniqueIdentifier, "duplicateId");
            return false;
        }

        Date now = new Date();
        if (now.compareTo(command.validTo) > 0) {
            CommandTracker.trackCommandSkip(command.uniqueIdentifier, "validTo");
            return false;
        }

        return true;
    }

    private void executeCommand(AppMetrListener listener, RemoteCommand command) throws Throwable {
        if (validateCommand(command)) {
            mPreferences.setCommandProcessed(command.uniqueIdentifier);
            try {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Executing command id:" + command.uniqueIdentifier);
                }

                listener.executeCommand(command.properties);
                CommandTracker.trackCommand(command.uniqueIdentifier);
            } catch (final AppMetrInvalidCommandException e) {
                CommandTracker.trackCommandFail(command.uniqueIdentifier, e.getProperties());
            } catch (final AppMetrUnsatisfiedConditionException e) {
                CommandTracker.trackCommandSkip(command.uniqueIdentifier, e.getProperties());
            }
        }
    }

    /**
     * Sets the handler for remote commands
     *
     * @param handler
     */
    public void setCommandHandler(final Handler handler) {
        if (handler != null) {
            setCommandTask(new Runnable() {
                public void run() {
                    handler.post(mProcessCommandTask);
                }
            });
        } else {
            setCommandTask(null);
        }
    }

    /**
     * Sets the task that will run by CommandsManager to select executing thread
     * for remote commands. This task must invoke Runnable that returned by
     * {@link #getInternalProcessCommand()}
     *
     * @param task The task that will be executed or null
     */
    public void setCommandTask(Runnable task) {
        synchronized (mCommandTaskLock) {
            mCommandTask = task;
            if (mCommandTask != null) {
                shceduleRemoteCommands();
            }
        }
    }

    /**
     * @return The current command task
     * @see CommandsManager#setCommandTask(Runnable)
     */
    public Runnable getCommandTask() {
        return mCommandTask;
    }

    /**
     * @return The runnable that just invoke {@link #processCommands()}
     */
    public Runnable getInternalProcessCommand() {
        return mProcessCommandTask;
    }

    /**
     * Sets the listener for this object
     *
     * @param lister The new listener object or null
     */
    public void setListener(AppMetrListener lister) {
        mListener = lister;
        shceduleRemoteCommands();
    }

    /**
     * @return The current AppMetr listener object or null
     * @see #setListener(com.appmetr.android.AppMetrListener)
     */
    public AppMetrListener getListener() {
        return mListener;
    }

    public void shceduleRemoteCommands() {
        synchronized (mCommandTaskLock) {
            if (mListener != null && mCommandTask != null) {
                mCommandTask.run();
            }
        }
    }

    public void processPacket(RemoteCommandPacket packet) {
        boolean res = false;
        for (RemoteCommand command : packet.commandList) {
            addRemoteCommand(command);
            res = true;
            mLastReceivedCommandID = command.uniqueIdentifier;
        }

        if (res) {
            shceduleRemoteCommands();
        }
    }

    public void sentQueryRemoteCommandList(List<NameValuePair> parameters, WebServiceRequest webServise) {
        if (mListener != null) {
            if (mLastReceivedCommandID != null) {
                parameters.add((new BasicNameValuePair("lastCommandId", mLastReceivedCommandID)));
            }
            sentQueryRemoteCommandListImpl(parameters, webServise);
        }
    }

    private void sentQueryRemoteCommandListImpl(List<NameValuePair> parameters, WebServiceRequest webServise) {
        try {
            JSONObject responce = null;
            String status = null;
            try {
                responce = webServise.sendRequest(parameters);
                status = responce.optString("status");
            } catch (final HttpException e) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "server.getCommands failed.", e);
                }
            } catch (final IOException e) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "server.getCommands failed.", e);
                }
            }

            if (status != null && status.compareTo("OK") == 0) {
                RemoteCommandPacket packet = new RemoteCommandPacket(responce, new RemoteCommandPacket.Listener() {
                    public boolean onRemoteCommandError(Throwable error) {
                        Log.e(TAG, "getCommand failed", error);

                        CommandTracker.trackCommandFail(mLastReceivedCommandID, error);
                        return true;
                    }
                });

                processPacket(packet);
            }
        } catch (final JSONException e) {
            Log.e(TAG, "getCommand failed", e);
            CommandTracker.trackCommandBatch(mLastReceivedCommandID, "JSONException", e.getMessage());
        }
    }
}
