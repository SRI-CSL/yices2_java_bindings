package com.sri.yices;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assume.assumeTrue;

public class TestYices {
    @Test
    public void testLoad() {
        assumeTrue(Assumptions.IS_YICES_INSTALLED);

        System.out.println("Loaded Yices version " + Yices.version());
        System.out.println("Built for " + Yices.buildArch());
        System.out.println("Build mode: " + Yices.buildMode());
        System.out.println("Build date: " + Yices.buildDate());
        System.out.println("MCSat supported: " + Yices.hasMcsat());
        System.out.println("Yices error: " + Yices.errorString());

        try {
            Yices.testException();
        } catch (OutOfMemory e) {
            System.out.println("Caught exception Yices.OutOfMemory as expected");
        }

        System.out.println();
    }

    @Test
    public void TestTypeConstructors() {
        assumeTrue(Assumptions.IS_YICES_INSTALLED);

        int boolType = Yices.boolType();
        int intType = Yices.intType();
        int realType = Yices.realType();
        int bv1 = Yices.bvType(1);
        int bv2 = Yices.bvType(2);
        int bv32 = Yices.bvType(32);
        int scalar = Yices.newScalarType(4);
        int t = Yices.newUninterpretedType();
        int u = Yices.newUninterpretedType();

        Assert.assertTrue(boolType >= 0);
        Assert.assertTrue(intType >= 0);
        Assert.assertTrue(realType >= 0);
        Assert.assertTrue(bv1 >= 0);
        Assert.assertTrue(bv2 >= 0);
        Assert.assertTrue(bv32 >= 0);
        Assert.assertTrue(scalar >= 0);
        Assert.assertTrue(t >= 0);
        Assert.assertTrue(u >= 0);

        System.out.println("Created basic types");
        System.out.println("bool: " + boolType);
        System.out.println("int:  " + intType);
        System.out.println("real: " + realType);
        System.out.println("bv1:  " + bv1);
        System.out.println("bv2:  " + bv2);
        System.out.println("bv32: " + bv32);
        System.out.println("scalar: " + scalar);
        System.out.println("t:    " + t);
        System.out.println("u:    " + u);

        int bv0 = Yices.bvType(0);
        Assert.assertEquals(bv0, -1);
        System.out.println("Failed for bv0: " + Yices.errorString());

        int bvneg = Yices.bvType(-100);
        Assert.assertEquals(bvneg, -1);
        System.out.println("Failed for bvneg: " + Yices.errorString());

        int bvlarge = Yices.bvType(Integer.MAX_VALUE);
        Assert.assertEquals(bvlarge, -1);
        System.out.println("Failed for bvlarge " + Yices.errorString());

        int[] a = new int[0];
        int badTuple = Yices.tupleType(a);
        Assert.assertEquals(badTuple, -1);
        System.out.println("Failed for badTuple: " + Yices.errorString());

        // Tuple and function type + give them a name
        int tuple = Yices.tupleType(intType, realType);
        Assert.assertTrue(tuple >= 0);
        System.out.println("tuple: " + tuple);
        int code = Yices.setTypeName(tuple, "the_tuple_type");
        Assert.assertTrue(code >= 0);

        int fun = Yices.functionType(boolType, bv32, boolType, scalar);
        Assert.assertTrue(fun >= 0);
        System.out.println("fun: " + fun);
        code = Yices.setTypeName(fun, "the_function_type");
        Assert.assertTrue(code >= 0);

        // Use the names
        int test = Yices.getTypeByName("the_tuple_type");
        Assert.assertEquals(test, tuple);
        System.out.println("Retrieved 'the_tuple_type': got " + test);

        test = Yices.getTypeByName("the_function_type");
        Assert.assertEquals(test, fun);
        System.out.println("Retrieved 'the_function_type': got " + test);

        test = Yices.getTypeByName("not_there");
        Assert.assertEquals(test, -1);
        System.out.println("Retrieved 'not_there': got -1");

        // Pretty print
        System.out.println("bool type: " + Yices.typeToString(boolType));
        System.out.println("int type:  " + Yices.typeToString(intType));
        System.out.println("real type: " + Yices.typeToString(realType));
        System.out.println("bv1 type:  " + Yices.typeToString(bv1));
        System.out.println("bv2 type:  " + Yices.typeToString(bv2));
        System.out.println("bv32 type: " + Yices.typeToString(bv32));
        System.out.println("scalar type: " + Yices.typeToString(scalar));
        System.out.println("type t:   " + Yices.typeToString(t));
        System.out.println("type u:   " + Yices.typeToString(u));
        System.out.println("tuple type: " + Yices.typeToString(tuple));
        System.out.println("fun type:   " + Yices.typeToString(fun));

        System.out.println();
    }

    static private int declare(String name, int type) {
        int v = Yices.newUninterpretedTerm(type);
        Yices.setTermName(v, name);
        return v;
    }

    // crude method to print results for binary operators
    // [op x y] = result
    static private void showBinOpResult(String op, int x, int y, int result) {
        System.out.println("[" + op + " " + Yices.termToString(x) +
                " " + Yices.termToString(y) + "] = " + Yices.termToString(result));
    }

    @Test
    public void testBoolTerms() {
        assumeTrue(Assumptions.IS_YICES_INSTALLED);

        // declare 20 variables of type bool
        int b = Yices.boolType();
        Assert.assertTrue(b >= 0);
        int[] v = new int[20];
        for (int i = 0; i < 20; i++) {
            v[i] = declare("A" + i, b);
        }
        // test 'AND' constructor + array allocation stuff in JNI
        for (int i = 0; i <= 20; i++) {
            int tst = Yices.and(java.util.Arrays.copyOf(v, i));
            Assert.assertTrue(tst >= 0);
            String pp = Yices.termToString(tst);
            System.out.println("And: size " + i + " : " + pp);
        }
        System.out.println();

        int ff = Yices.mkFalse();
        int tst_false = Yices.and(v[0], Yices.not(v[0]));
        Assert.assertEquals(tst_false, ff);
        System.out.println("(and A0 (not A0)) is " + Yices.termToString(tst_false));
        System.out.println();
        inspectTerm(tst_false);

        // test 'OR' constructor
        for (int i = 0; i <= 20; i++) {
            int tst = Yices.or(java.util.Arrays.copyOf(v, i));
            Assert.assertTrue(tst >= 0);
            String pp = Yices.termToString(tst);
            System.out.println("Or: size " + i + " : " + pp);
        }
        System.out.println();

        int tt = Yices.mkTrue();
        int tst_true = Yices.or(v[0], Yices.not(v[0]));
        Assert.assertEquals(tst_true, tt);
        System.out.println("(or A0 (not A0)) is " + Yices.termToString(tst_true));
        System.out.println();
        inspectTerm(tst_true);

        // test 'XOR' constructor
        for (int i = 0; i <= 20; i++) {
            int tst = Yices.xor(java.util.Arrays.copyOf(v, i));
            Assert.assertTrue(tst >= 0);
            String pp = Yices.termToString(tst);
            System.out.println("Xor: size " + i + " : " + pp);
        }
        System.out.println();

        // iff
        for (int i = 0; i <= 2; i++) {
            int x = v[i];
            int not_x = Yices.not(x);
            for (int j = 0; j <= 2; j++) {
                int y = v[j];
                int not_y = Yices.not(y);
                int tst1 = Yices.iff(x, y);
                int tst2 = Yices.iff(x, not_y);
                int tst3 = Yices.iff(not_x, y);
                int tst4 = Yices.iff(not_x, not_y);
                Assert.assertTrue(tst1 >= 0 && tst2 >= 0 && tst3 >= 0 && tst4 >= 0);
                showBinOpResult("iff", x, y, tst1);
                showBinOpResult("iff", x, not_y, tst2);
                showBinOpResult("iff", not_x, y, tst3);
                showBinOpResult("iff", not_x, not_y, tst4);
            }
        }
        System.out.println();

        // implies
        for (int i = 0; i <= 2; i++) {
            int x = v[i];
            int not_x = Yices.not(x);
            for (int j = 0; j <= 2; j++) {
                int y = v[j];
                int not_y = Yices.not(y);
                int tst1 = Yices.implies(x, y);
                int tst2 = Yices.implies(x, not_y);
                int tst3 = Yices.implies(not_x, y);
                int tst4 = Yices.implies(not_x, not_y);
                Assert.assertTrue(tst1 >= 0 && tst2 >= 0 && tst3 >= 0 && tst4 >= 0);
                showBinOpResult("implies", x, y, tst1);
                showBinOpResult("implies", x, not_y, tst2);
                showBinOpResult("implies", not_x, y, tst3);
                showBinOpResult("implies", not_x, not_y, tst4);
            }
        }
        System.out.println();
    }

    static private boolean int2bool(int x) {
        return x != 0;
    }

    static private void showBitvectorConstant(boolean[] c) {
        System.out.print("0b");
        for (int i = c.length-1; i>=0; i --) {
            System.out.print(c[i] ? "1" : "0");
        }
        System.out.println();
    }
    static private void showConstantValue(int t) {
        if (Yices.termIsBool(t)) {
            int c = Yices.boolConstValue(t);
            if (c < 0) {
                System.out.println("not a Boolean constant");
            } else {
                System.out.println(int2bool(c));
            }
        } else if (Yices.termIsScalar(t)) {
            int c = Yices.scalarConstantIndex(t);
            if (c < 0) {
                System.out.println("not a scalar constant");
            } else {
                System.out.println("<const!" + c + ">");
            }
        } else if (Yices.termIsBitvector(t)) {
            boolean c[] = Yices.bvConstValue(t);
            if (c == null) {
                System.out.println("not a bv constant");
            } else {
                showBitvectorConstant(c);
            }
        } else if (Yices.termIsArithmetic(t)) {
            BigRational q = Yices.rationalConstValue(t);
            if (q == null) {
                System.out.println("not a rational constant");
            } else {
                System.out.println(q);
            }
        } else {
            System.out.println("not a constant");
        }
    }

    static private void inspectTerm(int t) {
        System.out.println("Term: " + t + ": " + Yices.termToString(t));
        System.out.println("  type = " + Yices.typeToString(Yices.typeOfTerm(t)));
        System.out.println("  name = " + Yices.getTermName(t));
        System.out.println("  constructor = " + Yices.termConstructor(t));
        System.out.println("  numChildren = " + Yices.termNumChildren(t));
        System.out.println("  isBool = " + Yices.termIsBool(t));
        System.out.println("  isInt = " + Yices.termIsInt(t));
        System.out.println("  isReal = " + Yices.termIsReal(t));
        System.out.println("  isArithmetic = " + Yices.termIsArithmetic(t));
        System.out.println("  isBitvector = " + Yices.termIsBitvector(t));
        System.out.println("  isTuple = " + Yices.termIsTuple(t));
        System.out.println("  isFunction = " + Yices.termIsFunction(t));
        System.out.println("  isScalar = " + Yices.termIsScalar(t));
        System.out.println("  bitsize = " + Yices.termBitSize(t));
        System.out.println("  isGround = " + Yices.termIsGround(t));
        System.out.println("  isAtomic = " + Yices.termIsAtomic(t));
        System.out.println("  isComposite = " + Yices.termIsComposite(t));
        System.out.println("  isProjection = " + Yices.termIsProjection(t));
        System.out.println("  isSum = " + Yices.termIsSum(t));
        System.out.println("  isBvSum = " + Yices.termIsBvSum(t));
        System.out.println("  isProduct = " + Yices.termIsProduct(t));
        if (Yices.termIsAtomic(t)) {
            System.out.print("  val = ");
            showConstantValue(t);
        }
        System.out.println();
    }

    @Test
    public void testGeneral() {
        assumeTrue(Assumptions.IS_YICES_INSTALLED);

        /*
         * U, I: uninterpreted types
         * F: function from I to U
         * G: function from I \times I  to U
         * i1, i2: distinct constants of type I
         * u1, u2: distinct constants of type U
         * x, y: variables of type I
         * a, b: variables of type U
         */
        int type_u = Yices.newUninterpretedType();
        Yices.setTypeName(type_u, "U");
        int type_i = Yices.newUninterpretedType();
        Yices.setTypeName(type_i, "I");
        int f = Yices.newUninterpretedTerm(Yices.functionType(type_u, type_i));
        Yices.setTermName(f, "F");
        int g = Yices.newUninterpretedTerm(Yices.functionType(type_u, type_i, type_i));
        Yices.setTermName(g, "G");

        int i1 = Yices.mkConstant(type_i, 0);
        Yices.setTermName(i1, "i1");
        int i2 = Yices.mkConstant(type_i, 1);
        Yices.setTermName(i2, "i2");

        int u1 = Yices.mkConstant(type_u, 0);
        Yices.setTermName(u1, "u1");
        int u2 = Yices.mkConstant(type_u, 1);
        Yices.setTermName(u2, "u2");

        int x = Yices.newUninterpretedTerm(type_i);
        Yices.setTermName(x, "x");
        int y = Yices.newUninterpretedTerm(type_i);
        Yices.setTermName(y, "y");

        int a = Yices.newUninterpretedTerm(type_u);
        Yices.setTermName(a, "a");
        int b = Yices.newUninterpretedTerm(type_u);
        Yices.setTermName(b, "b");

        Assert.assertTrue(f >= 0 && g >= 0 && i1 >= 0 && i2 >= 0 && u1 >= 0 && u2 >= 0);
        Assert.assertTrue(x >= 0 && y >= 0 && a >= 0 && b >= 0);

        inspectTerm(f);
        inspectTerm(g);
        inspectTerm(i1);
        inspectTerm(i2);
        inspectTerm(u1);
        inspectTerm(u2);
        inspectTerm(x);
        inspectTerm(y);
        inspectTerm(a);
        inspectTerm(b);

        inspectTerm(Yices.funApplication(f, i1));
        inspectTerm(Yices.funApplication(f, i2));
        inspectTerm(Yices.funApplication(g, i1, i1));
        inspectTerm(Yices.funApplication(g, i1, x));

        inspectTerm(Yices.functionUpdate(f, new int[]{x}, u2));
        inspectTerm(Yices.functionUpdate1(f, x, u2));
        inspectTerm(Yices.functionUpdate(g, new int[]{x, x}, u1));
        inspectTerm(Yices.eq(f, Yices.functionUpdate1(f, i1, b)));
        inspectTerm(Yices.distinct(i1, i2, x, y));
        inspectTerm(Yices.distinct(i1, i2));
        inspectTerm(Yices.distinct(a, b, u1, u2));
        inspectTerm(Yices.neq(b, u2));
        inspectTerm(Yices.tuple(a, x, b, i2, Yices.mkTrue()));
        inspectTerm(Yices.tupleUpdate(Yices.tuple(a, i2, i2), 1, b));
        inspectTerm(Yices.tupleUpdate(Yices.tuple(a, i2, i2), 2, i1));

        int xxx = Yices.funApplication(f, i1, i2);
        Assert.assertTrue(xxx < 0);
        System.out.println("(f i1 i2) failed with error " + Yices.errorString());

        xxx = Yices.funApplication(g, y);
        Assert.assertTrue(xxx < 0);
        System.out.println("(g y) failed with error " + Yices.errorString());

        System.out.println();
    }

    @Test
    public void testBigInt() {
        assumeTrue(Assumptions.IS_YICES_INSTALLED);

        System.out.println("Bigint: 0");
        int tst = Yices.mkIntConstant(new BigInteger("0"));
        Assert.assertTrue(tst >= 0);
        inspectTerm(tst);

        System.out.println("Bigint: 1");
        tst = Yices.mkIntConstant(new BigInteger("1"));
        Assert.assertTrue(tst >= 0);
        inspectTerm(tst);

        System.out.println("Bigint: -1");
        tst = Yices.mkIntConstant(new BigInteger("-1"));
        Assert.assertTrue(tst >= 0);
        inspectTerm(tst);

        System.out.println("Bigint: 100000000000000000000000000000000");
        tst = Yices.mkIntConstant(new BigInteger("100000000000000000000000000000000"));
        Assert.assertTrue(tst >= 0);
        inspectTerm(tst);

        System.out.println("Bigint: -100000000000000000000000000000000");
        tst = Yices.mkIntConstant(new BigInteger("-100000000000000000000000000000000"));
        Assert.assertTrue(tst >= 0);
        inspectTerm(tst);

        System.out.println("Rational: -1/3");
        tst = Yices.mkRationalConstant(new BigRational("-1/3"));
        Assert.assertTrue(tst >= 0);
        inspectTerm(tst);

        System.out.println("Rational: 1/3");
        tst = Yices.mkRationalConstant(new BigRational("1/3"));
        Assert.assertTrue(tst >= 0);
        inspectTerm(tst);

        System.out.println("Rational: -1/30000000000000");
        tst = Yices.mkRationalConstant(new BigRational("-1/30000000000000"));
        Assert.assertTrue(tst >= 0);
        inspectTerm(tst);

        System.out.println("Rational: 125/30000000000000");
        tst = Yices.mkRationalConstant(new BigRational("125/30000000000000"));
        Assert.assertTrue(tst >= 0);
        inspectTerm(tst);
    }

    @Test
    public void testBitvectors() {
        assumeTrue(Assumptions.IS_YICES_INSTALLED);

        inspectTerm(Yices.bvZero(10));
        inspectTerm(Yices.bvZero(32));
        inspectTerm(Yices.bvZero(64));
        inspectTerm(Yices.bvZero(73));
        inspectTerm(Yices.bvOne(10));
        inspectTerm(Yices.bvOne(32));
        inspectTerm(Yices.bvOne(64));
        inspectTerm(Yices.bvOne(73));
        inspectTerm(Yices.parseBvHex("A0B1C2D3E4"));
        inspectTerm(Yices.parseBvBin("111000111"));
    }

    static private void tstMpz(String number) {
        System.out.println("Input: " + number);
        BigInteger base = new BigInteger(number);

        byte[] b = Yices.testMpzToBytes(number);
        System.out.print("Bytes:");
        for (byte c : b) {
            System.out.print(" " + c);
        }
        System.out.println();

        BigInteger test = new BigInteger(b);
        System.out.println("Output: " + test);
        Assert.assertEquals(base, test);
        System.out.println();
    }

    @Ignore
    @Test
    public void testMpz() {
        for (int i=0; i<=65536; i++) {
            tstMpz(Integer.toString(i));
            tstMpz(Integer.toString(-i));
        }
    }

    static private void tstArrayToMpz(String number) {
        System.out.println("Input: " + number);
        BigInteger x = new BigInteger(number);

        byte b[] = x.toByteArray();
        Yices.testBytesToMpz(b);
    }

    @Ignore
    @Test
    public void testArrayToMpz() {
        for (int i=0; i<=65536; i++) {
            tstArrayToMpz(Integer.toString(i));
            tstArrayToMpz(Integer.toString(-i));
        }
    }

    @Test
    public void testConfig() {
        assumeTrue(Assumptions.IS_YICES_INSTALLED);

        // Configs
        long cfg = Yices.newConfig();
        // setConfig
        int code = Yices.setConfig(cfg,"mode", "one-shot");
        Assert.assertEquals(code, 0);
        System.out.println("setConfig: valid one-shot code = " + code);
        code = Yices.setConfig(cfg,"mode", "invalid");
        Assert.assertEquals(code, -1);
        System.out.println("setConfig: invalid code = " + code);
        code = Yices.setConfig(cfg, "mode", "push-pop");
        Assert.assertEquals(code, 0);
        System.out.println("setConfig: valid push-pop code = " + code);
        // defaultConfigForLogic
        code = Yices.defaultConfigForLogic(cfg, "QF_AX");
        Assert.assertEquals(code, 0);
        System.out.println("defaultConfigForLogic: valid code = " + code);
        code = Yices.defaultConfigForLogic(cfg, "invalid");
        Assert.assertEquals(code, -1);
        System.out.println("defaultConfigForLogic: invalid code = " + code);
        // freeConfig
        Yices.freeConfig(cfg);
    }

    @Test
    public void testContext() {
        assumeTrue(Assumptions.IS_YICES_INSTALLED);

        // Contexts
        long ctx = Yices.newContext(0);
        // contextStatus
        int stat = Yices.contextStatus(ctx);
        Assert.assertEquals(stat, 0);
        System.out.println("contextStatus: stat = " + stat);
        // contextEnableOption
        int err = Yices.contextEnableOption(ctx, "var-elim");
        Assert.assertEquals(err, 0);
        System.out.println("contextEnableOption: valid err = " + err);
        err = Yices.contextEnableOption(ctx, "invalid");
        Assert.assertEquals(err, -1);
        System.out.println("contextEnableOption: invalid err = " + err);
        err = Yices.contextDisableOption(ctx, "var-elim");
        Assert.assertEquals(err, 0);
        System.out.println("contextDisableOption: valid err = " + err);
        // push and pop
        err = Yices.push(ctx);
        Assert.assertEquals(err, 0);
        System.out.println("push: valid err = " + err);
        err = Yices.pop(ctx);
        Assert.assertEquals(err, 0);
        System.out.println("pop: 1st is valid: err = " + err);
        // popping again is an error
        err = Yices.pop(ctx);
        Assert.assertEquals(err, -1);
        System.out.println("pop: 2nd is err = " + err);
        // newParamRecord
        long prm = Yices.newParamRecord();
        Yices.defaultParamsForContext(ctx, prm);
        err = Yices.setParam(prm, "d-factor", "1.0");
        System.out.println("setParam: err = " + err);
        Assert.assertEquals(err, 0);
        // freeParamRecord
        Yices.freeParamRecord(prm);
        // freeContext
        Yices.freeContext(ctx);
    }
        
    @Test
    public void testAssert() {
        assumeTrue(Assumptions.IS_YICES_INSTALLED);

        long ctx = Yices.newContext(0);
        // assertFormula a
        int bool = Yices.boolType();
        int a = declare("a", bool);
        int b = declare("b", bool);
        int c = declare("c", bool);
        int not_a = Yices.not(a);
        int err = Yices.assertFormula(ctx, a);
        Assert.assertEquals(err, 0);
        System.out.println("assertFormula: a, err = " + err);
        // checkContext - should be sat
        int stat = Yices.checkContext(ctx, 0);
        Assert.assertEquals(stat, 3);
        System.out.println("checkContext: stat = " + stat + " (3 = SAT)");
        // assertFormula not_a
        err = Yices.assertFormula(ctx, not_a);
        Assert.assertEquals(err, 0);
        System.out.println("assertFormula: not_a, err = " + err);
        // checkContext
        stat = Yices.checkContext(ctx, 0);
        Assert.assertEquals(stat, 4);
        System.out.println("checkContext: stat = " + stat + " (4 = UNSAT)");
        // resetContext
        Yices.resetContext(ctx);
        // assertFormulas b, b => a, not_a
        int impl = Yices.implies(b, a);
        int[] fmls = {b, not_a, impl};
        err = Yices.assertFormulas(ctx, fmls);
        Assert.assertEquals(err, 0);
        System.out.println("assertFormulas: err = " + err);
        // checkContext
        stat = Yices.checkContext(ctx, 0);
        Assert.assertEquals(stat, 4);
        System.out.println("checkContext: stat = " + stat + " (4 = UNSAT)");
        // assertBlockingClause
        Yices.resetContext(ctx);
        int[] ab = {a, b};
        int disj = Yices.or(ab);
        int[] abc = {disj, c};
        int conj = Yices.and(abc);
        err = Yices.assertFormula(ctx, conj);
        Assert.assertEquals(err, 0);
        stat = Yices.checkContext(ctx, 0);
        Assert.assertEquals(stat, 3);
        System.out.println("assertBlockingClause: (a or b) and c, stat = " + stat);

        err = Yices.assertBlockingClause(ctx);
        Assert.assertEquals(err, 0);
        stat = Yices.checkContext(ctx, 0);
        Assert.assertEquals(stat, 3);
        System.out.println("assertBlockingClause: stat = " + stat);
        err = Yices.assertBlockingClause(ctx);
        Assert.assertEquals(err, 0);
        stat = Yices.checkContext(ctx, 0);
        Assert.assertEquals(stat, 3);
        System.out.println("assertBlockingClause: stat = " + stat);
        err = Yices.assertBlockingClause(ctx);
        Assert.assertEquals(err, 0);
        stat = Yices.checkContext(ctx, 0);
        Assert.assertEquals(stat, 4);
        System.out.println("assertBlockingClause: stat = " + stat);
        // stopSearch - doesn't really test it, need to start a long
        //              checkContext and interrupt it from another thread
        Yices.stopSearch(ctx);
        // freeContext
        Yices.freeContext(ctx);
    }

    @Test
    public void testModels() {
        assumeTrue(Assumptions.IS_YICES_INSTALLED);

        long ctx = Yices.newContext(0);

        int ybool = Yices.boolType();
        int a = declare("a", ybool);
        int b = declare("b", ybool);
        int c = declare("c", ybool);
        int yint = Yices.intType();
        int[] ab = {a, b};
        int disj = Yices.or(ab);
        int[] abc = {disj, c};
        int conj = Yices.and(abc);
        int err = Yices.assertFormula(ctx, conj);
        Assert.assertEquals(err, 0);
        int ii = declare("i", yint);
        int jj = declare("j", yint);
        int kk = declare("k", yint);
        int ij = Yices.add(ii, jj);
        int ijk = Yices.arithLt(ij, kk);
        int three = Yices.mkIntConstant(3);
        int igt3 = Yices.arithGt(ii, three);
        int ar = Yices.and(igt3, ijk);
        err = Yices.assertFormula(ctx, ar);
        Assert.assertEquals(err, 0);
        int yreal = Yices.realType();
        int x = declare("x", yreal);
        int ix = Yices.arithLt(ii, x);
        int one = Yices.mkIntConstant(1);
        int i1 = Yices.add(ii, one);
        int xi1 = Yices.arithLt(x, i1);
        int ar2 = Yices.and(ix, xi1);
        err = Yices.assertFormula(ctx, ar2);
        Assert.assertEquals(err, 0);
        int stat = Yices.checkContext(ctx, 0);
        Assert.assertEquals(stat, 3);
        System.out.println("testModels: (a or b) and c, stat = " + stat);

        // Conversion to a string
        long mdl = Yices.getModel(ctx, 1);
        System.out.println(Yices.modelToString(mdl));
        System.out.println("testModels: 10 columns x 3 lines");
        System.out.println("----------");
        System.out.println(Yices.modelToString(mdl, 10, 3));
        System.out.println("----------");

        int cval = Yices.getBoolValue(mdl, c);
        Assert.assertEquals(cval, 1);

        // test get integer value
        long[] buffer = new long[2];
        int code = Yices.getIntegerValue(mdl, ii, buffer);
        Assert.assertEquals(code, 0); // success expected
        Assert.assertEquals(buffer[0], 4);
        System.out.println("value of i via getIntegerValue: " + buffer[0]);
        BigInteger test = Yices.getIntegerValue(mdl, ii);
        System.out.println("value of i as a BigInteger: "+ test);

        // test rational value
        code = Yices.getRationalValue(mdl, x, buffer);
        long num = buffer[0];
        long den = buffer[1];
        System.out.println("testModels: num = " + num + ", den = " + den);
        Assert.assertEquals(num, 9);
        Assert.assertEquals(den, 2);
        BigRational test2 = Yices.getRationalValue(mdl, x);
        System.out.println("value as a BigRational: " + test2);

        // freeModel
        Yices.freeModel(mdl);
        // freeContext
        Yices.freeContext(ctx);
    }

    /*
     * This is flaky & needs to be fixed.
     */
    @Test
    @Ignore
    public void testGC() {
        int b = Yices.boolType();
        int x = Yices.newUninterpretedTerm(b);
        int y = Yices.newUninterpretedTerm(b);
        int z = Yices.newUninterpretedTerm(b);
        System.out.println("Testing the garbage collector");
        System.out.println(" number of terms: " + Yices.yicesNumTerms());
        System.out.println(" number of types: " + Yices.yicesNumTypes());
        Yices.yicesGarbageCollect();
        System.out.println("After garbage collection");
        System.out.println(" number of terms: " + Yices.yicesNumTerms());
        System.out.println(" number of types: " + Yices.yicesNumTypes());
        System.out.println();
    }
}
