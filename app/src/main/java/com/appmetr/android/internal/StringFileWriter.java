/**
 * Copyright (c) 2013 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.internal;

import android.content.Context;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * Class which works with files. Called from AppMetrMobile library.
 */
public class StringFileWriter {
    protected final static String CHUNK_SEPARATOR = ",";
    protected final static String BATCH_OPENING = "[";
    protected final static String BATCH_CLOSING = "]";
    protected String mFileName;
    protected DeflaterOutputStream mOutputStream;
    protected int mCurrentFileSize = 0;

    /**
     * Default constructor of class.
     *
     * @param context   - required parameter. Must be valid.
     * @param fileIndex - index of file.
     * @throws IOException
     */
    public StringFileWriter(Context context, int fileIndex) throws IOException {
        mFileName = "batch" + fileIndex;

        FileOutputStream fileOutput = context.openFileOutput(mFileName, Context.MODE_PRIVATE);
        mOutputStream = new DeflaterOutputStream(fileOutput, new Deflater(Deflater.BEST_COMPRESSION, true));
        mOutputStream.write(BATCH_OPENING.getBytes(), 0, BATCH_OPENING.length());
    }

    /**
     * Public method, which save data and close pointer to file.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        mOutputStream.write(BATCH_CLOSING.getBytes(), 0, BATCH_CLOSING.length());
        mOutputStream.finish();
        mOutputStream.close();
    }

    /**
     * Method which returns current file name.
     *
     * @return - name of file
     */
    public String getFileName() {
        return mFileName;
    }

    /**
     * Method which returns size of current file.
     *
     * @return - size of file.
     */
    public int getCurrentFileSize() {
        return mCurrentFileSize;
    }

    /**
     * Method which writes string to current file and adds separator.
     *
     * @param chunk - string to write.
     * @throws IOException
     */
    public void addChunk(String chunk) throws IOException {
        if (mCurrentFileSize > 0) {
            mOutputStream.write(CHUNK_SEPARATOR.getBytes(), 0, CHUNK_SEPARATOR.length());
            mCurrentFileSize += CHUNK_SEPARATOR.length();
        }
        byte[] chunkBuff = chunk.getBytes();
        mOutputStream.write(chunkBuff, 0, chunkBuff.length);
        mCurrentFileSize += chunkBuff.length;
    }
}
