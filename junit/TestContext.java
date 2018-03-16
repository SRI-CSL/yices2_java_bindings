import com.sri.yices.Context;
import com.sri.yices.Terms;
import com.sri.yices.Types;
import com.sri.yices.Model;
import com.sri.yices.Status;
import org.junit.Assert;
import org.junit.Test;

public class TestContext {
    @Test
    public void test1() throws Exception {
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

    static void bitvector_test(int n) throws Exception {
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
    public void test2() throws Exception {
       bitvector_test(1);
       bitvector_test(2);
       bitvector_test(6);
       bitvector_test(32);
       bitvector_test(256);
    }
}
