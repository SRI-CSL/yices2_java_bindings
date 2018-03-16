package com.sri.yices;

import java.math.BigInteger;

/**
 * Class for Yices models
 */
public class Model implements java.lang.AutoCloseable {
    /*
     * This is a pointer to the model
     */
    private long ptr = 0;

    protected Model(long p) {
        ptr = p;
    }

    /*
     * Constructor from a map:
     * - var and map must be arrays of the same length
     * - var[i] must be an uninterpreted term
     *   map[i] must be a term of type compatible with the type of var[i]
     */
    public Model(int[] var, int[] map) throws Exception {
        if (var.length != map.length)
            throw new IllegalArgumentException("var and map must have the same length");
        long p = Yices.modelFromMap(var, map);
        if (p == 0) throw new YicesException();
        ptr = p;
    }

    /*
     * Finalize and close free the Yices internal model
     */
//     protected void finalize() {
//         if (ptr != 0) {
//             Yices.freeModel(ptr);
//             ptr = 0;
//         }
//     }

    public void close() {
        if (ptr != 0) {
            Yices.freeModel(ptr);
            ptr = 0;
        }
    }


    /*
     * Conversion to string
     * - this uses the Yices pretty printer.
     * - the first version prints the model in a box of 80 columns
     * - the second version uses numColumns and numLines
     */
    public String toString() {
        return Yices.modelToString(ptr);
    }

    public String toString(int numColumns, int numLines) {
        return Yices.modelToString(ptr, numColumns, numLines);
    }

    /*
     * Value of a term t in the model
     */
    public boolean boolValue(int t) throws YicesException {
        int x = Yices.getBoolValue(ptr, t);
        // x is either -1 (error), 0 (false), or 1 (true).
        if (x < 0) throw new YicesException();
        return x != 0;
    }

    public long integerValue(int t) throws YicesException {
        long[] aux = new long[1];
        int code = Yices.getIntegerValue(ptr, t, aux);
        if (code < 0) throw new YicesException();
        return aux[0];
    }

    public double doubleValue(int t) throws YicesException {
        double[] aux = new double[1];
        int code = Yices.getDoubleValue(ptr, t, aux);
        if (code < 0) throw new YicesException();
        return aux[0];
    }

    // the numerator is returned in a[0].
    // the denominator is returned in a[1].
    public void rationalValue(int t, long[] a) throws Exception {
        if (a.length < 2) throw new IllegalArgumentException("array too small");
        int code = Yices.getRationalValue(ptr, t, a);
        if (code < 0) throw new YicesException();
    }

    public BigInteger bigIntegerValue(int t) throws YicesException {
        BigInteger v = Yices.getIntegerValue(ptr, t);
        if (v == null) throw new YicesException();
        return v;
    }

    public BigRational bigRationalValue(int t) throws YicesException {
        BigRational v = Yices.getRationalValue(ptr, t);
        if (v == null) throw new YicesException();
        return v;
    }

    public boolean[] bvValue(int t) throws YicesException {
        boolean[] b = Yices.getBvValue(ptr, t);
        if (b == null) throw new YicesException();
        return b;
    }

    public int scalarValue(int t) throws YicesException {
        int v = Yices.getScalarValue(ptr, t);
        if (v < 0) throw new YicesException();
        return v;
    }

    public int valueAsTerm(int t) throws YicesException {
        int v = Yices.valueAsTerm(ptr, t);
        if (v < 0) throw new YicesException();
        return v;
    }
}
