package com.sri.yices;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assume.assumeTrue;

public class TestDelegates {

    public static final boolean HAS_CADICAL = Yices.hasDelegate("cadical");
    public static final boolean HAS_CRYPTOMINISAT = Yices.hasDelegate("cryptominisat");
    public static final boolean HAS_Y2SAT = Yices.hasDelegate("y2sat");


    public static final int FCOUNT = 3;


    static int[] makeFormlas() {

        int[] formulas = new int[FCOUNT];

        int tau = Types.bvType(20);
        int x = Terms.newUninterpretedTerm("x", tau);
        int y = Terms.newUninterpretedTerm("y", tau);
        int z = Terms.newUninterpretedTerm("z", tau);

        formulas[0] = Terms.bvEq(Terms.bvMul(x, y), Terms.bvConst(20, 12289));
        formulas[1] = Terms.bvEq(Terms.bvMul(y, z), Terms.bvConst(20, 20031));
        formulas[2] = Terms.bvEq(Terms.bvMul(x, z), Terms.bvConst(20, 10227));
        return formulas;
    }

    // return an array of the first n members of formulas
    static int[] truncate(int[] formulas, int n) {
        if (n >= formulas.length) return formulas;
        int[] retval = new int[n];
        for (int i = 0; i < n; i++){
            retval[i] = formulas[i];
        }
        return retval;
    }

    static int conjoin(int[] formulas, int n) {
        return Terms.and(truncate(formulas, n));
    }

    static void delgado(String delegate){
        System.out.println(String.format("%s test", delegate));

        int[] formulas = makeFormlas();

       for (int i = 1; i <= FCOUNT; i++ ){
            try (Context ctx = new Context("QF_BV")) {
                int[] terms = truncate(formulas, i);
                ctx.assertFormulas(terms);
                Status status = ctx.check(10);
                System.out.println(String.format("Yices status %d: %s", i, status));
            }
        }

        for (int i = 1; i <= FCOUNT; i++ ){
            Model[] model = { null };
            int[] terms = truncate(formulas, i);
            Status status = Delegate.checkFormulas(terms, "QF_BV", delegate, model);
            System.out.println(String.format("Delegate %s status %d: %s", delegate, i, status));
        }


        for (int i = 1; i <= FCOUNT; i++ ){
            Model[] model = { null };
            int term = conjoin(formulas, i);
            Status status = Delegate.checkFormula(term, "QF_BV", delegate, model);
            System.out.println(String.format("Delegate %s status %d: %s", delegate, i, status));
        }

        Terms.removeName("x");
        Terms.removeName("y");
        Terms.removeName("z");
    }

    @Test
    public void testLoad() {
        assumeTrue(TestAssumptions.IS_YICES_INSTALLED);

        System.out.println("Loaded Yices version " + Yices.version());
        System.out.println("Built for " + Yices.buildArch());
        System.out.println("Build mode: " + Yices.buildMode());
        System.out.println("Build date: " + Yices.buildDate());
        System.out.println("MCSat supported: " + Yices.hasMcsat());
        Yices.resetError();
        System.out.println("Yices error: " + Yices.errorString());

        System.out.println("Has cadical as a delegate: " + HAS_CADICAL);
        System.out.println("Has cryptominisat as a delegate: " + HAS_CRYPTOMINISAT);
        System.out.println("Has y2sat as a delegate: " + HAS_Y2SAT);
        System.out.println();
    }


    @Test
   public void testCadical() {
        assumeTrue(HAS_CADICAL);
        delgado("cadical");
    }

    @Test
    public void testCryptominisat() {
        assumeTrue(HAS_CRYPTOMINISAT);
        delgado("cadical");
    }

    @Test
   public void testY2sat() {
        assumeTrue(HAS_Y2SAT);
        delgado("y2sat");
    }




}
