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
        try (Context c = new Context()){
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
    }

    @Test
    public void testTuple() {
        int tau = Types.tupleType(Types.BOOL, Types.REAL, Types.INT);
        int t1 = Terms.newUninterpretedTerm("t1", tau);
        try(Context c = new Context()){
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
            Terms.removeName("t1");
        }
    }

    @Test
    public void testScalar() {
        int tau = Types.newScalarType("s10", 10);
        int s1 = Terms.newUninterpretedTerm("s1", tau);
        int s2 = Terms.newUninterpretedTerm("s2", tau);
        int s3 = Terms.newUninterpretedTerm("s3", tau);
        try (Context c = new Context()){
            c.assertFormula(Terms.parse("(/= s1 s2)"));
            c.assertFormula(Terms.parse("(/= s1 s3)"));
            Status stat = c.check();
            Assert.assertEquals(stat, Status.SAT);
            Model m = c.getModel();
            System.out.println("Model for (and (/= s1 s2) (/= s1 s3))");
            System.out.println(m);
            YVal y1 = m.getValue(s1);
            Assert.assertEquals(y1.tag, YValTag.SCALAR);
            int[] vals = {0, 0};
            Assert.assertTrue(m.scalarValue(y1, vals));
            Assert.assertEquals(vals[0], m.scalarValue(s1));
            Assert.assertEquals(vals[1], tau);
            Terms.removeName("s1");
            Terms.removeName("s2");
            Terms.removeName("s3");
            Types.removeName("s10");
        }

    }

    @Test
    public void testFunction() {
        int tau = Types.functionType(Types.INT, Types.BOOL, Types.REAL, Types.REAL);
        String tauStr = Types.toString(tau);
        Assert.assertEquals(tauStr, "(-> int bool real real)");
        System.out.println(tauStr);

        int f = Terms.newUninterpretedTerm("f", tau);
        int i = Terms.newUninterpretedTerm("i", Types.INT);
        int b = Terms.newUninterpretedTerm("b", Types.BOOL);
        int r = Terms.newUninterpretedTerm("r", Types.REAL);

        try (Context c = new Context()){
            String fmla = "(> (f i b r) (f (+ i 1) (not b) (- r i)))";
            c.assertFormula(Terms.parse(fmla));
            Status stat = c.check();
            Assert.assertEquals(stat, Status.SAT);
            Model m = c.getModel();
            System.out.println("Model for " + fmla);
            System.out.println(m);
            YVal yval = m.getValue(f);
            Assert.assertEquals(yval.tag, YValTag.FUNCTION);
            VectorValue yvv = m.expandFunction(yval);

            YVal[] mappings = yvv.vector;
            YVal def =  yvv.value;
            Assert.assertEquals(def.tag, YValTag.RATIONAL);
            Assert.assertEquals(yvv.vector.length, 2);
            Assert.assertEquals(m.integerValue(def), 2);

            YVal map0 = yvv.vector[0];
            YVal map1 = yvv.vector[1];
            Assert.assertEquals(map0.tag, YValTag.MAPPING);
            Assert.assertEquals(map1.tag, YValTag.MAPPING);

            VectorValue ymm0 = m.expandMapping(map0);
            YVal[] args0 = ymm0.vector;
            YVal val0 = ymm0.value;
            Assert.assertEquals(args0.length, 3);
            Assert.assertEquals(args0[0].tag, YValTag.RATIONAL);
            Assert.assertEquals(args0[1].tag, YValTag.BOOL);
            Assert.assertEquals(args0[2].tag, YValTag.RATIONAL);
            Assert.assertEquals(val0.tag, YValTag.RATIONAL);
            // these are a bit iffy (check the model print out above if they fail)
            Assert.assertEquals(m.integerValue(args0[0]), 1463);
            Assert.assertEquals(m.boolValue(args0[1]), false);
            Assert.assertEquals(m.integerValue(args0[2]), -579);
            Assert.assertEquals(m.integerValue(val0), 1);


            VectorValue ymm1 = m.expandMapping(map1);
            YVal[] args1 = ymm1.vector;
            YVal val1 = ymm1.value;
            Assert.assertEquals(args1.length, 3);
            Assert.assertEquals(args1[0].tag, YValTag.RATIONAL);
            Assert.assertEquals(args1[1].tag, YValTag.BOOL);
            Assert.assertEquals(args1[2].tag, YValTag.RATIONAL);
            Assert.assertEquals(val1.tag, YValTag.RATIONAL);
            // these are a bit iffy (check the model print out above if they fail)
            Assert.assertEquals(m.integerValue(args1[0]), 1464);
            Assert.assertEquals(m.boolValue(args1[1]), true);
            Assert.assertEquals(m.integerValue(args1[2]), -2042);
            Assert.assertEquals(m.integerValue(val1), 0);

            Terms.removeName("f");
            Terms.removeName("i");
            Terms.removeName("b");
            Types.removeName("r");

        }
    }


    @Test
    public void testImplicant() {
        int i = Terms.newUninterpretedTerm("i", Types.INT);
        try (Context c = new Context()){
            String fmla = "(and (> i 2) (< i 8) (/= i 4))";
            c.assertFormula(Terms.parse(fmla));
            Status stat = c.check();
            Assert.assertEquals(stat, Status.SAT);
            Model m = c.getModel();
            System.out.println("Model for " + fmla);
            System.out.println(m);
            Assert.assertEquals(m.toString(), "(= i 7)");
            int p = Terms.parse("(>= i 3)");
            int[] implicants = m.implicant(p);
            Assert.assertEquals(implicants.length, 1);
            //System.out.println(Terms.toString(Terms.and(implicants[0])));
            Assert.assertEquals(Terms.toString(implicants[0]), "(>= (+ -3 i) 0)");
            int q = Terms.parse("(<= i 9)");
            int[] terms = { p, q };
            int[] implicants2 = m.implicant(terms);
            Assert.assertEquals(implicants2.length, 2);
            //System.out.println(Terms.toString(Terms.and(implicants2)));
            Assert.assertEquals(Terms.toString(Terms.and(implicants2)), "(and (>= (+ -3 i) 0) (>= (+ 9 (* -1 i)) 0))");
        }
    }

    @Test
    public void testModelFromMap() {
        int tau = Types.bvType(8);
        int i = Terms.newUninterpretedTerm("i", Types.INT);
        int r = Terms.newUninterpretedTerm("r", Types.REAL);
        int v = Terms.newUninterpretedTerm("v", tau);
        int ic = Terms.intConst(42);
        int rc = Terms.rationalConst(13, 131);
        int vc = Terms.bvConst(8, 134);
        int[] vars = {i, r, v};
        int[] vals = {ic, rc, vc};
        Model m = new Model(vars, vals);
        Assert.assertEquals(m.toString(),"(= i 42)\n(= r 13/131)\n(= v 0b10000110)");
    }

    @Test
    public void testModelSupport() {
        int x = Terms.newUninterpretedTerm("x", Types.REAL);
        int y = Terms.newUninterpretedTerm("y", Types.REAL);
        int z = Terms.newUninterpretedTerm("z", Types.REAL);
        try (Context c = new Context()){
            String f = "(> x 0)";
            int fmla = Terms.parse(f);
            int t0 = Terms.parse("(ite (> x 0) (+ x z) y)");
            int t1 = Terms.parse("(+ (* x z) y)");
            int[] terms = {t0, t1};
            c.assertFormula(fmla);
            Status stat = c.check();
            Assert.assertEquals(stat, Status.SAT);
            Model m = c.getModel();
            //System.out.println("Model for " + f);
            //System.out.println(m);
            int[] support = m.support(t0);
            Assert.assertEquals(support.length, 2);
            Assert.assertEquals(support[0], x);
            Assert.assertEquals(support[1], z);
            support = m.support(terms);
            Assert.assertEquals(support.length, 3);
            Assert.assertEquals(support[0], x);
            Assert.assertEquals(support[1], y);
            Assert.assertEquals(support[2], z);
        }
    }


}
