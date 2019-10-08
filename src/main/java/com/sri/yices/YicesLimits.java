package com.sri.yices;

/**
 * Various constants defined in yices_limits.h.
 * The yices definitions use UINT32_MAX
 */
public final class YicesLimits {
    // Maximal number of terms and types
    public final static int MAX_TYPES = Integer.MAX_VALUE/4;
    public final static int MAX_TERMS = Integer.MAX_VALUE/4;

    // Maximal arity
    public final static int MAX_ARITY = Integer.MAX_VALUE/8;

    // Maximal polynomial degree
    public final static int MAX_DEGREE = Integer.MAX_VALUE;

    // Maximal number of variables in quantifiers/lambdas
    public final static int MAX_VARS = Integer.MAX_VALUE/8;

    // Maximal bitvector size
    public final static int MAX_BVSIZE = Integer.MAX_VALUE/8;
}
