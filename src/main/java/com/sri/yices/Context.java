package com.sri.yices;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Class for wrapping yices contexts
 */
public class Context implements AutoCloseable {
    /*
     * pointer to the context
     */
    private long ptr;

    static private final int ERROR_STATUS;

    static {
        ERROR_STATUS = Status.ERROR.getIndex();
    }

    /*
     * Default constructor:
     * - the context supports push/pop
     * - included solvers:
     *    linear arithmetic, arrays + uninterpreted functions, bitvector
     */
    public Context() {
        ptr = Yices.newContext(0);
    }

    /*
     * Constructor using a configuration
     */
    public Context(Config config) throws YicesException {
        long p = Yices.newContext(config.getPtr());
        if (p == 0) throw new YicesException();
        ptr = p;
    }

    /*
     * Constructor for a given logic:
     * - supports push/pop but specialized for the logic
     */
    public Context(String logic) throws YicesException {
        long config = Yices.newConfig();
        int code = Yices.defaultConfigForLogic(config, logic);
        if (code < 0) {
            Yices.freeConfig(config);
            throw new YicesException();
        }
        long p = Yices.newContext(config);
        if (p == 0) {
            Yices.freeConfig(config);
            throw new YicesException();
        }
        Yices.freeConfig(config);
        ptr = p;
    }

    /*
     * Constructor for a given logic and mode
     * - the allowed modes are "one-shot", "multi-check", "push-pop", "interactive"
     * - not all modes are supported by all logics
     */
    public Context(String logic, String mode) throws YicesException {
        long config = Yices.newConfig();
        int code = Yices.defaultConfigForLogic(config, logic);
        if (code >= 0) {
            code = Yices.setConfig(config, "mode", mode);
        }
        if (code < 0) {
            Yices.freeConfig(config);
            throw new YicesException();
        }
        long p = Yices.newContext(config);
        if (p == 0) {
            Yices.freeConfig(config);
            throw new YicesException();
        }
        Yices.freeConfig(config);
        ptr = p;
    }

    protected long getPtr() { return ptr; }

    /*
     * Close: free the Yices data structure
     */
    public void close() {
	    if (ptr != 0) {
	        Yices.freeContext(ptr);
	        ptr = 0;
	    }
    }

    /*
     * Enable/disable options
     */
    public void enableOption(String option) throws YicesException {
        int code = Yices.contextEnableOption(ptr, option);
        if (code < 0) throw new YicesException();
    }

    public void disableOption(String option) throws YicesException {
        int code = Yices.contextDisableOption(ptr, option);
        if (code < 0) throw new YicesException();
    }

    /*
     * Get the status
     */
    public Status getStatus() {
        return Status.idToStatus(Yices.contextStatus(ptr));
    }

    /*
     * Push/pop/reset
     * - push and pop may fail if the context does not support them
     */
    public void reset() {
        Yices.resetContext(ptr);
    }

    public void push() throws YicesException {
        int code = Yices.push(ptr);
        if (code < 0) throw new YicesException();
    }

    public void pop() throws YicesException {
        int code = Yices.pop(ptr);
        if (code < 0) throw new YicesException();
    }

    /*
     * Stop search
     */
    public void stopSearch() {
        Yices.stopSearch(ptr);
    }

    /*
     * Get a model
     */
    public Model getModel() throws YicesException {
        long model = Yices.getModel(ptr, 1);
        if (model == 0) throw new YicesException();
        return new Model(model);
    }

    /*
     * Assert a formula f
     */
    public void assertFormula(int f) throws YicesException {
        int code = Yices.assertFormula(ptr, f);
        if (code < 0) {
            System.out.println("--- Error in assertFomula ---");
            System.out.println(Terms.toString(f));
            System.out.println("---");
            throw new YicesException();
        }
    }

    /*
     * Assert an array of formulas a[]
     */
    public void assertFormulas(int[] a) throws YicesException {
        int code = Yices.assertFormulas(ptr, a);
        if (code < 0) throw new YicesException();
    }

    /*
     * Assert a list of formulas
     */
    public void assertFormulas(Collection<Integer> list) throws YicesException {
        list.forEach(this::assertFormula);
    }

    /*
     * Assert a blocking clause
     */
    public void assertBlockingClause() throws YicesException {
        int code = Yices.assertBlockingClause(ptr);
        if (code < 0) throw new YicesException();
    }

    /*
     * Call the solver, use parameter pointer p
     */
    private Status doCheck(long p) throws YicesException {
        int code = Yices.checkContext(ptr, p);
        if (code == ERROR_STATUS) throw new YicesException();
        return Status.idToStatus(code);
    }

    /*
     * Call the solver with default parameters
     */
    public Status check() throws YicesException {
        return doCheck(0); // null pointer
    }

    /*
     * Call the solver, use the given parameter set.
     */
    public Status check(Parameters p) throws YicesException {
        return doCheck(p.getPtr());
    }

    /*
     * Simple watchdog to stop the search after a timeout
     */
    private class WatchDog implements Runnable {
        private long ctx;
        private int timeout; // in seconds
        private volatile boolean stopped;
        private Thread thread;

        WatchDog(long ctx, int timeout) {
            if (timeout < 1) timeout = 1;
            this.ctx = ctx;
            this.timeout = timeout;
            stopped = false;
            thread = null;
        }

        public void run() {
            try {
                TimeUnit.SECONDS.sleep(timeout);
            } catch (InterruptedException e) {
                // don't do anything
            }
            if (! stopped) {
                // stop the context
                Yices.stopSearch(ctx);
            }
        }

        public synchronized void stop() {
            stopped = true;
            if (thread != null && thread.isAlive()) {
               thread.interrupt();
               thread = null;
            }
        }

        public synchronized void start() {
            stopped = false;
            thread =  new Thread(this::run);
            thread.start();
        }
    }

    /*
     * Check with timeout
     */
    private Status doCheckWithTimeout(long p, int timeout) throws YicesException {
        WatchDog watchDog = new WatchDog(ptr, timeout);
        watchDog.start();
        int code = Yices.checkContext(ptr, p);
        watchDog.stop();
        if (code < 0) throw new YicesException();
        return Status.idToStatus(code);
    }

    public Status check(int timeout) throws YicesException {
        return doCheckWithTimeout(0, timeout);
    }

    public Status check(Parameters p, int timeout) throws YicesException {
        return doCheckWithTimeout(p.getPtr(), timeout);
    }

 }
