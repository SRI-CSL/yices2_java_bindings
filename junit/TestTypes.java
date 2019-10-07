import com.sri.yices.Types;
import com.sri.yices.YicesException;
import org.junit.Assert;
import org.junit.Test;


public class TestTypes {
    @Test
    public void testBooltype() throws YicesException {
        int b = Types.boolType();
        Assert.assertEquals(b, Types.BOOL);
        System.out.println("bool type: " + b);
        System.out.println("toString(bool): " + Types.toString(b));
        System.out.println("name(bool): " + Types.getName(b));
        Assert.assertTrue(Types.isBool(b));
        Assert.assertFalse(Types.isArithmetic(b));
        Assert.assertFalse(Types.isBitvector(b));
        Assert.assertFalse(Types.isTuple(b));
        Assert.assertFalse(Types.isFunction(b));
        System.out.println();
    }

    @Test
    public void testArithmeticTypes() throws YicesException {
        int itype = Types.intType();
        int rtype = Types.realType();
        Assert.assertEquals(itype, Types.INT);
        Assert.assertEquals(rtype, Types.REAL);
        System.out.println("int type: " + itype + ", real type: " + rtype);
        System.out.println("toString(int): " + Types.toString(itype));
        System.out.println("toString(real): " + Types.toString(rtype));
        System.out.println("name(int): " + Types.getName(itype));
        System.out.println("name(real): " + Types.getName(rtype));

        Assert.assertFalse(Types.isBool(itype));
        Assert.assertTrue(Types.isArithmetic(itype));
        Assert.assertTrue(Types.isInt(itype));
        Assert.assertFalse(Types.isReal(itype));
        Assert.assertFalse(Types.isBitvector(itype));
        Assert.assertFalse(Types.isTuple(itype));
        Assert.assertFalse(Types.isFunction(itype));

        Assert.assertFalse(Types.isBool(rtype));
        Assert.assertTrue(Types.isArithmetic(rtype));
        Assert.assertFalse(Types.isInt(rtype));
        Assert.assertTrue(Types.isReal(rtype));
        Assert.assertFalse(Types.isBitvector(rtype));
        Assert.assertFalse(Types.isTuple(rtype));
        Assert.assertFalse(Types.isFunction(rtype));

        System.out.println();
    }

    @Test
    public void testBvTypes() throws Exception {
        for (int i=1; i<70; i += 3) {
            int bv = Types.bvType(i);
            System.out.println("type bv[" + i + "]: " + bv);
            System.out.println("toString(bv[" + i + "]): " + Types.toString(bv));
            System.out.println("name(bv[" + i + "]): " + Types.getName(bv));

            Assert.assertFalse(Types.isBool(bv));
            Assert.assertFalse(Types.isArithmetic(bv));
            Assert.assertTrue(Types.isBitvector(bv));

            Assert.assertEquals(Types.bvSize(bv), i);
        }
        Assert.assertEquals(Types.bvType(8), Types.BV8);
        Assert.assertEquals(Types.bvType(16), Types.BV16);
        Assert.assertEquals(Types.bvType(32), Types.BV32);
        Assert.assertEquals(Types.bvType(64), Types.BV64);

        System.out.println();
    }

    @Test
    public void testUninterpreted() throws Exception {
        int tau = Types.newUninterpretedType();
        System.out.println("New anonymous uninterpreted type: " + tau);
        System.out.println("toString: " + Types.toString(tau));
        System.out.println("name: " + Types.getName(tau));
        Assert.assertTrue(Types.isUninterpreted(tau));

        int tau2 = Types.newUninterpretedType("TTT");
        System.out.println("New type TTT: " + tau2);
        System.out.println("toString: " + Types.toString(tau2));
        System.out.println("name: " + Types.getName(tau2));
        Assert.assertTrue(Types.isUninterpreted(tau2));
        Assert.assertEquals(Types.getName(tau2), "TTT");

        int test = Types.getByName("TTT");
        Assert.assertEquals(test, tau2);

        System.out.println();
    }

    @Test
    public void testScalar() throws Exception {
        int tau = Types.newScalarType(20);
        System.out.println("New anonymous scalar type: " + tau);
        System.out.println("toString: " + Types.toString(tau));
        System.out.println("name: " + Types.getName(tau));
        Assert.assertTrue(Types.isScalar(tau));
        Assert.assertEquals(Types.scalarCard(tau), 20);

        int tau2 = Types.newScalarType("S", 5);
        System.out.println("New type S: " + tau2);
        System.out.println("toString: " + Types.toString(tau2));
        System.out.println("name: " + Types.getName(tau2));
        Assert.assertTrue(Types.isScalar(tau2));
        Assert.assertEquals(Types.getName(tau2), "S");
        Assert.assertEquals(Types.scalarCard(tau2), 5);

        int test = Types.getByName("S");
        Assert.assertEquals(test, tau2);

        System.out.println();
    }

    @Test
    public void testTuple() throws Exception {
        int tau = Types.tupleType(Types.BOOL, Types.BV16, Types.REAL);
        System.out.println("Tuple[bool,bv16,real]: " + tau);
        System.out.println("toString: " + Types.toString(tau));
        System.out.println("name: " + Types.getName(tau));
        System.out.println("num children: " + Types.numChildren(tau));
        for (int i=0; i<Types.numChildren(tau); i ++ ) {
            System.out.println(" child[" + i + "]: " + Types.toString(Types.child(tau, i)));
        }
        Assert.assertTrue(Types.isTuple(tau));
        Assert.assertEquals(Types.numChildren(tau), 3);

        int a[] = Types.children(tau);
        Assert.assertEquals(a.length, 3);
        Assert.assertEquals(a[0], Types.BOOL);
        Assert.assertEquals(a[1], Types.BV16);
        Assert.assertEquals(a[2], Types.REAL);

        int tau2 = Types.tupleType(Types.BOOL, Types.BV16, Types.REAL);
        Assert.assertEquals(tau, tau2);

        Types.setName(tau, "D");
        System.out.println("Setting name to D");
        System.out.println("  name: " + Types.getName(tau));

        int test = Types.getByName("D");
        Assert.assertEquals(test, tau);

        System.out.println();
    }

    @Test
    public void testFunctions() throws Exception {
        int tau = Types.functionType(Types.INT, Types.INT, Types.BOOL);
        System.out.println("[int, int -> bool]: " + tau);
        System.out.println("toString: " + Types.toString(tau));
        System.out.println("name: " + Types.getName(tau));
        System.out.println("num childern: " + Types.numChildren(tau));

        for (int i=0; i<Types.numChildren(tau); i ++ ) {
            System.out.println(" child[" + i + "]: " + Types.toString(Types.child(tau, i)));
        }
        Assert.assertTrue(Types.isFunction(tau));
        Assert.assertEquals(Types.numChildren(tau), 3);

        int a[] = Types.children(tau);
        Assert.assertEquals(a.length, 3);
        Assert.assertEquals(a[0], Types.INT);
        Assert.assertEquals(a[1], Types.INT);
        Assert.assertEquals(a[2], Types.BOOL);

        int tau2 = Types.predicateType(Types.INT, Types.INT);
        Assert.assertEquals(tau, tau2);

        Types.setName(tau, "F");
        System.out.println("Setting name to F");
        System.out.println("  name: " + Types.getName(tau));

        int test = Types.getByName("F");
        Assert.assertEquals(test, tau);

        System.out.println("Removing name F");
        Types.removeName("F");
        test = Types.getByName("F");
        Assert.assertEquals(test, Types.NULL_TYPE);

        System.out.println();
    }

    @Test
    public void testExceptions() {
        try {
            int tau = Types.parse("(-> bool)");
        } catch (Exception e) {
            System.out.println("Got exception: " + e);
        }
        try {
            int a[] = new int [] { };
            int error = Types.tupleType(a);
        } catch (Exception e) {
            System.out.println("Got exception: " + e);
        }
        try {
            int n = Types.numChildren(Types.NULL_TYPE);
        } catch (Exception e) {
            System.out.println("Got exception: " + e);
        }
    }
}

