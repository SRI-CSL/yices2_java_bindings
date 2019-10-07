package com.sri.yices;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/**
 * Minimal implementation of rationals so that we can get them out of
 * Yices. We just use two pairs of bigIntegers.
 */
public class BigRational {
    private BigInteger numerator;
    private BigInteger denominator;

    /**
     * Constructor
     * @param num = array of bytes for the numerator
     * @param den = array of bytes for the denominator
     * @throws ArithmeticException if the denominator is zero
     *
     * This constructor does not try to remove common factors.
     */
    public BigRational(byte[] num, byte[] den) {
        numerator = new BigInteger(num);
        denominator = new BigInteger(den);
        if (denominator.equals(BigInteger.ZERO)) {
            throw new ArithmeticException("Division by zero");
        }
    }

    /**
     * Constructor
     * @param num = array of bytes for the numerator
     * the denominator is set to 1.
     */
    public BigRational(byte[] num) {
        numerator = new BigInteger(num);
        denominator = BigInteger.ONE;
    }

    /**
     * Constructor from BigIntegers
     */
    public BigRational(BigInteger num, BigInteger den) {
        if (den.equals(BigInteger.ZERO)) {
            throw new ArithmeticException("Division by zero");
        }
        numerator = num;
        denominator = den;
    }

    /**
     * Constructor from a String
     */
    public BigRational(String s) {
        int i = s.indexOf('/');
        if (i < 0) {
            numerator = new BigInteger(s);
            denominator = BigInteger.ONE;
        } else if (i+1 < s.length()) {
            numerator = new BigInteger(s.substring(0,i));
            denominator = new BigInteger(s.substring(i+1,s.length()));
            normalize();
        } else {
            throw new NumberFormatException();
        }
    }

    /**
     * Constructor from a BigDecimal
     */
    static private final BigInteger ten = BigInteger.valueOf(10);

    public BigRational(BigDecimal x) {
        BigInteger u = x.unscaledValue();
        int n = x.scale();
        // x is u/10^n
        if (n >= 0) {
            numerator = u;
            denominator = ten.pow(n);
        } else {
            // -n could overflow so we compute 10 * 10^(-(n+1))
            n ++;
            numerator = u.multiply(ten.multiply(ten.pow(-n))); //  u * 10^-n
            denominator = BigInteger.ONE;
        }
    }

    /**
     * Accessors
     */
    public BigInteger getDenominator() {
        return denominator;
    }

    public BigInteger getNumerator() {
        return numerator;
    }

    /**
     * Get numerator and denominator as arrays of bytes
     */
    protected byte[] numToBytes() {
        return numerator.toByteArray();
    }

    protected byte[] denToBytes() {
        return denominator.toByteArray();
    }

    /**
     * Convert to a string
     */
    public String toString() {
	    String s = numerator.toString();
	    if (! denominator.equals(BigInteger.ONE)) {
	        s = s + "/" + denominator.toString();
	    }
	    return s;
    }

    /**
     * Remove common factors and force denominator to be positive
     */
    public void normalize() {
        BigInteger d = numerator.gcd(denominator);
        if (!d.equals(BigInteger.ONE)) {
            numerator = numerator.divide(d);
            denominator = denominator.divide(d);
        }
        if (denominator.signum() < 0) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BigRational r = (BigRational) obj;
        return denominator.equals(r.denominator) && numerator.equals(r.numerator);
    }

    public int hashCode() {
        return Objects.hash(numerator, denominator);
    }
}
