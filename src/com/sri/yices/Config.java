package com.sri.yices;

/*
 * Context configuration
 */
public class Config implements java.lang.AutoCloseable {
    // pointer to the Yices config_t object
    private long ptr;

    /*
     * Default configuration
     */
    public Config () {
        ptr = Yices.newConfig();
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
    }

    /*
     * Get the pointer
     */
    protected long getPtr() { return ptr; }

    /*
     * Finalize and close
     */
//     protected void finalize() {
//         if (ptr != 0) {
//             Yices.freeConfig(ptr);
//             ptr = 0;
//         }
//     }

    public void close() {
        if (ptr != 0) {
            Yices.freeConfig(ptr);
            ptr = 0;
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
