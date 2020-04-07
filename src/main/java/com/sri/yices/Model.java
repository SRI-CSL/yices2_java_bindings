package com.sri.yices;

import java.math.BigInteger;

/**
 * Class for Yices models
 */
public class Model implements AutoCloseable {
    /*
     * This is a pointer to the model
     */
    private long ptr;

    protected Model(long p) {
        ptr = p;
        population++;
    }

    //<PROFILING>
    static private long population = 0;

    /**
     * Returns the count of Model objects that have an unfreed pointer
     * to a Yices shared library object.
     */
    public static long getCensus(){
        return population;
    }
    //</PROFILING>

    /*
     * Constructor from a map:
     * - var and map must be arrays of the same length
     * - var[i] must be an uninterpreted term
     *   map[i] must be a term of type compatible with the type of var[i]
     */
    public Model(int[] var, int[] map) throws YicesException {
        if (var.length != map.length)
            throw new IllegalArgumentException("var and map must have the same length");
        long p = Yices.modelFromMap(var, map);
        if (p == 0) throw new YicesException();
        ptr = p;
        population++;
    }

    /*
     * Close: free the Yices internal model
     */
    public void close() {
        if (ptr != 0) {
            Yices.freeModel(ptr);
            ptr = 0;
            population--;
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
    public void rationalValue(int t, long[] a) throws YicesException {
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

    /*
     * Term exploration in a model
     */
    public YVal getValue(int t){
        return Yices.getValue(ptr, t);
    }

    public boolean isInt(YVal yval){
        return Yices.valIsInt(ptr, yval.tag.ordinal(), yval.id);
    }

    public boolean isLong(YVal yval){
        return Yices.valIsLong(ptr, yval.tag.ordinal(), yval.id);
    }

    public boolean isInteger(YVal yval){
        return Yices.valIsLong(ptr, yval.tag.ordinal(), yval.id);
    }

    public int bitSize(YVal yval){
        return Yices.valBitSize(ptr, yval.tag.ordinal(), yval.id);
    }

    public int tupleArity(YVal yval){
        return Yices.valTupleArity(ptr, yval.tag.ordinal(), yval.id);
    }

    public int mappingArity(YVal yval){
        return Yices.valMappingArity(ptr, yval.tag.ordinal(), yval.id);
    }

    public int functionArity(YVal yval){
        return Yices.valFunctionArity(ptr, yval.tag.ordinal(), yval.id);
    }

    public boolean boolValue(YVal yval) throws YicesException {
        int code = Yices.valGetBool(ptr, yval.tag.ordinal(), yval.id);
        if (code < 0) throw new YicesException();
        return code == 1;
    }

    public long integerValue(YVal yval) throws YicesException {
        long[] aux = new long[1];
        int code = Yices.valGetInteger(ptr, yval.tag.ordinal(), yval.id, aux);
        if (code < 0) throw new YicesException();
        return aux[0];
    }

    public double doubleValue(YVal yval) throws YicesException {
        double[] aux = new double[1];
        int code = Yices.valGetDouble(ptr, yval.tag.ordinal(), yval.id, aux);
        if (code < 0) throw new YicesException();
        return aux[0];
    }

    // the numerator is returned in a[0].
    // the denominator is returned in a[1].
    public void rationalValue(YVal yval, long[] a) throws YicesException {
        if (a.length < 2) throw new IllegalArgumentException("array too small");
        int code = Yices.valGetRational(ptr, yval.tag.ordinal(), yval.id, a);
        if (code < 0) throw new YicesException();
    }

    public BigInteger bigIntegerValue(YVal yval) throws YicesException {
        BigInteger v = Yices.valGetInteger(ptr, yval.tag.ordinal(), yval.id);
        if (v == null) throw new YicesException();
        return v;
    }

    public BigRational bigRationalValue(YVal yval) throws YicesException {
        BigRational v = Yices.valGetRational(ptr, yval.tag.ordinal(), yval.id);
        if (v == null) throw new YicesException();
        return v;
    }

    public boolean[] bvValue(YVal yval) throws YicesException {
        boolean[] b = Yices.valGetBV(ptr, yval.tag.ordinal(), yval.id);
        if (b == null) throw new YicesException();
        return b;
    }

    // If successful returns true, and stores the scalar value in a[0], and it's type in a[1]
    // If unsuccessful, returns false and does nothin to a.
    public boolean scalarValue(YVal yval, int[] a) throws YicesException {
        if (a.length < 2) throw new IllegalArgumentException("array too small");
        int v = Yices.valGetScalar(ptr, yval.tag.ordinal(), yval.id, a);
        if (v < 0) return false;
        return true;
    }


}
