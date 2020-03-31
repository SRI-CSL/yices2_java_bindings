package com.sri.yices;

/*
 * Context configuration
 */
public class Config implements AutoCloseable {
    // pointer to the Yices config_t object
    private long ptr;

    //<PROFILING>
    static private long population = 0;

    /**
     * Returns the count of Config objects that have an unfreed
     * pointer to a Yices shared library object.
     */
    public static long getCensus(){
        return population;
    }
    //</PROFILING>

    /*
     * Default configuration
     */
    public Config () {
        ptr = Yices.newConfig();
        population++;
    }

    /*
     * Default configuration for a given logic
     */
    public Config(String logic) throws YicesException {
        long p = Yices.newConfig();
        int code = Yices.defaultConfigForLogic(p, logic);
        if (code < 0) {
            Yices.freeConfig(p);
            throw new YicesException();
        }
        ptr = p;
        population++;
    }

    /*
     * Get the pointer
     */
    protected long getPtr() { return ptr; }

    /*
     * close
     */
    public void close() {
        if (ptr != 0) {
            Yices.freeConfig(ptr);
            ptr = 0;
            population--;
        }
    }


    /*
     * Set a configuration parameter
     * - name = parameter name
     * - value = parameter value
     */
    public void set(String name, String value) throws YicesException {
        int code = Yices.setConfig(ptr, name, value);
        if (code < 0) throw new YicesException();
    }

}
