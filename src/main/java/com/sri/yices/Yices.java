package com.sri.yices;

import java.math.BigInteger;

public final class Yices {
    private static boolean is_ready;

    /*
     * Try to load the Yices Library libyices2java.
     *
     * For now, it's best to see the exception (if any) rather than catch it
     * and print a generic message.
     */
    static {
        try {
            System.loadLibrary("yices2java");
            init();
            is_ready = true;
        } catch (LinkageError e) {
            is_ready = false;
            throw e;
        }
    }

    /*
     * Check whether the library is loaded.
     * @throws LinkageError if one of the required runtime library is not found
     * (or can't be loaded for some reason).
     */
    public static boolean isReady() {
        return is_ready;
    }

    /* Version as an ordinal. For library compatability checks */
    public static native long versionOrdinal();

    /**
     * For comparisons (this is how the no argument version is computed, using the constants in yices.h)
     * @param  version  (a number between 0 and less than 100).
     * @param  major    (a number between 0 and less than 100).
     * @param  patch    (a number between 0 and less than 100).
     *
     */
    public static final long versionOrdinal(int version, int major, int patch){
        return (1000 * 100 * version) + (100 * major) + patch;
    }

    /*
     * Generic functions in yices.h
     */
    public static native String version();
    public static native String buildArch();
    public static native String buildMode();
    public static native String buildDate();



    // check support for mcsat
    public static native boolean hasMcsat();

    // check whether the library was compiled in THREAD_SAFE mode.
    public static native boolean isThreadSafe();

    /*
     * Global operations:
     * - init is required and must be performed first
     * - exit frees the internal data structures used by Yices
     * - reset is the same as exit(); init();
     */
    private static native void init();
    private static native void exit();
    public static native void reset();

    /*
     * Error reports
     */
    public static native int errorCode();
    public static native String errorString();
    public static native void resetError();

    // For testing only
    public static native void testException();


    /*
     * TYPES
     */

    /*
     * Type constructors: all types are int32
     * - they return -1 if there's an error
     */
    public static native int boolType();
    public static native int realType();
    public static native int intType();
    public static native int bvType(int num_bits); // num_bits must be positive
    public static native int newScalarType(int card); // card must be positive
    public static native int newUninterpretedType();
    public static native int tupleType(int ... tau); // tau must be non-empty
    public static native int functionType(int range, int ... domain); // domain -> range

    /*
     * Accessors and checks on a type
     * - see the Yices documentation for error codes
     */
    public static native boolean typeIsBool(int tau);
    public static native boolean typeIsInt(int tau);
    public static native boolean typeIsReal(int tau);
    public static native boolean typeIsArithmetic(int tau);
    public static native boolean typeIsBitvector(int tau);
    public static native boolean typeIsScalar(int tau);
    public static native boolean typeIsUninterpreted(int tau);
    public static native boolean typeIsTuple(int tau);
    public static native boolean typeIsFunction(int tau);

    public static native boolean isSubtype(int tau, int sigma);
    public static native boolean areCompatible(int tau, int sigma);
    public static native int bvTypeSize(int tau);
    public static native int scalarTypeCard(int tau);

    public static native int typeNumChildren(int tau);
    public static native int typeChild(int tau, int i);

    // all children of tau or NULL if tau is not a valid type
    public static native int[] typeChildren(int tau);

    /*
     * Names
     */
    public static native int setTypeName(int tau, String name);
    public static native String getTypeName(int tau);
    public static native int getTypeByName(String name);
    public static native void removeTypeName(String name);
    public static native int clearTypeName(int tau);

    // Pretty print as a string.
    public static native String typeToString(int tau);

    // Parsing (Yices syntax)
    public static native int parseType(String s);

    /*
     * TERMS
     */

    /*
     * Constructors: term are represented by non-negative int32
     * The constructors return -1 on error.
     */
    public static native int mkTrue();
    public static native int mkFalse();
    public static native int mkConstant(int tau, int index);
    public static native int newUninterpretedTerm(int tau);
    public static native int newVariable(int tau);
    public static native int funApplication(int fun, int... arg);
    public static native int ifThenElse(int cond, int iftrue, int iffalse);
    public static native int eq(int left, int right);
    public static native int neq(int left, int right); // not equal
    public static native int not(int arg);
    public static native int and(int... arg);
    public static native int or(int... arg);
    public static native int xor(int... arg);
    public static native int iff(int left, int right);
    public static native int implies(int left, int right);
    public static native int tuple(int... arg);
    public static native int select(int idx, int tuple);
    public static native int tupleUpdate(int tuple, int idx, int newval);
    public static native int functionUpdate(int fun, int[] arg, int newval);
    // Update1 is the common case where arg[] is a single argument. FIXME: iam says why the 1 on the end? this isn't C.
    public static native int functionUpdate1(int fun, int arg, int newval);
    public static native int distinct(int... arg);
    public static native int forall(int[] vars, int body);
    public static native int exists(int[] vars, int body);
    public static native int lambda(int[] vars, int body);

    /*
     * Arithmetic terms
     */
    public static native int zero();
    public static native int mkIntConstant(long x); // same as yices_int64
    public static native int mkRationalConstant(long num, long den); // yices_rational64

    // FOR TESTING ONLY
    public static native byte[] testMpzToBytes(String s);
    public static native void testBytesToMpz(byte[] b);

    private static native int bytesToIntConstant(byte[] b);
    public static int mkIntConstant(BigInteger z) {
        return bytesToIntConstant(z.toByteArray());
    }

    private static native int bytesToRationalConstant(byte[] num, byte[] den);
    public static int mkRationalConstant(BigRational r) {
        return bytesToRationalConstant(r.numToBytes(), r.denToBytes());
    }

    public static int mkRationalConstant(BigInteger num, BigInteger den) {
        BigRational r = new BigRational(num, den);
        r.normalize();
        return mkRationalConstant(r);
    }

    public static native int parseRational(String s);
    public static native int parseFloat(String s);
    public static native int add(int left, int right);
    public static native int sub(int left, int right);
    public static native int neg(int arg); // unary minus
    public static native int mul(int left, int right);
    public static native int square(int arg);
    public static native int power(int arg, int exponent); // returns -1 if exponent < 0
    public static native int add(int... arg);    // n-ary sum
    public static native int mul(int... arg);    // n-ary product
    public static native int div(int x, int y);  // real division
    public static native int idiv(int x, int y); // integer division
    public static native int imod(int x, int y); // modulo
    public static native int abs(int x);
    public static native int floor(int x);
    public static native int ceil(int x);
    public static native int intPoly(long[] coeff, int[] t);
    public static native int rationalPoly(long[] num, long[] den, int[] t);

    public static native int divides(int x, int y);
    public static native int isInt(int x);
    public static native int arithEq(int x, int y);  // same as eq(x, y)
    public static native int arithNeq(int x, int y); // same as neq(x, y);
    public static native int arithGeq(int x, int y); // x >= y
    public static native int arithLeq(int x, int y); // x <= y
    public static native int arithGt(int x, int y);  // x > y
    public static native int arithLt(int x, int y);  // x < y
    public static native int arithEq0(int x);        // x = 0
    public static native int arithNeq0(int x);       // x /= 0
    public static native int arithGeq0(int x);       // x >= 0
    public static native int arithLeq0(int x);       // x <= 0
    public static native int arithGt0(int x);        // x > 0
    public static native int arithLt0(int x);        // x < 0

    /*
     * BITVECTOR TERMS
     */

    /*
     * The yices API uses uint32_t parameters to pass the number of bits.
     * The constructors below use int n and will return -1 (fail) if n is < 0.
     * They also return -1 under for other errors detected by Yices.
     */
    public static native int bvConst(int n, long x); // see yices_bvconst_int64
    public static native int bvZero(int n);          // n = number of bits
    public static native int bvOne(int n);           // 0b00001 where n = number of bits
    public static native int bvMinusOne(int n);      // 0b11111 where n = number of bits
    // this converts x to a bitvector: x[0] = low-order bit, x[n-1] = high-order bit,
    // where n = array length (n must be positive).
    public static native int bvConstFromIntArray(int... x);
    public static native int parseBvBin(String s);
    public static native int parseBvHex(String x);

    public static native int bvAdd(int left, int right);
    public static native int bvSub(int left, int right);
    public static native int bvNeg(int arg); // 2s complement negation
    public static native int bvMul(int left, int right);
    public static native int bvSquare(int arg);
    public static native int bvPower(int arg, int exponent); // returns -1 if exponent < 0
    public static native int bvDiv(int left, int right); // unsigned division
    public static native int bvRem(int left, int right); // unsigned
    public static native int bvSDiv(int left, int right); // signed division
    public static native int bvSRem(int left, int right); // signed
    public static native int bvSMod(int left, int right); // signed

    public static native int bvNot(int arg);
    public static native int bvAnd(int left, int right);
    public static native int bvOr(int left, int right);
    public static native int bvXor(int left, int right);
    public static native int bvNand(int left, int right);
    public static native int bvNor(int left, int right);
    public static native int bvXNor(int left, int right);
    public static native int bvShl(int left, int right);
    public static native int bvLshr(int left, int right);
    public static native int bvAshr(int left, int right);

    // n-ary variants for the most common associative operations
    public static native int bvAdd(int... arg);
    public static native int bvMul(int ... arg);
    public static native int bvAnd(int... arg);
    public static native int bvOr(int... arg);
    public static native int bvXor(int... arg);

    // shift by constants: n is not a term here, it's the shift amount
    // it n is negative, the functions do return -1
    public static native int bvShiftLeft0(int arg, int n);
    public static native int bvShiftLeft1(int arg, int n);
    public static native int bvShiftRight0(int arg, int n);
    public static native int bvShiftRight1(int arg, int n);
    public static native int bvAShiftRight(int arg, int n); // arithmetic shift
    public static native int bvRotateLeft(int arg, int n);
    public static native int bvRotateRight(int arg, int n);

    // extract a[i:j] from a[0 .. n-1] where n = size of a
    // fails if i<0 or j<0
    public static native int bvExtract(int a, int i, int j);

    // extract bit i from a[0 ... n-1]: the result is a Boolean term
    public static native int bvExtractBit(int a, int i);

    // convert an array of boolean terms into a bitvector
    // a[0] = low-order bit of the result, a[n-1] = high-order bit
    public static native int bvFromBoolArray(int... a);

    // concat: high-order bits are from the left
    public static native int bvConcat(int left, int right);
    public static native int bvConcat(int... a);
    public static native int bvRepeat(int a, int n); // n copies of a concatenated

    public static native int bvSignExtend(int arg, int n); // add n bits
    public static native int bvZeroExtend(int arg, int n);

    // obscure operations
    public static native int bvRedAnd(int arg);
    public static native int bvRedOr(int arg);
    public static native int bvRedComp(int left, int right);

    // Atoms
    public static native int bvEq(int left, int right);
    public static native int bvNeq(int left, int right);

    // unsigned comparisons
    public static native int bvGe(int left, int right);
    public static native int bvGt(int left, int right);
    public static native int bvLe(int left, int right);
    public static native int bvLt(int left, int right);

    // signed comparisons
    public static native int bvSGe(int left, int right);
    public static native int bvSGt(int left, int right);
    public static native int bvSLe(int left, int right);
    public static native int bvSLt(int left, int right);

    /*
     * Accessors and checks on term x
     */
    public static native int typeOfTerm(int x);
    public static native boolean termIsBool(int x);
    public static native boolean termIsInt(int x);
    public static native boolean termIsReal(int x);
    public static native boolean termIsArithmetic(int x);
    public static native boolean termIsBitvector(int x);
    public static native boolean termIsTuple(int x);    // i.e., does x have type (tuple ...)
    public static native boolean termIsFunction(int x); // i.e., does x have a function type
    public static native boolean termIsScalar(int x);   // i.e., does x have a scalar or uninterpreted type

    public static native int termBitSize(int x);  // returns 0 if x is not a bitvector term
    public static native boolean termIsGround(int x); // no free variables in x

    public static native boolean termIsAtomic(int x);
    public static native boolean termIsComposite(int x);
    public static native boolean termIsProjection(int x);
    public static native boolean termIsSum(int x);
    public static native boolean termIsBvSum(int x);
    public static native boolean termIsProduct(int x);

    // The native method returns a term constructor as an integer constant.
    // The corresponding enum is in Constructor.java
    public static native int termConstructor(int x);
    public static native int termNumChildren(int x);
    public static native int termChild(int x, int idx);

    // all children of x or NULL if x is not a valid term
    public static native int[] termChildren(int x);
    public static native int termProjIndex(int x);
    public static native int termProjArg(int x);

    /*
     * Values of constant terms
     * To access the value of rational constants, we provide two functions:
     * one to get the numerator and one to get the denominator.
     * Both return arrays of bytes suitable to use in BigInteger.
     */
    public static native int boolConstValue(int x);      // returns -1 for error, 0 for false, 1 for true
    public static native int scalarConstantIndex(int x); // -1 for error
    public static native boolean[] bvConstValue(int x);  // null for error, or an array of n Booleans little-endian
    private static native byte[] rationalConstNumAsBytes(int x); // null for error
    private static native byte[] rationalConstDenAsBytes(int x); // null for error

    public static BigRational rationalConstValue(int x) {
        byte[] num = rationalConstNumAsBytes(x);
        byte[] den = rationalConstDenAsBytes(x);
        if (num != null && den != null) {
            return new BigRational(num, den);
        } else {
            return null;
        }
    }

    /*
     * Names
     */
    public static native int setTermName(int t, String name);
    public static native String getTermName(int t);
    public static native int getTermByName(String name);
    public static native void removeTermName(String name);
    public static native int clearTermName(int t);


    /*
     * Pretty print t then convert to a String
     * - the first variant prints t in a box of numLines x numColumns
     * - the second variant uses numLines = 30, numColumns = 80
     */
    public static native String termToString(int t, int numColumns, int numLines);
    public static native String termToString(int t);

    // Parsing of a term (Yices syntax)
    public static native int parseTerm(String s);

    /*
     * Substitutions
     *
     * A substitution is defined by two arrays: v = variable array, map = term array.
     * They must have the same length.
     *
     * substTerm(t, v, map): apply the substitution to t or return -1 if there's an error.
     * substTermArray(a, v, map): apply the substitution to every term a[i]. Return 0 if this
     * works or -1 if there's an error. If there's an error, a is unchanged.
     */
    public static native int substTerm(int t, int[] v, int[] map);
    public static native int substTermArray(int[] a, int[] v, int[] map);


    /*
     * GARBAGE COLLECTION
     */
    public static native int yicesNumTerms();
    public static native int yicesNumTypes();

    public static native int yicesIncrefTerm(int t);
    public static native int yicesDecrefTerm(int t);
    public static native int yicesIncrefType(int tau);
    public static native int yicesDecrefType(int tau);
    public static native int yicesNumPosrefTerms();
    public static native int yicesNumPosrefTypes();

    public static native void yicesGarbageCollect(int[] rootTerms, int[] rootTypes, boolean keepNamed);

    public static void yicesGarbageCollect(boolean keepNamed) {
    yicesGarbageCollect(null, null, keepNamed);
    }

    public static void yicesGarbageCollect() {
        yicesGarbageCollect(null, null, false);
    }

    /*
     * CONTEXTS AND SOLVERS
     */
    public static native long newConfig();
    public static native void freeConfig(long config);
    public static native int setConfig(long config, String name, String value);
    public static native int defaultConfigForLogic(long config, String logic);
    public static native long newContext(long config);
    public static native void freeContext(long ctx);
    public static native int contextStatus(long ctx);
    public static native void resetContext(long ctx);

    public static native int push(long ctx);
    public static native int pop(long ctx);
    public static native int contextEnableOption(long ctx, String option);
    public static native int contextDisableOption(long ctx, String option);
    public static native int assertFormula(long ctx, int t);
    public static native int assertFormulas(long ctx, int[] t);
    public static native int checkContext(long ctx, long params);
    public static native int assertBlockingClause(long ctx);
    public static native void stopSearch(long ctx);
    public static native long newParamRecord();
    public static native void defaultParamsForContext(long ctx, long params);
    public static native int setParam(long p, String pname, String value);
    public static native void freeParamRecord(long param);

    /*
     * MODELS
     */
    public static native long getModel(long ctx, int keep_subst);
    public static native void freeModel(long model);
    public static native long modelFromMap(int [] var, int [] map);

    // getBoolValue returns the value of term t in model.
    // It returns -1 for error, 0 for false, +1 for true
    public static native int getBoolValue(long model, int t);

    // These three methods return -1 for error and 0 otherwise.
    // The value of term t in model 'model' is returned in array a.
    //
    // For getIntValue, a must be an array of one element.
    // - if t has an integer value that fits in 64bits,
    //   this value is returned in a[0]
    // For getRationalValue, a must be an array of two elements:
    // - if t has a value that can be written den/num where
    //   both den and num fit in 64 bits then den is returned in a[0]
    //   and num is returned in a[1]
    // For getDoubleValue, the value is returned in a[0]
    public static native int getIntegerValue(long model, int t, long[] a);
    public static native int getRationalValue(long model, int t, long[] a);
    public static native int getDoubleValue(long model, int t, double[] a);

    // These return arrays of bytes suitable for conversion to BigInteger.
    // They return null if there's an error.
    private static native byte[] getIntegerValueAsBytes(long model, int t);
    private static native byte[] getRationalValueNumAsBytes(long model, int t);
    private static native byte[] getRationalValueDenAsBytes(long model, int t);

    public static BigInteger getIntegerValue(long model, int t) {
        byte[] val = getIntegerValueAsBytes(model, t);
        return val != null ? new BigInteger(val) : null;
    }

    public static BigRational getRationalValue(long model, int t) {
        byte[] num = getRationalValueNumAsBytes(model, t);
        byte[] den = getRationalValueDenAsBytes(model, t);
        if (num != null && den != null) {
            return new BigRational(num, den);
        } else {
            return null;
        }
    }

    // Value of a bitvector term: the result is little endian
    // return null if there's an error
    public static native boolean[] getBvValue(long model, int t);

    // Value (i.e., index) of a scalar or uninterpreted term
    // return -1 if there's an error.
    public static native int getScalarValue(long model, int t);

    /*
     * Value of t converted to a constant term.
     * - returns -1 if there's an error
     */
    public static native int valueAsTerm(long model, int t);


    /*
     * Get the values of terms a[0 .. n-1] in mdl and convert the values to terms.
     * - a must be an array of n terms
     * - b must be large enough to store n terms
     *
     * This function has the same behavior and limitations as yices_get_value_as_term.
     * If there's no error, the function returns 0 and store the values in array b:
     * - b[i] = value of a[i] in mdl, converted to a term
     *
     * Otherwise, the function returns -1 and sets the error report.
     * The error codes are the same as for yices_get_value_as_term.
     * FIXME: shouldn't there be an additional error code for when b isn't long enough?
     * Here and in the API!
     */
    public static native int valuesAsTerms(long model, int[] in, int[] out);

    /*
     * Export the model as a String (pretty printing).
     *
     * Return null if something is wrong.
     *
     * The first version prints the model in a box of ncol x nlines
     * The second version uses ncol = 80, nlines = 2^32-1
     */
    public static native String modelToString(long model, int numColumns, int numLines);
    public static native String modelToString(long model);


    /*
     * Check whether the given delegate is supported
     * - return false if it's not supported.
     * - return true if delegate is the name of a supported delegate
     *
     * Which delegate is supported depends on how the Yices' dynamic library was compiled.
     *
     * Since 2.6.2.
     */
    public static native boolean hasDelegate(String delegate);

    /*
     * Check whether a formula is satisfiable
     * - f = formula
     * - logic = SMT name for a logic (or NULL)
     * - model = resulting model (or NULL if no model is needed)
     * - delegate = external solver to use or NULL
     *
     * This function first checks whether f is trivially sat or trivially unsat.
     * If not, it constructs a context configured for the specified logic, then
     * asserts f in this context and checks whether the context is satisfiable.
     *
     * The return value is
     *   STATUS_SAT if f is satisfiable,
     *   STATUS_UNSAT if f is not satisifiable
     *   STATUS_ERROR if something goes wrong
     *
     * Since 2.6.2.
     */
    // model array can be null, in which case no model is constructed, otherwise a "model" is placed in model[0]
    public static native int checkFormula(int t, String logic, String delegate, long[] model);

    /*
     * Check whether n formulas are satisfiable.
     * - f = array of n Boolean terms
     * - n = number of elements in f
     *
     * This is similar to yices_check_formula except that it checks whether
     * the conjunction of f[0] ... f[n-1] is satisfiable.
     *
     * Since 2.6.2.
     */
    // model array can be null, in which case no model is constructed, otherwise a "model" is placed in model[0]
    public static native int checkFormulas(int[] terms, String logic, String delegate, long[] model);


    /*
     * Compute an implicant for t in model
     * - t must be a Boolean term that's true in model
     * - the implicant is a list of Boolean terms a[0] ... a[n-1] such that
     *    1) a[i] is a literal (atom or negation of an atom)
     *    2) a[i] is true in model
     *    3) the conjunction a[0] /\ ... /\ a[n-1] implies t
     *
     * The implicant is returned in an int array, or null indicating an error.
     */
    public static native int[] implicantForFormula(long model, int t);

    /*
     * Compute an implicant for an array of terms in model
     * - t must be an array of Boolean terms, each being true in model
     * - the implicant is a list of Boolean terms a[0] ... a[n-1] such that
     *    1) a[i] is a literal (atom or negation of an atom)
     *    2) a[i] is true in model
     *    3) the conjunction a[0] /\ ... /\ a[n-1] implies (and terms[0] ... terms[m-1]).
     *
     * The implicant is returned in an int array, or null indicating an error.
     */
    public static native int[] implicantForFormulas(long model, int[] terms);


    /*
     * Compute a generalization of model for formula t
     * - elim[] = variables to eliminate
     * - each term in elim[i] must be an uninterpreted term of one of the
     * following types: Boolean, (bitvector k), or Real
     * - mode defines the generalization algorithm
     *
     * returns the array of formulas.
     *
     * If mode = GEN_BY_PROJ, then every element of v is guaranteed to be a literal
     *
     * Important: t must be true in model, otherwise, the returned data may be garbage.
     *
     */
    public static native int[] generalizeModel(long model, int term, int[] elims, int mode);
    public static native int[] generalizeModel(long model, int[] terms, int[] elims, int mode);

    /*
     * Bit-blast then export the CNF to a file
     * - f = a Boolean formula (in the QF_BV theory)
     * - filename = name of the ouput file
     * - simplify_cnf = boolean flag
     * - status = an array to store the ordinal of the status.
     *  returns:
     *   1 if the DIMACS file was constructed
     *   0 if the formula is solved without CNF or after simplifying
     *  -1 if there's an error
     */
    public static native int exportToDimacs(int term, String filename, boolean simplify_cnf, int[] status);

    /*
     * Bit-blast n formulas then export the CNF to a file
     * - f = array of n Boolean formula (in the QF_BV theory)
     * - filename = name of the ouput file
     * - simplify_cnf = boolean flag
     *  returns:
     *   1 if the DIMACS file was constructed
     *   0 if the formula is solved without CNF or after simplifying
     *  -1 if there's an error
     */
    public static native int exportToDimacs(int[] terms, String filename, boolean simplify_cnf, int[] status);

    /*
     * Given a term t and a model 'model', the support of t in model is a set of uninterpreted
     * terms whose values are sufficient to fix the value of t in model. For example, if
     * t is (if x>0 then x+z else y) and x has value 1 in model, then the value of t doesn't depend
     * on the value of y in model. In this case, support(t) = [ x, z ].
     */
    public static native int[] getSupport(long model, int term);
    public static native int[] getSupport(long model, int[] terms);

    public static native YVal getValue(long model, int term);

    public static native boolean valIsInt(long model, int tag, int id);
    public static native boolean valIsLong(long model, int tag, int id);
    public static native boolean valIsInteger(long model, int tag, int id);

    public static native int valBitSize(long model, int tag, int id);

    public static native int valTupleArity(long model, int tag, int id);

    public static native int valMappingArity(long model, int tag, int id);

    public static native int valFunctionArity(long model, int tag, int id);

    // new in 2.6.2
    public static native int valFunctionType(long model, int tag, int id);

    // getBoolValue returns the value of yval { tag, id} in model.
    // It returns -1 for error, 0 for false, +1 for true
    public static native int valGetBool(long model, int tag, int id);

    // These three methods return -1 for error and 0 otherwise.
    // The value of term t in model 'model' is returned in array a.
    //
    // For valGetInteger, a must be an array of one element.
    // - if t has an integer value that fits in 64bits,
    //   this value is returned in a[0]
    // For valGetRational, a must be an array of two elements:
    // - if t has a value that can be written den/num where
    //   both den and num fit in 64 bits then den is returned in a[0]
    //   and num is returned in a[1]
    // For valGetDouble, the value is returned in a[0]
    public static native int valGetInteger(long model, int tag, int id, long[] a);
    public static native int valGetRational(long model, int tag, int id, long[] a);
    public static native int valGetDouble(long model, int tag, int id, double[] a);

    public static native boolean[] valGetBV(long model, int tag, int id);


    // These return arrays of bytes suitable for conversion to BigInteger.
    // They return null if there's an error.
    private static native byte[] valGetIntegerAsBytes(long model, int tag, int id);
    private static native byte[] valGetRationalNumAsBytes(long model, int tag, int id);
    private static native byte[] valGetRationalDenAsBytes(long model, int tag, int id);

    public static BigInteger valGetInteger(long model, int tag, int id) {
        byte[] val = valGetIntegerAsBytes(model, tag, id);
        return val != null ? new BigInteger(val) : null;
    }

    public static BigRational valGetRational(long model, int tag, int id) {
        byte[] num = valGetRationalNumAsBytes(model, tag, id);
        byte[] den = valGetRationalDenAsBytes(model, tag, id);
        if (num != null && den != null) {
            return new BigRational(num, den);
        } else {
            return null;
        }
    }


    // Value (i.e., index) of a scalar or uninterpreted term
    // return 0 on success. -1 or -2 if there's an error.
    // -1 if the tag isn't kosher.
    // -2 if the array a isn't of length 2
    // -3 if the yval isn't of a scalar
    // stores the scalar val in a[0] and the type in a[1]

    public static native int valGetScalar(long model, int tag, int id, int[] a);


    // public static native int yices_val_expand_tuple(model_t *model, const yval_t *v, yval_t child[]);
    // returns 0 on success and stores the children in the "children: array
    // returns a negative number if something is wrong:
    // -1 the tag is no good.
    // -2 the given yval is not a tuple
    // -3 if the children array is not big enough
    // -4 if getting the children goes pear shapped (the yices error struct should have the reason why)
    public static native int valExpandTuple(long model, int tag, int id, YVal[] children);

    // Returns the number of mappings a function has, a negative value indicates an error.
    public static native int valFunctionCardinality(long model, int tag, int id);

    // public static native int yices_val_expand_function(model_t *model, const yval_t *f, yval_t *def, yval_vector_t *v);
    public static native int valExpandFunction(long model, int tag, int id, YVal[] def, YVal[] mappings);

    // public static native int yices_val_expand_mapping(model_t *model, const yval_t *m, yval_t tup[], yval_t *val);
    public static native int valExpandMapping(long model, int tag, int id, YVal[] args, YVal[] value);


    /* <TooHardBasket> */
    // public static native int yices_val_get_algebraic_number(model_t *model, const yval_t *v, lp_algebraic_number_t *a);
    // public static native int yices_pp_model_fd(int fd, model_t *model, uint width, uint height, uint offset);
    /* </TooHardBasket> */


}
