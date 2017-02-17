/**
 * Copyright (c) 2017 AppMetr.
 * All rights reserved.
 */
package com.appmetr.android.internal;

import java.io.IOException;

/**
 * Special IO exception, when http error occurs
 */

public class HttpException extends IOException {

    public HttpException(String message) {
        super(message);
    }
}
