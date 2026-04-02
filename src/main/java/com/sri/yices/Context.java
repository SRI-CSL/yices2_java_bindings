package com.sri.yices;

import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Class for wrapping yices contexts
 */
public class Context implements AutoCloseable {
    /*
     * pointer to the context
     */
    private long ptr;

    /**
     * A counter used to prevent memory leaks.
     */
    static private long population = 0;

    /**
     * Returns the count of Context objects that have an unfreed
     * pointer to a Yices shared library object.
     */
    public static long getCensus(){
        return population;
    }

    static private final int ERROR_STATUS;

    static {
        ERROR_STATUS = Status.ERROR.ordinal();
    }



    /*
     * Default constructor:
     * - the context supports push/pop
     * - included solvers:
     *    linear arithmetic, arrays + uninterpreted functions, bitvector
     */
    public Context() {
        ptr = Yices.newContext(0);
        population++;
    }

    /*
     * Constructor using a configuration
     */
    public Context(Config config) throws YicesException {
        long p = Yices.newContext(config.getPtr());
        if (p == 0) throw new YicesException();
        ptr = p;
        population++;
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
        population++;
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
        population++;
    }

    protected long getPtr() { return ptr; }

    /*
     * Close: free the Yices data structure
     */
    public void close() {
	    if (ptr != 0) {
            if (Profiler.enabled) {
                long start = System.nanoTime();
                Yices.freeContext(ptr);
                long finish = System.nanoTime();
                Profiler.delta("Yices.freeContext", start, finish);
            } else {
                Yices.freeContext(ptr);
            }
	        ptr = 0;
            population--;
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
        long model = 0;
        if (Profiler.enabled) {
            long start = System.nanoTime();
            model = Yices.getModel(ptr, 1);
            long finish = System.nanoTime();
            Profiler.delta("Yices.getModel", start, finish);
        } else {
            model = Yices.getModel(ptr, 1);
        }
        if (model == 0) throw new YicesException();
        return new Model(model);
    }

    /*
     * Assert a formula f
     */
    public void assertFormula(int f) throws YicesException {
        int code;
        if (Profiler.enabled) {
            long start = System.nanoTime();
            code = Yices.assertFormula(ptr, f);
            long finish = System.nanoTime();
            Profiler.delta("Yices.assertFormula", start, finish, true);
        } else {
            code = Yices.assertFormula(ptr, f);
        }
        if (code < 0) {
            throw new YicesException();
        }
    }

    /*
     * Assert an array of formulas a[]
     */
    public void assertFormulas(int[] a) throws YicesException {
        int code;
        if (Profiler.enabled) {
            long start = System.nanoTime();
            code = Yices.assertFormulas(ptr, a);
            long finish = System.nanoTime();
            Profiler.delta("Yices.assertFormulas", start, finish, true);
        } else {
            code = Yices.assertFormulas(ptr, a);
        }
        if (code < 0) {
            throw new YicesException();
        }
    }


    /*
     * Assert a list of formulas
     */
    public void assertFormulas(List<Integer> list) throws YicesException {
        int[] a = list.stream().mapToInt(Integer::intValue).toArray();
        assertFormulas(a);
    }

    /*
     * Assert a collection of formulas
     */
    public void assertFormulas(Collection<Integer> list) throws YicesException {
        int[] a = list.stream().mapToInt(Integer::intValue).toArray();
        assertFormulas(a);
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
    private static int doCheck(long ptr, long p) throws YicesException {
        int code;
        if (Profiler.enabled) {
            long start = System.nanoTime();
            code = Yices.checkContext(ptr, p);
            long finish = System.nanoTime();
            Profiler.delta("Yices.checkContext", start, finish);
        } else {
            code = Yices.checkContext(ptr, p);
        }
        return code;
    }

    private int doCheck(long p) throws YicesException {
        int code = doCheck(ptr, p);
        if (code == ERROR_STATUS) throw new YicesException();
        return code;
    }

    /*
     * Call the solver with default parameters
     */
    public Status check() throws YicesException {
        return check(null);
    }

    /*
     * Call the solver, use the given parameter set.
     */
    public Status check(Parameters p) throws YicesException {
        int code = doCheck(p == null ? 0 : p.getPtr());
        return Status.idToStatus(code);
    }

    /*
     * Check with a timeout in seconds
     */
    public Status check(int timeout) throws YicesException {
        return doCheckWithTimer(0, timeout);
    }

    public Status check(Parameters p, int timeout) throws YicesException {
        return doCheckWithTimer(p.getPtr(), timeout);
    }

    /*
     * Check with a timeout:
     * - p = pointer to the Yices internal parameter descriptor
     * - timeout = timeout in seconds
     * This calls Yices.stopSearch if the timer expires.
     */
    private Status doCheckWithTimer(long p, int timeout)  throws YicesException {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            public void run() {
                Yices.stopSearch(ptr);
            }
        };
        long delay = (timeout < 1) ? 1000L : 1000L * timeout; //need miliseconds, have seconds.
        timer.schedule(task, delay);
        int code = doCheck(ptr, p);
        timer.cancel();
        if (code < 0) throw new YicesException();
        return Status.idToStatus(code);
    }

    // Since 2.6.4
    public int getModelInterpolant() {
        int retval = Yices.getModelInterpolant(ptr);
        if (retval < 0){
            YicesException error = YicesException.checkVersion(2, 6, 4);
            if (error == null) {
                // not a library mismatch error; so do the default
                error = new YicesException();
            }
            throw error;
        }
        return retval;
    }

    // Since 2.6.4
    public Status checkWithAssumptions(Parameters params, int[] assumptions) {
        int code = Yices.checkContextWithAssumptions(ptr, params == null ? 0 : params.getPtr(), assumptions);
        if (code < 0) {
            throw new YicesException();
        }
        return Status.idToStatus(code);
    }

    // Since 2.6.4
    public Status checkWithModel(Parameters params, Model model, int[] assumptions) {
        int code = Yices.checkContextWithModel(ptr, params == null ? 0 : params.getPtr(), model.getPtr(), assumptions);
        if (code < 0) {
            YicesException error = YicesException.checkVersion(2, 6, 4);
            if (error == null) {
                // not a library mismatch error; so do the default
                error = new YicesException();
            }
            throw error;
        }
        return Status.idToStatus(code);
    }

    // Since 2.6.4
    public int[] getUnsatCore() {
        int[] retval = Yices.getUnsatCore(ptr);
        if (retval == null) {
            YicesException error = YicesException.checkVersion(2, 6, 4);
            if (error == null) {
                // not a library mismatch error; so do the default
                error = new YicesException();
            }
            throw error;
        }
        return retval;
    }


 }
