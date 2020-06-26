package com.sri.yices;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assume.assumeTrue;

public class TestDimacs {


    // return an array of the first n members of formulas
    static int[] truncate(int[] formulas, int n) {
        if (n >= formulas.length) return formulas;
        int[] retval = new int[n];
        for (int i = 0; i < n; i++){
            retval[i] = formulas[i];
        }
        return retval;
    }

    @Test
    public void testDimacs() {
        System.err.println("testDimacs");

        // JUnit runner treats tests with failing assumptions as ignored
        assumeTrue(TestAssumptions.IS_YICES_INSTALLED);
        assumeTrue(Yices.versionOrdinal() >= Yices.versionOrdinal(2, 6, 2));

        int fcount = 3;

        int[] formulas = new int[fcount];

        int tau = Types.bvType(20);
        int x = Terms.newUninterpretedTerm("x", tau);
        int y = Terms.newUninterpretedTerm("y", tau);
        int z = Terms.newUninterpretedTerm("z", tau);

        formulas[0] = Terms.bvEq(Terms.bvMul(x, y), Terms.bvConst(20, 12289));
        formulas[1] = Terms.bvEq(Terms.bvMul(y, z), Terms.bvConst(20, 20031));
        formulas[2] = Terms.bvEq(Terms.bvMul(x, z), Terms.bvConst(20, 10227));

        Status[] simplified = new Status[fcount];


        // first round, don't simplify the CNF
        System.err.println("Round one");
        for (int i = 1; i <= fcount; i++ ){
            boolean simplify = false;
            String filename = String.format("/tmp/basic%d.cnf", i);
            int[] terms = truncate(formulas, i);
            Status[] status = new Status[1];
            boolean fileOK = Dimacs.export(terms, filename, simplify, status);
            System.out.println(String.format("Yices.exportToDimacs(%s, simplify: %s) = %s status = %s", filename, simplify, fileOK, status[0]));
        }

        // second round, simplify the CNF
        System.err.println("Round two");
        for (int i = 1; i <= fcount; i++ ){
            boolean simplify = true;
            String filename = String.format("/tmp/simplify%d.cnf", i);
            int[] terms = truncate(formulas, i);
            Status[] status = new Status[1];
            boolean fileOK = Dimacs.export(terms, filename, simplify, status);
            simplified[i - 1] = status[0];
            System.out.println(String.format("Yices.exportToDimacs(%s, simplify: %s) = %s status = %s", filename, simplify, fileOK, status[0]));
         }


        System.err.println("Round three");
        for (int i = 1; i <= fcount; i++ ){
             try (Context ctx = new Context("QF_BV")) {
                 int[] terms = truncate(formulas, i);
                 ctx.assertFormulas(terms);
                 Status stat = ctx.check(10);
                 System.out.println(String.format("Status %d: %s", i, stat));
                 Assert.assertEquals(stat, simplified[i - 1]);
             }
         }
    }

}
