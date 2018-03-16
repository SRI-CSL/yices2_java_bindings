package com.sri.yices;

import java.util.Arrays;

/**
 * Wrappers to access the Yices type constructors.
 * These call the native API and throw an exception if there's an error.
 */
public class Types {
    /**
     * Short cuts for common types bool, int, real, bv8, bv16, bv32, bv64
     */
    public static final int NULL_TYPE;
    public static final int BOOL;
    public static final int INT;
    public static final int REAL;
    public static final int BV8;
    public static final int BV16;
    public static final int BV32;
    public static final int BV64;

    static {
        NULL_TYPE = -1;
        BOOL = Yices.boolType();
        INT = Yices.intType();
        REAL = Yices.realType();
        // it's safe to call the bvType constructor with these values
        BV8 = Yices.bvType(8);
        BV16 = Yices.bvType(16);
        BV32 = Yices.bvType(32);
        BV64 = Yices.bvType(64);
    }

    /*
     * TYPE CONSTUCTORS
     */

    /*
     * Primitive types
     */
    static public int boolType() {
        return BOOL;
    }

    static public int intType() { return INT; }

    static public int realType() {
        return REAL;
    }

    static public int bvType(int nbits) throws Exception {
        if (nbits <= 0) {
            throw new IllegalArgumentException("nbits must be positive");
        }
        int tau = Yices.bvType(nbits);
        if (tau < 0) {
            throw new YicesException();
        }
        return tau;
    }

    /*
     * Scalar and uninterpreted types
     */
    // anonymous scalar type of the given cardinality
    static public int newScalarType(int card) throws Exception {
        if (card <= 0) {
            throw new IllegalArgumentException("card must be positive");
        }
        int tau = Yices.newScalarType(card);
        if (tau < 0) {
            throw new YicesException();
        }
        return tau;
    }

    // named scalar type
    static public int newScalarType(String name, int card) throws Exception {
        int tau = newScalarType(card);
        Yices.setTypeName(tau, name);
        return tau;
    }

    // anonymous uninterpreted type
    static public int newUninterpretedType() {
        return Yices.newUninterpretedType();
    }

    // named uninterpreted type
    static public int newUninterpretedType(String name) {
        int tau = Yices.newUninterpretedType();
        Yices.setTypeName(tau, name);
        return tau;
    }

    /*
     * Declare new uninterpreted or scalar types
     */
    static public void declareUninterpretedType(String name) {
        int tau = Yices.newUninterpretedType();
        Yices.setTypeName(tau, name);
    }

    static public void declareScalarType(String name, int card) throws Exception {
        int tau = newScalarType(card);
        Yices.setTypeName(tau, name);
    }

    /*
     * Tuple and function types
     */
    static public int tupleType(int... a) throws Exception {
        int tau = Yices.tupleType(a);
        if (tau < 0) {
            throw new YicesException();
        }
        return tau;
    }

    /*
     * Function type: domain a, range sigma
     */
    static public int functionType(int[] a, int sigma) throws Exception {
        if (a == null || a.length == 0) {
            throw new IllegalArgumentException("bad function type: empty domain");
        }
        int tau = Yices.functionType(sigma, a);
        if (tau < 0) {
            throw new YicesException();
        }
        return tau;
    }

    /*
     * Array a contains domain + range:
     *   a[0] ... a[n-2] = domain
     *   a[n-1] = range type,
     * where n>=2 = length of the array.
     */
    static public int functionType(int... a) throws Exception {
        if (a == null || a.length < 2) {
            throw new IllegalArgumentException("bad function type");
        }
        int range = a[a.length-1];
        int[] domain = Arrays.copyOf(a, a.length - 1);
        int tau = Yices.functionType(range, domain);
        if (tau < 0) {
            throw new YicesException();
        }
        return tau;
    }

    /*
     * Predicate type: domain a[], range BOOL
     */
    static public int predicateType(int... a) throws Exception {
        return functionType(a, BOOL);
    }

    /*
     * ACCESSORS AND TESTS
     */

    /*
     * These return false if tau is not a valid type.
     * We don't throw exceptions or check for errors.
     */
    static public boolean isBool(int tau) {
        return Yices.typeIsBool(tau);
    }
    
    static public boolean isInt(int tau) {
        return Yices.typeIsInt(tau);
    }
    
    static public boolean isReal(int tau) {
        return Yices.typeIsReal(tau);
    }

    static public boolean isArithmetic(int tau) {
        return Yices.typeIsArithmetic(tau);
    }

    static public boolean isBitvector(int tau) {
        return Yices.typeIsBitvector(tau);
    }

    static public boolean isScalar(int tau) {
        return Yices.typeIsScalar(tau);
    }

    static public boolean isUninterpreted(int tau) {
        return Yices.typeIsUninterpreted(tau);
    }

    static public boolean isTuple(int tau) {
        return Yices.typeIsTuple(tau);
    }

    static public boolean isFunction(int tau) {
        return Yices.typeIsFunction(tau);
    }

    static public boolean isSubtype(int tau, int sigma) {
        return Yices.isSubtype(tau, sigma);
    }

    static public int bvSize(int tau) throws YicesException {
        int n = Yices.bvTypeSize(tau);
        if (n <= 0) {
            throw new YicesException();
        }
        return n;
    }

    static public int scalarCard(int tau) throws YicesException {
        int n = Yices.scalarTypeCard(tau);
        if (n <= 0) {
            throw new YicesException();
        }
        return n;
    }

    static public int numChildren(int tau) throws YicesException {
        int n = Yices.typeNumChildren(tau);
        if (n < 0) {
            throw new YicesException();
        }
        return n;
    }

    static public int child(int tau, int i) throws YicesException {
        int sigma = Yices.typeChild(tau, i);
        if (sigma < 0) {
            throw new YicesException();
        }
        return sigma;
    }

    static public int[] children(int tau) throws YicesException {
        int[] a = Yices.typeChildren(tau);
        if (a == null) {
            throw new YicesException();
        }
        return a;
    }

    /*
     * Type names
     */
    static public void setName(int tau, String name) throws YicesException {
        int code = Yices.setTypeName(tau, name);
        if (code < 0) {
            throw new YicesException();
        }
    }

    static public String getName(int tau) {
        return Yices.getTypeName(tau);
    }

    static public int getByName(String name) {
        return Yices.getTypeByName(name);
    }

    static public void removeName(String name) {
        Yices.removeTypeName(name);
    }

    /*
     * Conversion to a string
     */
    static public String toString(int tau) throws YicesException {
        String s = Yices.typeToString(tau);
        if (s == null) {
            throw new YicesException();
        }
        return s;
    }

    /*
     * Parse a string as a type
     */
    static public int parse(String s) throws YicesException {
        int tau = Yices.parseType(s);
        if (tau < 0) {
            throw new YicesException();
        }
        return tau;
    }
}

