package com.sri.yices;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assume.assumeTrue;

public class TestDimacs {

    @Test
    public void testDimacs() {
        // JUnit runner treats tests with failing assumptions as ignored
        assumeTrue(TestAssumptions.IS_YICES_INSTALLED);

        int fcount = 3;

        int[] formulas = new int[fcount];

        int tau = Types.bvType(20);
        int x = Terms.newUninterpretedTerm("x", tau);
        int y = Terms.newUninterpretedTerm("y", tau);
        int z = Terms.newUninterpretedTerm("z", tau);

        formulas[0] = Terms.bvEq(Terms.bvMul(x, y), Terms.bvConst(20, 12289));
        formulas[1] = Terms.bvEq(Terms.bvMul(y, z), Terms.bvConst(20, 20031));
        formulas[2] = Terms.bvEq(Terms.bvMul(x, z), Terms.bvConst(20, 10227));

        // first round, don't simplify the CNF
        for (int i = 0; i < fcount; i++ ){
            boolean simplify = false;
            String filename = String.format("/tmp/basic%d.cnf", i);
            Status[] status = new Status[1];
            boolean fileOK = Dimacs.export(formulas, filename, simplify, status);
            System.out.println(String.format("Yices.exportToDimacs(%s, simplify: %s) = %s status = %s", filename, simplify, fileOK, status[0]));
        }

        // second round, simplify the CNF
         for (int i = 0; i < fcount; i++ ){
            boolean simplify = true;
            String filename = String.format("/tmp/simplify%d.cnf", i);
            Status[] status = new Status[1];
            boolean fileOK = Dimacs.export(formulas, filename, simplify, status);
            System.out.println(String.format("Yices.exportToDimacs(%s, simplify: %s) = %s status = %s", filename, simplify, fileOK, status[0]));
         }

    }



}
