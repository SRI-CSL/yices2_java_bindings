package com.sri.yices;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.List;

/**
 * Wrappers to access the Yices term constructors.
 * These call the native API and throw a YicesException if there's an error.
 */
public class Terms {
    /**
     * Short cuts for true/false/zero/one/minus_one
     */
    static public final int NULL_TERM;
    static public final int TRUE;
    static public final int FALSE;
    static public final int ZERO;
    static public final int ONE;
    static public final int MINUS_ONE;

    static {
        NULL_TERM = -1;
        TRUE = Yices.mkTrue();
        FALSE = Yices.mkFalse();
        ZERO = Yices.zero();
        ONE = Yices.mkIntConstant(1);
        MINUS_ONE = Yices.mkIntConstant(-1);
    }

    /*
     * CONSTANT TERMS
     */

    /**
     * Boolean constants
     */
    static public int mkTrue() { return TRUE; }

    static public int mkFalse() { return FALSE; }

    static public int mkBoolConst(boolean b) { return b ? TRUE : FALSE; }

    /**
     * Constants of scalar and uninterpreted type tau
     */
    static public int mkConst(int tau, int index) throws YicesException {
        int t = Yices.mkConstant(tau, index);
        if (t < 0) throw new YicesException();
        return t;
    }

    /**
     * Bitvector constants:
     * - bvConst(n, x): convert x to an n-bit constant
     *   if n < 64, then x is truncated (i.e., only the n lower bits are used).
     *   if n > 64, then x is sign-extended to n bits
     *
     *  FIXME: add a bvConst constructor that takes a BigInteger x as value
     */
    static public int bvConst(int n, long x) throws YicesException {
        int t = Yices.bvConst(n, x);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvZero(int n) throws YicesException {
        if (n < 0) throw new IllegalArgumentException("negative bitvector size");
        int t = Yices.bvZero(n);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvOne(int n) throws YicesException {
        if (n < 0) throw new IllegalArgumentException("negative bitvector size");
        int t = Yices.bvOne(n);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvMinusOne(int n) throws YicesException {
        if (n < 0) throw new IllegalArgumentException("negative bitvector size");
        int t = Yices.bvMinusOne(n);
        if (t < 0) throw new YicesException();
        return t;
    }

    /*
     * Convert integer array a to a bitvector constant
     * - a[0] = low-order bit
     * - a[n-1] = high-order bit
     * Bit i is set if a[i] is non-zero.
     */
    static public int bvConst(int... a) throws YicesException {
        int t = Yices.bvConstFromIntArray(a);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvConst(List<Integer> a) throws YicesException {
        return bvConst(a.stream().mapToInt(Integer::intValue).toArray());
    }

    /*
     * Convert boolean array a to a bit-vector constant
     * - a[0] = low-order bit
     * - a[n-1] = high-order bit
     * where n = a.length
     */
    static public int bvConst(boolean... a) throws YicesException {
        int[] aux = new int[a.length];
        for (int i=0; i<a.length; i++) {
            aux[i] = a[i] ? 1 : 0;
        }
        return bvConst(aux);
    }

   /*
     * Parse s as a binary constant: i.e. a sequence of '0' and '1'
     * then return the corresponding bitvector constant
     */
    static public int parseBvBin(String s) throws YicesException {
        int t = Yices.parseBvBin(s);
        if (t < 0) throw new YicesException();
        return t;
    }

    /**
     * Parse s as an hexadecimal constant then convert to a bitvector constant
     * - s must be a sequence of digits '0' to '9' or 'a' to 'f' or 'A' to 'F'
     */
    static public int parseBvHex(String s) throws YicesException {
        int t = Yices.parseBvHex(s);
        if (t < 0) throw new YicesException();
        return t;
    }


    /**
     * Integer and rational constants
     */
    static public int zero() { return ZERO; }
    static public int one() { return ONE; }
    static public int minusOne() { return MINUS_ONE; }

    static public int intConst(long x) {
        return Yices.mkIntConstant(x);
    }

    static public int intConst(BigInteger x) {
        return Yices.mkIntConstant(x);
    }

    static public int rationalConst(long num, long den) throws YicesException {
        if (den < 0) {
            if (den == Long.MIN_VALUE || num == Long.MIN_VALUE) {
                throw new IllegalArgumentException("arithmetic overflow");
            }
            den = - den;
            num = - num;
        }
        int t = Yices.mkRationalConstant(num, den);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int rationalConst(BigInteger num, BigInteger den)  {
        return Yices.mkRationalConstant(num, den);
    }

    static public int rationalConst(BigRational r) {
        return Yices.mkRationalConstant(r);
    }

    static public int rationalConst(BigDecimal r) {
        return Yices.mkRationalConstant(new BigRational(r));
    }

    /**
     * Parse s as a rational number
     * Examples:
     *   "1000", "-1000", "+1000", "1/2", "-1/2". "+1/2" are all
     */
    static public int parseRational(String s) throws YicesException {
        int t = Yices.parseRational(s);
        if (t < 0) throw new YicesException();
        return t;
    }

    /**
     * Parse s as a float
     * Examples 2E+10, -2.20e-23, etc.
     */
    static public int parseFloat(String s) throws YicesException {
        int t = Yices.parseFloat(s);
        if (t < 0) throw new YicesException();
        return t;
    }


    /*
     * UNINTERPRETED TERMS AND VARIABLES
     */

    /**
     * Fresh uninterpreted term of type tau
     *
     * Variant1: uninterpreted function: the function type is defined by
     * array a (cf. Types.java).
     *
     * Variant2: uninterpreted predicate: the domain is defined by array a,
     * the range is bool
     */
    static public int newUninterpretedTerm(int tau) throws YicesException {
        int t = Yices.newUninterpretedTerm(tau);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int newUninterpretedFunction(int... a) throws YicesException {
        return newUninterpretedTerm(Types.functionType(a));
    }

    static public int newUninterpretedFunction(List<Integer> a) throws YicesException {
        return newUninterpretedFunction(a.stream().mapToInt(Integer::intValue).toArray());
    }

    static public int newUninterpretedPredicate(int... a) throws YicesException {
        return newUninterpretedTerm(Types.predicateType(a));
    }

    static public int newUninterpretedPredicate(List<Integer> a) throws YicesException {
        return newUninterpretedPredicate(a.stream().mapToInt(Integer::intValue).toArray());
    }

    /**
     * Fresh uninterpreted term of type tau, and with the associated name.
     * Variants for function and predicates.
     */
    static public int newUninterpretedTerm(String name, int tau) throws YicesException {
        int t = newUninterpretedTerm(tau);
        Yices.setTermName(t, name);
        return t;
    }

    static public int newUninterpretedFunction(String name, int... a) throws YicesException {
        int t = newUninterpretedFunction(a);
        Yices.setTermName(t, name);
        return t;
    }

    static public int newUninterpretedFunction(String name, List<Integer> a) throws YicesException {
        return newUninterpretedFunction(name, a.stream().mapToInt(Integer::intValue).toArray());
    }

    static public int newUninterpretedPredicate(String name, int... a) throws YicesException {
        int t = newUninterpretedPredicate(a);
        Yices.setTermName(t, name);
        return t;
    }

    static public int newUninterpretedPredicate(String name, List<Integer> a) throws YicesException {
        return newUninterpretedPredicate(name, a.stream().mapToInt(Integer::intValue).toArray());
    }

    /**
     * Declare a fresh uninterpreted term of type tau and with the associated name
     * + variants for functions and predicates.
     */
    static public void declareUninterpretedTerm(String name, int tau) throws YicesException {
        Yices.setTermName(newUninterpretedTerm(tau), name);
    }

    static public void declareUninterpretedFunction(String name, int ... a) throws YicesException {
        Yices.setTermName(newUninterpretedFunction(a), name);
    }

    static public void declareUninterpretedFunction(String name, List<Integer> a) throws YicesException {
        declareUninterpretedFunction(name, a.stream().mapToInt(Integer::intValue).toArray());
    }

    static public void declareUninterpretedPredicate(String name, int... a) throws YicesException {
        Yices.setTermName(newUninterpretedPredicate(a), name);
    }

    static public void declareUninterpretedPredicate(String name, List<Integer> a) throws YicesException {
        declareUninterpretedPredicate(name, a.stream().mapToInt(Integer::intValue).toArray());
    }

    /**
     * Fresh variable of type tau
     * - variables are used in lambdas/forall/exists + term substitutions
     */
    static public int newVariable(int tau) throws YicesException {
        int t = Yices.newVariable(tau);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int newVariable(String name, int tau) throws YicesException {
        int t = newVariable(tau);
        Yices.setTermName(t, name);
        return t;
    }

    static public void declareVariable(String name, int tau) throws YicesException {
        Yices.setTermName(newVariable(tau), name);
    }


    /*
     * TERM CONSTRUCTORS
     */

    /**
     * General constructors
     */
    static public int ifThenElse(int cond, int iftrue, int iffalse) throws YicesException {
        int t = Yices.ifThenElse(cond, iftrue, iffalse);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int eq(int left, int right) throws YicesException {
        int t = Yices.eq(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int neq(int left, int right) throws YicesException {
        int t = Yices.neq(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int distinct(int... arg) throws YicesException {
        int t = Yices.distinct(arg);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int distinct(List<Integer> args) throws YicesException {
        return distinct(args.stream().mapToInt(Integer::intValue).toArray());
    }


    static public int forall(int[] vars, int body) throws YicesException {
        int t = Yices.forall(vars, body);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int forall(List<Integer> varlist, int body) throws YicesException {
        int[] vars =  varlist.stream().mapToInt(Integer::intValue).toArray();
        int t = Yices.forall(vars, body);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int exists(int[] vars, int body) throws YicesException {
        int t = Yices.exists(vars, body);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int exists(List<Integer> varlist, int body) throws YicesException {
        int[] vars =  varlist.stream().mapToInt(Integer::intValue).toArray();
        int t = Yices.exists(vars, body);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int lambda(int[] vars, int body) throws YicesException {
        int t = Yices.lambda(vars, body);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int lambda(List<Integer> varlist, int body) throws YicesException {
        int[] vars =  varlist.stream().mapToInt(Integer::intValue).toArray();
        int t = Yices.lambda(vars, body);
        if (t < 0) throw new YicesException();
        return t;
    }

    /**
     * Tuple operations
     */
    static public int tuple(int... arg) throws YicesException {
        int t = Yices.tuple(arg);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int tuple(List<Integer> arg) throws YicesException {
        return tuple(arg.stream().mapToInt(Integer::intValue).toArray());
    }

    static public int select(int idx, int tuple) throws YicesException {
        int t = Yices.select(idx, tuple);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int tupleUpdate(int tuple, int idx, int newval) throws YicesException {
        int t = Yices.tupleUpdate(tuple, idx, newval);
        if (t < 0) throw new YicesException();
        return t;
    }

    /**
     * Uninterpreted functions/arrays
     */
    static public int funApplication(int fun, int... arg) throws YicesException {
        int t = Yices.funApplication(fun, arg);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int funApplication(int fun, List<Integer> arg) throws YicesException {
        return funApplication(fun, arg.stream().mapToInt(Integer::intValue).toArray());
    }

    static public int functionUpdate(int fun, int[] arg, int newval) throws YicesException {
        int t = Yices.functionUpdate(fun, arg, newval);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int functionUpdate(int fun, List<Integer> arg, int newval) throws YicesException {
        int t = Yices.functionUpdate(fun, arg.stream().mapToInt(Integer::intValue).toArray(), newval);
        if (t < 0) throw new YicesException();
        return t;
    }

    // Update1 is the common case where arg[] is a single argument
    static public int functionUpdate1(int fun, int arg, int newval) throws YicesException {
        int t = Yices.functionUpdate1(fun, arg, newval);
        if (t < 0) throw new YicesException();
        return t;
    }

    /**
     * Boolean terms
     */
    static public int not(int arg) throws YicesException {
        int t = Yices.not(arg);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int and(int... arg) throws YicesException {
        int t = Yices.and(arg);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int and(List<Integer> arg) throws YicesException {
        return and(arg.stream().mapToInt(Integer::intValue).toArray());
    }

    static public int or(int... arg) throws YicesException {
        int t = Yices.or(arg);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int or(List<Integer> arg) throws YicesException {
        return or(arg.stream().mapToInt(Integer::intValue).toArray());
    }


    static public int xor(int... arg) throws YicesException {
        int t = Yices.xor(arg);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int xor(List<Integer> arg) throws YicesException {
        return xor(arg.stream().mapToInt(Integer::intValue).toArray());
    }


    static public int iff(int left, int right) throws YicesException {
        int t = Yices.iff(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int implies(int left, int right) throws YicesException {
        int t = Yices.implies(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }


    /**
     * Arithmetic terms
     */
    static public int add(int left, int right) throws YicesException {
        int t = Yices.add(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int sub(int left, int right) throws YicesException {
        int t = Yices.sub(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    // unary minus
    static public int neg(int arg) throws YicesException {
        int t = Yices.neg(arg);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int mul(int left, int right) throws YicesException {
        int t = Yices.mul(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int square(int arg) throws YicesException {
        int t = Yices.square(arg);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int power(int arg, int exponent) throws YicesException {
        if (exponent < 0) throw new IllegalArgumentException("exponent can't be negative");
        int t = Yices.power(arg, exponent);
        if (t < 0) throw new YicesException();
        return t;
    }

    // sum of all elements of arg
    static public int add(int... arg) throws YicesException {
        int t = Yices.add(arg);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int add(List<Integer> arg) throws YicesException {
        return add(arg.stream().mapToInt(Integer::intValue).toArray());
    }

    // product of all elements of arg
    static public int mul(int... arg) throws YicesException {
        int t = Yices.mul(arg);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int mul(List<Integer> arg) throws YicesException {
        return mul(arg.stream().mapToInt(Integer::intValue).toArray());
    }


    // real division: x/y
    static public int div(int x, int y) throws YicesException {
        int t = Yices.div(x, y);
        if (t < 0) throw new YicesException();
        return t;
    }

    // integer division:
    static public int idiv(int x, int y) throws YicesException {
        int t = Yices.idiv(x, y);
        if (t < 0) throw new YicesException();
        return t;
    }

    // remainder in integer division
    static public int imod(int x, int y) throws YicesException {
        int t = Yices.imod(x, y);
        if (t < 0) throw new YicesException();
        return t;
    }

    // absolute value
    static public int abs(int x) throws YicesException {
        int t = Yices.abs(x);
        if (t < 0) throw new YicesException();
        return t;
    }

    // floor and ceiling
    static public int floor(int x) throws YicesException {
        int t = Yices.floor(x);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int ceil(int x) throws YicesException {
        int t = Yices.ceil(x);
        if (t < 0) throw new YicesException();
        return t;
    }

    // polynomial: sum coeff[i] * t[i]
    static public int intPoly(long[] coeff, int[] t) throws YicesException {
        if (coeff.length != t.length)
            throw new IllegalArgumentException("coeff and term arrays must have the same length");
        int term = Yices.intPoly(coeff, t);
        if (term < 0) throw new YicesException();
        return term;
    }

    static public int intPoly(List<Long> coeff, List<Integer> t) throws YicesException {
        long[] acoeff =  coeff.stream().mapToLong(Long::longValue).toArray();
        int[] at =  t.stream().mapToInt(Integer::intValue).toArray();
        return intPoly(acoeff, at);
    }


    // sum of num[i]/den[i] * t[i]
    static public int rationalPoly(long[] num, long[] den, int[] t) throws YicesException {
        if (num.length != den.length || num.length != t.length)
            throw new IllegalArgumentException("coeff and term arrays must have the same length");
        int term = Yices.rationalPoly(num, den, t);
        if (term < 0) throw new YicesException();
        return term;
    }

    static public int rationalPoly(List<Long> num, List<Long> den, List<Integer> t) throws YicesException {
        long[] anum =  num.stream().mapToLong(Long::longValue).toArray();
        long[] aden =  den.stream().mapToLong(Long::longValue).toArray();
        int[] at =  t.stream().mapToInt(Integer::intValue).toArray();
        return rationalPoly(anum, aden, at);
    }

    // arithmetic atoms
    static public int divides(int x, int y) throws YicesException {
        int t = Yices.divides(x, y);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int isInt(int x) throws YicesException {
        int t = Yices.isInt(x);
        if (t < 0) throw new YicesException();
        return t;
    }

    // (x == y)
    static public int arithEq(int x, int y) throws YicesException {
        int t = Yices.arithEq(x, y);
        if (t < 0) throw new YicesException();
        return t;
    }

    // (x != y)
    static public int arithNeq(int x, int y) throws YicesException {
        int t = Yices.arithNeq(x, y);
        if (t < 0) throw new YicesException();
        return t;
    }

    // (x >= y)
    static public int arithGeq(int x, int y) throws YicesException {
        int t = Yices.arithGeq(x, y);
        if (t < 0) throw new YicesException();
        return t;
    }

    // (x <= y)
    static public int arithLeq(int x, int y) throws YicesException {
        int t = Yices.arithLeq(x, y);
        if (t < 0) throw new YicesException();
        return t;
    }

    // (x > y)
    static public int arithGt(int x, int y) throws YicesException {
        int t = Yices.arithGt(x, y);
        if (t < 0) throw new YicesException();
        return t;
    }

    // (x < y)
    static public int arithLt(int x, int y) throws YicesException {
        int t = Yices.arithLt(x, y);
        if (t < 0) throw new YicesException();
        return t;
    }

    // (x == 0)
    static public int arithEq0(int x) throws YicesException {
        int t = Yices.arithEq0(x);
        if (t < 0) throw new YicesException();
        return t;
    }

    // (x != 0)
    static public int arithNeq0(int x) throws YicesException {
        int t = Yices.arithNeq0(x);
        if (t < 0) throw new YicesException();
        return t;
    }

    // (x >= 0)
    static public int arithGeq0(int x) throws YicesException {
        int t = Yices.arithGeq0(x);
        if (t < 0) throw new YicesException();
        return t;
    }

    // (x <= 0)
    static public int arithLeq0(int x) throws YicesException {
        int t = Yices.arithLeq0(x);
        if (t < 0) throw new YicesException();
        return t;
    }

    // (x > 0)
    static public int arithGt0(int x) throws YicesException {
        int t = Yices.arithGt0(x);
        if (t < 0) throw new YicesException();
        return t;
    }

    // (x < 0)
    static public int arithLt0(int x) throws YicesException {
        int t = Yices.arithLt0(x);
        if (t < 0) throw new YicesException();
        return t;
    }

    /**
     * Bitvector terms
     */
    static public int bvAdd(int left, int right) throws YicesException {
        int t = Yices.bvAdd(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvSub(int left, int right) throws YicesException {
        int t = Yices.bvSub(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    // 2s complement negation
    static public int bvNeg(int arg) throws YicesException {
        int t = Yices.bvNeg(arg);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvMul(int left, int right) throws YicesException {
        int t = Yices.bvMul(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvSquare(int arg) throws YicesException {
        int t = Yices.bvSquare(arg);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvPower(int arg, int exponent) throws YicesException {
        if (exponent < 0) throw new IllegalArgumentException("exponent can't be negative");
        int t = Yices.bvPower(arg, exponent);
        if (t < 0) throw new YicesException();
        return t;
    }

    // unsigned division
    static public int bvDiv(int left, int right) throws YicesException {
        int t = Yices.bvDiv(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvRem(int left, int right) throws YicesException {
        int t = Yices.bvRem(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    // signed division
    static public int bvSDiv(int left, int right) throws YicesException {
        int t = Yices.bvSDiv(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvSRem(int left, int right) throws YicesException {
        int t = Yices.bvSRem(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvSMod(int left, int right) throws YicesException {
        int t = Yices.bvSMod(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    // bitwise operations
    static public int bvNot(int arg) throws YicesException {
        int t = Yices.bvNot(arg);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvAnd(int left, int right) throws YicesException {
        int t = Yices.bvAdd(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvOr(int left, int right) throws YicesException {
        int t = Yices.bvOr(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvXor(int left, int right) throws YicesException {
        int t = Yices.bvXor(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvNand(int left, int right) throws YicesException {
        int t = Yices.bvNand(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvNor(int left, int right) throws YicesException {
        int t = Yices.bvNor(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvXNor(int left, int right) throws YicesException {
        int t = Yices.bvXNor(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    // shift operations: the left operand is shifted. The right operand
    // gives the shift amount.
    static public int bvShl(int left, int right) throws YicesException {
        int t = Yices.bvShl(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvLshr(int left, int right) throws YicesException {
        int t = Yices.bvLshr(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvAshr(int left, int right) throws YicesException {
        int t = Yices.bvAshr(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    // n-ary variants for the most common associative operations
    static public int bvAdd(int... arg) throws YicesException {
        if (arg.length == 0) throw new IllegalArgumentException("empty input");
        int t = Yices.bvAdd(arg);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvAdd(List<Integer> arg) throws YicesException {
        return bvAdd(arg.stream().mapToInt(Integer::intValue).toArray());
    }

    static public int bvAnd(int... arg) throws YicesException {
        if (arg.length == 0) throw new IllegalArgumentException("empty input");
        int t = Yices.bvAnd(arg);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvAnd(List<Integer> arg) throws YicesException {
        return bvAnd(arg.stream().mapToInt(Integer::intValue).toArray());
    }

    static public int bvOr(int... arg) throws YicesException {
        if (arg.length == 0) throw new IllegalArgumentException("empty input");
        int t = Yices.bvOr(arg);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvOr(List<Integer> arg) throws YicesException {
        return bvOr(arg.stream().mapToInt(Integer::intValue).toArray());
    }

    static public int bvXor(int... arg) throws YicesException {
        if (arg.length == 0) throw new IllegalArgumentException("empty input");
        int t = Yices.bvXor(arg);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvXor(List<Integer> arg) throws YicesException {
        return bvXor(arg.stream().mapToInt(Integer::intValue).toArray());
    }

    // shift by constants: n is a constant shift amount
    static public int bvShiftLeft0(int arg, int n) throws YicesException {
        if (n < 0) throw new IllegalArgumentException("shift amount can't be negative");
        int t = Yices.bvShiftLeft0(arg, n);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvShiftLeft1(int arg, int n) throws YicesException {
        if (n < 0) throw new IllegalArgumentException("shift amount can't be negative");
        int t = Yices.bvShiftLeft1(arg, n);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvShiftRight0(int arg, int n) throws YicesException {
        if (n < 0) throw new IllegalArgumentException("shift amount can't be negative");
        int t = Yices.bvShiftRight0(arg, n);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvShiftRight1(int arg, int n) throws YicesException {
        if (n < 0) throw new IllegalArgumentException("shift amount can't be negative");
        int t = Yices.bvShiftRight1(arg, n);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvAShiftRight(int arg, int n) throws YicesException {
        if (n < 0) throw new IllegalArgumentException("shift amount can't be negative");
        int t = Yices.bvAShiftRight(arg, n);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvRotateLeft(int arg, int n) throws YicesException {
        if (n < 0) throw new IllegalArgumentException("shift amount can't be negative");
        int t = Yices.bvRotateLeft(arg, n);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvRotateRight(int arg, int n) throws YicesException {
        if (n < 0) throw new IllegalArgumentException("shift amount can't be negative");
        int t = Yices.bvRotateRight(arg, n);
        if (t < 0) throw new YicesException();
        return t;
    }

    // extract a[i:j] from a[0 .. n-1] where n = size of a
    static public int bvExtract(int a, int i, int j) throws YicesException {
        if (i < 0 || j < 0) throw new IllegalArgumentException("negative bit-vector index");
        int t = Yices.bvExtract(a, i, j);
        if (t < 0) throw new YicesException();
        return t;
    }

    // extract bit i from a[0 ... n-1]: the result is a Boolean term
    static public int bvExtractBit(int a, int i) throws YicesException {
        if (i < 0) throw new IllegalArgumentException("negative bit-vector index");
        int t = Yices.bvExtractBit(a, i);
        if (t < 0) throw new YicesException();
        return t;
    }

    // convert an array of boolean terms into a bitvector
    // a[0] = low-order bit of the result, a[n-1] = high-order bit
    static public int bvFromBoolArray(int... a) throws YicesException {
        if (a.length == 0) throw new IllegalArgumentException("empty boolean array");
        int t = Yices.bvFromBoolArray(a);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvFromBoolArray(List<Integer> a) throws YicesException {
        return bvFromBoolArray(a.stream().mapToInt(Integer::intValue).toArray());
    }

    // concat: high-order bits are from the left
    static public int bvConcat(int left, int right) throws YicesException {
        int t = Yices.bvConcat(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }
    static public int bvConcat(int... a) throws YicesException {
        if (a.length == 0) throw new IllegalArgumentException("empty input");
        int t = Yices.bvConcat(a);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvConcat(List<Integer> a) throws YicesException {
        return bvConcat(a.stream().mapToInt(Integer::intValue).toArray());
    }

    // n copies of a concatenated
    static public int bvRepeat(int a, int n) throws YicesException {
        if (n <= 0) throw new IllegalArgumentException("n must be positive");
        int t = Yices.bvRepeat(a, n);
        if (t < 0) throw new YicesException();
        return t;
    }

    // add n bits
    static public int bvSignExtend(int arg, int n) throws YicesException {
        if (n <= 0) throw new IllegalArgumentException("n must be positive");
        int t = Yices.bvSignExtend(arg, n);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvZeroExtend(int arg, int n) throws YicesException {
        if (n <= 0) throw new IllegalArgumentException("n must be positive");
        int t = Yices.bvZeroExtend(arg, n);
        if (t < 0) throw new YicesException();
        return t;
    }

    // obscure operations
    static public int bvRedAnd(int arg) throws YicesException {
        int t = Yices.bvRedAnd(arg);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvRedOr(int arg) throws YicesException {
        int t = Yices.bvRedOr(arg);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvRedComp(int left, int right) throws YicesException {
        int t = Yices.bvRedComp(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    // Atoms
    static public int bvEq(int left, int right) throws YicesException {
        int t = Yices.bvEq(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int bvNeq(int left, int right) throws YicesException {
        int t = Yices.bvNeq(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    // unsigned comparison: (left >= right)
    static public int bvGe(int left, int right) throws YicesException {
        int t = Yices.bvGe(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    // (left > right)
    static public int bvGt(int left, int right) throws YicesException {
        int t = Yices.bvGt(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    // (left <= right)
    static public int bvLe(int left, int right) throws YicesException {
        int t = Yices.bvLe(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    // (left < right)
    static public int bvLt(int left, int right) throws YicesException {
        int t = Yices.bvLt(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }


    // signed comparison: (left >= right)
    static public int bvSGe(int left, int right) throws YicesException {
        int t = Yices.bvSGe(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    // (left > right)
    static public int bvSGt(int left, int right) throws YicesException {
        int t = Yices.bvSGt(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    // (left <= right)
    static public int bvSLe(int left, int right) throws YicesException {
        int t = Yices.bvSLe(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    // (left < right)
    static public int bvSLt(int left, int right) throws YicesException {
        int t = Yices.bvSLt(left, right);
        if (t < 0) throw new YicesException();
        return t;
    }

    /**
     * ACCESSORS AND CHECKS ON TERMS
     */

    /*
     * Type of a term
     */
    static public int typeOf(int x) throws YicesException {
        int tau = Yices.typeOfTerm(x);
        if (tau < 0) throw new YicesException();
        return tau;
    }

    static public boolean isBool(int x) {
        return Yices.termIsBool(x);
    }

    // check whether x has type integer
    static public boolean isInteger(int x) {
        return Yices.termIsInt(x);
    }

    static public boolean isReal(int x) {
        return Yices.termIsReal(x);
    }

    static public boolean isArithmetic(int x) {
        return Yices.termIsArithmetic(x);
    }

    static public boolean isBitvector(int x) {
        return Yices.termIsBitvector(x);
    }

    // does x have type (tuple ...)
    static public boolean isTuple(int x) {
        return Yices.termIsTuple(x);
    }

    // does x have a function type
    static public boolean isFunction(int x) {
        return Yices.termIsFunction(x);
    }

    // does x have a scalar or uninterpreted term
    static public boolean isScalar(int x) {
        return Yices.termIsScalar(x);
    }

    // number of bits in term x.
    static public int bitSize(int x) throws YicesException {
        int n = Yices.termBitSize(x);
        if (n == 0) throw new YicesException();
        return n;
    }

    // check whether x is a term without free variables
    static public boolean isGround(int x) {
        return Yices.termIsGround(x);
    }

    /*
     * Term structure
     */
    static public boolean isAtomic(int x) {
        return Yices.termIsAtomic(x);
    }

    static public boolean isComposite(int x) {
        return Yices.termIsComposite(x);
    }

    static public boolean isProjection(int x) {
        return Yices.termIsProjection(x);
    }

    static public boolean isSum(int x) {
        return Yices.termIsSum(x);
    }

    static public boolean isBvSum(int x) {
        return Yices.termIsBvSum(x);
    }

    static public boolean isProduct(int x) {
        return Yices.termIsProduct(x);
    }

    // Constructor of term x
    static public Constructor constructor(int x) {
        return Constructor.idToConstructor(Yices.termConstructor(x));
    }

    static public int numChildren(int x) throws YicesException {
        int n = Yices.termNumChildren(x);
        if (n < 0) throw new YicesException();
        return n;
    }

    static public int[] children(int x) throws YicesException {
        int[] children = Yices.termChildren(x);
        if (children == null) throw new YicesException();
        return children;
    }

    static public int child(int x, int idx) throws YicesException {
        int t = Yices.termChild(x, idx);
        if (t < 0) throw new YicesException();
        return t;
    }

    static public int projIndex(int x) throws YicesException {
        int idx = Yices.termProjIndex(x);
        if (idx < 0) throw new YicesException();
        return idx;
    }

    static public int projArg(int x) throws YicesException {
        int t = Yices.termProjArg(x);
        if (t < 0) throw new YicesException();
        return t;
    }


    /*
     * Check whether term x is a constant
     */
    static public boolean isBoolConstant(int x) throws YicesException {
        return constructor(x) == Constructor.BOOL_CONSTANT;
    }

    static public boolean isArithConstant(int x) throws YicesException {
        return constructor(x) == Constructor.ARITH_CONSTANT;
    }

    static public boolean isBvConstant(int x) throws YicesException {
        return constructor(x) == Constructor.BV_CONSTANT;
    }

    static public boolean isScalarConstant(int x) throws YicesException {
        return constructor(x) == Constructor.SCALAR_CONSTANT;
    }

    static public boolean isUninterpreted(int x) throws YicesException {
        return constructor(x) == Constructor.UNINTERPRETED_TERM;
    }

    /*
     * Value of a constant term
     */
    static public boolean boolConstValue(int x) throws YicesException {
        int b = Yices.boolConstValue(x);
        if (b < 0) throw new YicesException();
        return (b != 0);
    }

    static public int scalarConstantIndex(int x) throws YicesException {
        int idx = Yices.scalarConstantIndex(x);
        if (idx < 0) throw new YicesException();
        return idx;
    }

    // value of x returned as an array of n Booleans little-endian
    static public boolean[] bvConstValue(int x) throws YicesException {
        boolean[] b = Yices.bvConstValue(x);
        if (b == null) throw new YicesException();
        return b;
    }

    // value of an arithmetic constant returned as a BigRational
    static public BigRational arithConstValue(int x) throws YicesException {
        BigRational r = Yices.rationalConstValue(x);
        if (r == null) throw new YicesException();
        return r;
    }

    // value of an arithmetic constant, converted to long or int
    // throws an exception if the conversion looses information
    static public long arithConstLongValue(int x) throws YicesException {
        BigRational r = arithConstValue(x);
        if (!r.fitsLong()) {
            throw new IllegalArgumentException("Yices constant can't be converted to long");
        }
        return r.longValue();
    }

    static public int arithConstIntValue(int x) throws YicesException {
        BigRational r = arithConstValue(x);
        if (!r.fitsInt()) {
            throw new IllegalArgumentException("Yices constant can't be converted to int");
        }
        return r.intValue();
    }

    /*
     * Names
     */
    static public void setName(int t, String name) throws YicesException {
        int code = Yices.setTermName(t, name);
        if (code < 0)
            throw new YicesException();
    }

    static public String getName(int t) {
        return Yices.getTermName(t);
    }

    static public int getByName(String name) {
        return Yices.getTermByName(name);
    }

    static public void removeName(String name) {
        Yices.removeTermName(name);
    }

    // Pretty print t then convert to a String: use a box of 80 columns and 30 lines
    static public String toString(int t) throws YicesException {
        String s = Yices.termToString(t);
        if (s == null) throw new YicesException();
        return s;
    }

    static public String toString(int t, int numColumns, int numLines) throws YicesException {
        String s = Yices.termToString(t, numColumns, numLines);
        if (s == null) throw new YicesException();
        return s;
    }

    // Parsing of a term (Yices syntax)
    static public int parse(String s) throws YicesException {
        int t = Yices.parseTerm(s);
        if (t < 0) throw new YicesException();
        return t;
    }

    /*
     * Substitutions
     *
     * A substitution is defined by two arrays: v = variable array, map = term array.
     * They must have the same length.
     *
     * substTerm(t, v, map): apply the substitution to t.
     * substTermArray(a, v, map): apply the substitution to all elements of array a.
     * If there's an error, a is unchanged.
     */
     static public int subst(int t, int[] v, int[] map) throws YicesException {
         if (v.length != map.length) throw new IllegalArgumentException("bad substitution");
         int w = Yices.substTerm(t, v, map);
         if (w < 0) throw new YicesException();
         return w;
     }

     static public int subst(int t, List<Integer> v, List<Integer> map) throws YicesException {
        int[] av =  v.stream().mapToInt(Integer::intValue).toArray();
        int[] amap =  map.stream().mapToInt(Integer::intValue).toArray();
        return subst(t, av, amap);
     }

     static public void substArray(int[] a, int[] v, int[] map) throws YicesException {
         if (v.length != map.length) throw new IllegalArgumentException("bad substitution");
         int code = Yices.substTermArray(a, v, map);
         if (code < 0) throw new YicesException();
     }

    static public void substArray(List<Integer> a, List<Integer> v, List<Integer> map) throws YicesException {
        int[] aa =  a.stream().mapToInt(Integer::intValue).toArray();
        int[] av =  v.stream().mapToInt(Integer::intValue).toArray();
        int[] amap =  map.stream().mapToInt(Integer::intValue).toArray();
        substArray(aa, av, amap);
        // copy the result back out  into the input.
        for(int i = 0; i < aa.length; i++){
            a.set(i, aa[i]);
        }
    }
}
