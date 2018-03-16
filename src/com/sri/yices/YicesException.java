package com.sri.yices;

import com.sri.yices.Yices;

/**
 * Catch all exception thrown by the Yices wrapper when something goes wrong.
 */
public class YicesException extends Exception {

    // construct an exception form the Yices internal error string
    // then clear the internal error.
    protected YicesException() {
        super(Yices.errorString());
        Yices.resetError();
    }
}
