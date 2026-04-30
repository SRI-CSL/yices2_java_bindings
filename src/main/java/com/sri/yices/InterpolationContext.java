package com.sri.yices;

// Since 2.6.4

public class InterpolationContext {

    private final Context ctxA;
    private final Context ctxB;

    private int interpolant = 0;
    private Model model = null;

    // constructor
    public InterpolationContext(Context ctxA, Context ctxB) {
        this.ctxA = ctxA;
        this.ctxB = ctxB;
        YicesException error = YicesException.checkVersion(2, 6, 4);
        if (error != null) {
            throw error;
        }
    }

    /**
     * Check satisfiability and compute interpolant.
     *
     * Check whether the combined assertions stored in ctx are satisfiable. If they are
     * not compute an interpolant (defined on variables common to both contexts).
     * - params is an optional structure to store heuristic parameters
     * - if params is NULL, default parameter settings are used.
     * If this function returns STATUS_UNSAT, then an interpolant can be obtained via getInterpolant()
     * If this function returns STATUS_SAT and buildModel is true, then a model can be obtained by getModel()
     * Only the first call to  getModel() returns a non-null model. The caller should free/close it when done.
     */
    public Status check(Parameters params, boolean buildModel) {
        int[] tarr = { 0 };
        long[] marr = { 0 };
        if (!buildModel) { marr = null; }
        int code = Yices.checkContextWithInterpolation(this.ctxA.getPtr(), this.ctxB.getPtr(), params == null ? 0 : params.getPtr(), marr, tarr);
        Status status = Status.idToStatus(code);
        if (status == Status.ERROR) {
            throw new YicesException();
        }
        if (buildModel && status == Status.SAT) {
            long model = marr[0];
            if (model == 0) throw new YicesException();
            this.model = new Model(model);
        } else if (status == Status.UNSAT) {
            this.interpolant = tarr[0];
        }
        return status;
    }


    public int getInterpolant() {
        return this.interpolant;
    }

    // Note that we give up ownership of the model. The caller should close it.
    public Model getModel() {
        Model retval = this.model;
        this.model = null;
        return retval;
    }


}
