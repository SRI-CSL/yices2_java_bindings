package com.sri.yices;

import com.sri.yices.Yices;

/*
 * Catch all exception thrown by the Yices wrapper when something goes wrong.
 * We make this a RuntimeException to be consistent with the Galois AST visitors.
 * The visitors don't allow checked Exceptions.
 */
public class YicesException extends RuntimeException {

    // construct an exception form the Yices internal error string
    // then clear the internal error.
    protected YicesException() {
        super(Yices.errorString());
        Yices.resetError();
    }
}
