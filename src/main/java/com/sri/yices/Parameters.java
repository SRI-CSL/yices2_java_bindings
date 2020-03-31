package com.sri.yices;

/*
 * Wrapper around a Yices param_t structure
 */
public class Parameters implements AutoCloseable {
    // pointer to the parameter record
    private long ptr;

    //<PROFILING>
    static private long population = 0;

    /**
     * Returns the count of Parameters objects that have an unfreed
     * pointer to a Yices shared library object.
     */
    public static long getCensus(){
        return population;
    }
    //</PROFILING>

    /*
     * Constructor: all search parameters are set to their defaults
     */
    public Parameters() {
        ptr = Yices.newParamRecord();
        population++;
    }

    /*
     * Close: free the record
     */
    public void close() {
        if (ptr != 0) {
            Yices.freeParamRecord(ptr);
            ptr = 0;
            population--;
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
