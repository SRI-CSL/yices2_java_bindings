package com.sri.yices;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assume.assumeTrue;

public class TestContext {
    @Test
    public void test1() {
        // JUnit runner treats tests with failing assumptions as ignored
        assumeTrue(Yices.isReady());

        int x = Terms.newUninterpretedTerm("x", Types.REAL);
        int y = Terms.newUninterpretedTerm("y", Types.REAL);
        Context c = new Context();
        c.assertFormula(Terms.arithGt(x, y)); // x > y
        Status stat = c.check();
        Assert.assertEquals(stat, Status.SAT);
        Model m = c.getModel();
        System.out.println("Model for (x > y)");
        System.out.println(m);
        Terms.removeName("x");
        Terms.removeName("y");
        System.out.println();
    }


    static String boolArray(boolean[] b) {
        String s = "[";
        for (int i=0; i<b.length; i++) {
            if (i > 0) s += " ";
            char c = b[i] ? 't' : 'f';
            s += c;
        }
        s += "]";
        return s;
    }

    static void bitvector_test(int n) {
        // JUnit runner treats tests with failing assumptions as ignored
        assumeTrue(Yices.isReady());

        int tau = Types.bvType(n);
        int x = Terms.newUninterpretedTerm("x", tau);
        int y = Terms.newUninterpretedTerm("y", tau);
        int z = Terms.newUninterpretedTerm("z", tau);
        int f = Terms.parse("(and (bv-gt x z) (bv-gt y z) (bv-lt (bv-mul (bv-add x y) z) z))");

        System.out.println("Testing " + Terms.toString(f) + " nbits = " + n);
        Context c = new Context("QF_BV");
        c.assertFormula(f);
        Status stat = c.check(10);
        System.out.println("Status: " + stat);
        if (stat == Status.SAT) {
            Model m = c.getModel();
            System.out.println("model");
            System.out.println(m);
            boolean[] zval = m.bvValue(z);
            boolean[] xval = m.bvValue(x);
            boolean[] yval = m.bvValue(y);
            boolean[] sumval = m.bvValue(Terms.bvAdd(x,y));
            System.out.println("Value of z: " + boolArray(zval));
            System.out.println("Value of x: " + boolArray(xval));
            System.out.println("Value of y: " + boolArray(yval));
            System.out.println("Value of x + y: " + boolArray(sumval));
        }

        Terms.removeName("x");
        Terms.removeName("y");
        Terms.removeName("z");
        System.out.println();
    }

    @Test
    public void test2() {
       // JUnit runner treats tests with failing assumptions as ignored
       assumeTrue(Yices.isReady());

       bitvector_test(1);
       bitvector_test(2);
       bitvector_test(6);
       bitvector_test(32);
       bitvector_test(256);
    }

    static void bitvectorFactor(long product, int nbits) {
        int tau = Types.bvType(nbits);
        int a = Terms.newUninterpretedTerm("a", tau);
        int b = Terms.newUninterpretedTerm("b", tau);
        int p = Terms.bvConst(nbits, product);
        System.out.println("Factoring " + product + ", nbits = " + nbits);

        Context c = new Context("QF_BV");
        Parameters param = new Parameters();
        param.defaultsForContext(c);

        // constraints: a * b = p, 1 < a <= b < p.
        c.assertFormula(Terms.bvEq(p, Terms.bvMul(a, b)));
        c.assertFormula(Terms.bvGt(a, Terms.bvOne(nbits)));
        c.assertFormula(Terms.bvGe(b, a));
        c.assertFormula(Terms.bvLt(b, p));

        Status status;
        do {
            status = c.check(param);
            System.out.println("Status: " + status);
            if (status == Status.SAT) {
                Model m = c.getModel();
                int t1 = m.valueAsTerm(a);
                int t2 = m.valueAsTerm(b);
                System.out.println("solution: ");
                System.out.println("a = " + Terms.toString(t1));
                System.out.println("b = " + Terms.toString(t2));
                c.assertBlockingClause();
            }
        } while (status == Status.SAT);

        System.out.println();
    }

    @Test
    public void test3() {
        // JUnit runner treats tests with failing assumptions as ignored
        assumeTrue(Yices.isReady());

        // Skipped tests with 9 and 10 bits. They have too many solutions
        // bitvectorFactor(17 * 23, 9);
        // bitvectorFactor(17 * 23, 10);
        bitvectorFactor(17 * 23, 11);
        bitvectorFactor(17 * 23, 12);
        bitvectorFactor(17 * 23, 13);
        bitvectorFactor(17 * 23, 14);
        bitvectorFactor(17 * 23, 15);
        bitvectorFactor(17 * 23, 32);

        // 1009 is prime
        bitvectorFactor(1009, 32);
    }
}
