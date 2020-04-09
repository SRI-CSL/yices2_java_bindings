package com.sri.yices;

/**
 *  Providing access to yices's third party SAT solvers.
 *
 */
public class  Delegate {

    public static final String CADICAL = "cadical";
    public static final String CRYPTOMINISAT = "cryptominisat";
    public static final String Y2SAT = "y2sat";

    public static Status checkFormula(int term, String logic, String delegate, Model[] marr){
        if (marr != null && marr.length < 1) {
            throw new IllegalArgumentException("array too small");
        }
        long[] larr = marr == null ? null : new long[1];
        int status = Yices.checkFormula(term, logic, delegate, larr);
        Status retval = Status.idToStatus(status);
        if (retval == Status.SAT && marr != null){
            marr[0] = new Model(larr[0]);
        }
        return retval;
    }

    public static Status checkFormulas(int[] terms, String logic, String delegate, Model[] marr){
        if (marr != null && marr.length < 1) {
            throw new IllegalArgumentException("array too small");
        }
        long[] larr = marr == null ? null : new long[1];
        int status = Yices.checkFormulas(terms, logic, delegate, larr);
        Status retval = Status.idToStatus(status);
        if (retval == Status.SAT && marr != null){
            marr[0] = new Model(larr[0]);
        }
        return retval;
    }



}
