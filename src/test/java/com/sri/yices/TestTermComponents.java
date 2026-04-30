package com.sri.yices;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assume.assumeTrue;

public class TestTermComponents {
    @Test
    public void testProjectedComponentsWhenAvailable() throws Exception {
        assumeTrue(TestAssumptions.IS_YICES_INSTALLED);

        Method sumComponent = optionalMethod("sumComponent");
        Method productComponent = optionalMethod("productComponent");
        if (sumComponent == null || productComponent == null) {
            return;
        }

        int x = Terms.newUninterpretedTerm("term_components_x", Types.INT);
        int y = Terms.newUninterpretedTerm("term_components_y", Types.INT);
        int bvType = Types.bvType(8);
        int bx = Terms.newUninterpretedTerm("term_components_bx", bvType);
        int by = Terms.newUninterpretedTerm("term_components_by", bvType);

        try {
            int arith = Terms.rationalPoly(new long[] { 3, -5 }, new long[] { 2, 1 }, new int[] { x, y });
            Assert.assertTrue(Terms.isSum(arith));

            Map<Integer, BigRational> arithComponents = collectSumComponents(sumComponent, arith);
            Assert.assertEquals(2, arithComponents.size());
            Assert.assertEquals(new BigRational(BigInteger.valueOf(3), BigInteger.valueOf(2)), arithComponents.get(x));
            Assert.assertEquals(new BigRational(BigInteger.valueOf(-5), BigInteger.ONE), arithComponents.get(y));

            int bsum = Terms.bvAdd(
                    Terms.bvMul(Terms.bvConst(8, 3), bx),
                    Terms.bvMul(Terms.bvConst(8, 5), by));
            Assert.assertTrue(Terms.isBvSum(bsum));

            Map<Integer, boolean[]> bvComponents = collectBvSumComponents(sumComponent, bsum);
            Assert.assertEquals(2, bvComponents.size());
            Assert.assertArrayEquals(Terms.bvConstValue(Terms.bvConst(8, 3)), bvComponents.get(bx));
            Assert.assertArrayEquals(Terms.bvConstValue(Terms.bvConst(8, 5)), bvComponents.get(by));

            int product = Terms.mul(Terms.power(x, 3), Terms.power(y, 2));
            Assert.assertTrue(Terms.isProduct(product));

            Map<Integer, Integer> productComponents = collectProductComponents(productComponent, product);
            Assert.assertEquals(2, productComponents.size());
            Assert.assertEquals(Integer.valueOf(3), productComponents.get(x));
            Assert.assertEquals(Integer.valueOf(2), productComponents.get(y));
        } finally {
            Terms.removeName("term_components_x");
            Terms.removeName("term_components_y");
            Terms.removeName("term_components_bx");
            Terms.removeName("term_components_by");
        }
    }

    private static Method optionalMethod(String name) {
        try {
            return Yices.class.getMethod(name, int.class, int.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Map<Integer, BigRational> collectSumComponents(Method method, int term) throws Exception {
        Map<Integer, BigRational> result = new HashMap<>();
        for (int idx = 0; ; idx++) {
            Object component = invoke(method, term, idx);
            if (component == null) {
                break;
            }
            int child = (Integer) component.getClass().getMethod("getTerm").invoke(component);
            Object factor = component.getClass().getMethod("getFactor").invoke(component);
            Assert.assertTrue(factor instanceof BigRational);
            result.put(child, (BigRational) factor);
        }
        return result;
    }

    private static Map<Integer, boolean[]> collectBvSumComponents(Method method, int term) throws Exception {
        Map<Integer, boolean[]> result = new HashMap<>();
        for (int idx = 0; ; idx++) {
            Object component = invoke(method, term, idx);
            if (component == null) {
                break;
            }
            int child = (Integer) component.getClass().getMethod("getTerm").invoke(component);
            Object factor = component.getClass().getMethod("getFactor").invoke(component);
            if (factor instanceof boolean[]) {
                result.put(child, (boolean[]) factor);
            }
        }
        return result;
    }

    private static Map<Integer, Integer> collectProductComponents(Method method, int term) throws Exception {
        Map<Integer, Integer> result = new HashMap<>();
        for (int idx = 0; ; idx++) {
            Object component = invoke(method, term, idx);
            if (component == null) {
                break;
            }
            int child = (Integer) component.getClass().getMethod("getTerm").invoke(component);
            int power = (Integer) component.getClass().getMethod("getPower").invoke(component);
            result.put(child, power);
        }
        return result;
    }

    private static Object invoke(Method method, int term, int idx) throws Exception {
        try {
            return method.invoke(null, term, idx);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw e;
        }
    }
}
