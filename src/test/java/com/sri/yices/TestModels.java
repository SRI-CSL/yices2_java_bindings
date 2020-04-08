package com.sri.yices;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assume.assumeTrue;

public class TestModels {

    @Test
    public void testInt() {
        // JUnit runner treats tests with failing assumptions as ignored
        assumeTrue(TestAssumptions.IS_YICES_INSTALLED);

        int x = Terms.newUninterpretedTerm("x", Types.INT);
        int y = Terms.newUninterpretedTerm("y", Types.INT);
        Context c = new Context();
        c.assertFormula(Terms.parse("(> x y)"));
        Status stat = c.check();
        Assert.assertEquals(stat, Status.SAT);
        Model m = c.getModel();
        System.out.println("Model for (> x y)");
        System.out.println(m);
        System.out.format("x = %s\n", m.integerValue(x));
        System.out.format("y = %s\n", m.integerValue(y));
        YVal yval = m.getValue(x);
        Assert.assertEquals(yval.tag, YValTag.RATIONAL);
        Assert.assertEquals(m.integerValue(x), m.integerValue(m.getValue(x)));
        Terms.removeName("x");
        Terms.removeName("y");
        System.out.println();
    }

    @Test
    public void testTuple() {
        int tau = Types.tupleType(Types.BOOL, Types.REAL, Types.INT);
        int t1 = Terms.newUninterpretedTerm("t1", tau);
        Context c = new Context();
        String fmla = "(ite (select t1 1) (< (select t1 2) (select t1 3)) (> (select t1 2) (select t1 3)))";
        c.assertFormula(Terms.parse(fmla));
        Status stat = c.check();
        Assert.assertEquals(stat, Status.SAT);
        Model m = c.getModel();
        System.out.println("Model for " + fmla);
        System.out.println(m);
        YVal yval = m.getValue(t1);
        Assert.assertEquals(yval.tag, YValTag.TUPLE);
        YVal[] yvals = m.expandTuple(yval);
        Assert.assertEquals(yvals.length, 3);
        Assert.assertEquals(yvals[0].tag, YValTag.BOOL);
        Assert.assertEquals(yvals[1].tag, YValTag.RATIONAL);
        Assert.assertEquals(yvals[2].tag, YValTag.RATIONAL);
        Assert.assertEquals(m.boolValue(yvals[0]), false);
        Assert.assertEquals(m.integerValue(yvals[1]), 1);
        Assert.assertEquals(m.integerValue(yvals[2]), 0);
    }



}
