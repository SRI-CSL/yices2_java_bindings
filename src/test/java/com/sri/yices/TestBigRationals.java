package com.sri.yices;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.Assume.assumeTrue;

public class TestBigRationals {
    @Test
    public void testConstructors() {
        // JUnit runner treats tests with failing assumptions as ignored
        assumeTrue(Assumptions.IS_YICES_INSTALLED);

        BigInteger three = BigInteger.valueOf(3);
        BigInteger minus_one = BigInteger.valueOf(-1);
        BigInteger ten = BigInteger.valueOf(10);

        BigRational q = new BigRational("-1/3");
        System.out.println("big rational q: " + q);
        Assert.assertEquals(q.getDenominator(), three);
        Assert.assertEquals(q.getNumerator(), minus_one);

        BigDecimal d = BigDecimal.valueOf(3, -2);
        q = new BigRational(d);
        System.out.println("big rational from " + d + " = " + q);
        Assert.assertEquals(q.getDenominator(), BigInteger.ONE);
        Assert.assertEquals(q.getNumerator(), BigInteger.valueOf(300));

        d = BigDecimal.valueOf(-1,25);
        q = new BigRational(BigDecimal.valueOf(-1,25));
        System.out.println("big rational from " + d + " = " + q);
        Assert.assertEquals(q.getNumerator(), minus_one);
        Assert.assertEquals(q.getDenominator(), ten.pow(25));

        BigRational q2 = new BigRational(minus_one, ten.pow(25));
        Assert.assertEquals(q, q2);
    }
}
