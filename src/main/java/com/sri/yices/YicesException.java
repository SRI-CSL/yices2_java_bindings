package com.sri.yices;

/**
 * Catch all exception thrown by the Yices wrapper when something goes wrong.
 */
public class YicesException extends RuntimeException {

    // construct an exception form the Yices internal error string
    // then clear the internal error.
    protected YicesException() {
        super(Yices.errorString());
        Yices.resetError();
    }
}
