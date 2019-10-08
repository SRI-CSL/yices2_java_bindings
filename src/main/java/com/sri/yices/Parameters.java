package com.sri.yices;

/*
 * Wrapper around a Yices param_t structure
 */
public class Parameters implements AutoCloseable {
    // pointer to the parameter record
    private long ptr;

    /*
     * Constructor: all search parameters are set to their defaults
     */
    public Parameters() {
        ptr = Yices.newParamRecord();
    }

    /*
     * Close: free the record
     */
    public void close() {
        if (ptr != 0) {
            Yices.freeParamRecord(ptr);
            ptr = 0;
        }
    }


    protected long getPtr() { return ptr; }

    /*
     * Set a search parameter: name and value are both given as strings
     */
    public void setParam(String name, String value) throws YicesException {
        int code = Yices.setParam(ptr, name, value);
        if (code < 0) throw new YicesException();
    }

    /*
     * Set parameters for a context
     */
    public void defaultsForContext(Context ctx) {
        Yices.defaultParamsForContext(ctx.getPtr(), ptr);
    }
}
