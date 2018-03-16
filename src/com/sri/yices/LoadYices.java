package com.sri.yices;

public class LoadYices {

    private static void testTypes() {
		int boolType = Yices.boolType();
		int intType = Yices.intType();
		int realType = Yices.realType();
		int bv1 = Yices.bvType(1);
		int bv2 = Yices.bvType(2);
		int bv32 = Yices.bvType(32);
		int scalar = Yices.newScalarType(4);
		int t = Yices.newUninterpretedType();
		int u = Yices.newUninterpretedType();

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

	// Tests of incorrect types
	int bv0 = Yices.bvType(0);
	System.out.println("Bad type: bv0 = " + bv0);
	System.out.println("Error: " + Yices.errorString());

	int bvneg = Yices.bvType(-100);
	System.out.println("Bad type: bvneg = " + bvneg);
	System.out.println("Error: " + Yices.errorString());

	int bvlarge = Yices.bvType(Integer.MAX_VALUE);
	System.out.println("Bad type: bvlarge = " + bvlarge);
	System.out.println("Error: " + Yices.errorString());

	int[] a = new int[0];
	int badTuple = Yices.tupleType(a);
	System.out.println("badTuple: " + badTuple);
	System.out.println("Error: " + Yices.errorString());

	// Tuple and function types
	a = new int[] { intType, realType };
	int tuple = Yices.tupleType(a);
	System.out.println("tuple: " + tuple);
	int code = Yices.setTypeName(tuple, "the_tuple_type");
	if (code < 0) {
	    System.out.println("failed in setTypeName for tuple");
	} else {
	    System.out.println("name of type " + tuple + " is " + Yices.getTypeName(tuple));
	}

	int fun = Yices.functionType(boolType, a);
	System.out.println("fun: " + fun);
	code = Yices.setTypeName(fun, "the_function_type");
	if (code < 0) {
	    System.out.println("failed in setTypeName for function");
	} else {
	    System.out.println("name of type " + fun + " is " + Yices.getTypeName(fun));
	}

	// Use the names
	int test = Yices.getTypeByName("the_tuple_type");
	if (test == tuple) {
	    System.out.println("Retrieved 'the_tuple_type': got " + test);
	} else {
	    System.out.println("Failed in getTypeByName: expected " + tuple + ", got " + test);
	}

    	test = Yices.getTypeByName("the_function_type");
	if (test == fun) {
	    System.out.println("Retrieved 'the_function_type': got " + test);
	} else {
	    System.out.println("Failed in getTypeByName: expected " + fun + ", got " + test);
	}

	test = Yices.getTypeByName("not_there");
	if (test != -1) {
	    System.out.println("Failed in getTypeByName: expected -1, got " + test);
	}

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
    }

    public static void main(String[] arg) {
	System.out.println("Loaded Yices version " + Yices.version());
	System.out.println("Built for " + Yices.buildArch());
	System.out.println("Build mode: " + Yices.buildMode());
	System.out.println("Build date: " + Yices.buildDate());
	System.out.println("MCSat supported: " + Yices.hasMcsat());
	System.out.println("Yices error: " + Yices.errorString());
	testTypes();

	Yices.testException();
    }
}
