package com.sri.yices;

import java.util.EnumSet;

/**
 * The Yices term constructors as defined in yices_types.h
 */
public enum Constructor {
    CONSTRUCTOR_ERROR(-1),   // to report an error

    // atomic terms
    BOOL_CONSTANT(0),        // boolean constant
    ARITH_CONSTANT(1),       // rational constant
    BV_CONSTANT(2),          // bitvector constant
    SCALAR_CONSTANT(3),      // constant of uninterpreted or scalar type
    VARIABLE(4),             // variable in quantifiers
    UNINTERPRETED_TERM(5),   // (i.e., global variables, can't be bound)

    // composite terms
    ITE_TERM(6),             // if-then-else
    APP_TERM(7),             // application of an uninterpreted function
    UPDATE_TERM(8),          // function update
    TUPLE_TERM(9),           // tuple constructor
    EQ_TERM(10),             // equality
    DISTINCT_TERM(11),       // distinct t_1 ... t_n
    FORALL_TERM(12),         // quantifier
    LAMBDA_TERM(13),         // lambda
    NOT_TERM(14),            // (not t)
    OR_TERM(15),             // n-ary OR
    XOR_TERM(16),            // n-ary XOR

    BV_ARRAY(17),            // array of boolean terms
    BV_DIV(18),              // unsigned division
    BV_REM(19),              // unsigned remainder
    BV_SDIV(20),             // signed division
    BV_SREM(21),             // remainder in signed division (rounding to 0)
    BV_SMOD(22),             // remainder in signed division (rounding to -infinity)
    BV_SHL(23),              // shift left (padding with 0)
    BV_LSHR(24),             // logical shift right (padding with 0)
    BV_ASHR(25),             // arithmetic shift right (padding with sign bit)
    BV_GE_ATOM(26),          // unsigned comparison: (t1 >= t2)
    BV_SGE_ATOM(27),         // signed comparison (t1 >= t2)
    ARITH_GE_ATOM(28),       // atom (t1 >= t2) for arithmetic terms: t2 is always 0
    ARITH_ROOT_ATOM(29),     // atom (0 <= k <= root_count(p)) && (x r root(p, k)) for r in <, <=, ==, !=, >, >=

    ABS(30),                 // absolute value
    CEIL(31),                // ceil
    FLOOR(32),               // floor
    RDIV(33),                // real division (as in x/y)
    IDIV(34),                // integer division
    IMOD(35),                // modulo
    IS_INT_ATOM(36),         // integrality test: (is-int t)
    DIVIDES_ATOM(37),        // divisibility test: (divides t1 t2)

    // projections
    SELECT_TERM(38),         // tuple projection
    BIT_TERM(39),            // bit-select: extract the i-th bit of a bitvector

    // sums
    BV_SUM(40),              // sum of pairs a * t where a is a bitvector constant (and t is a bitvector term)
    ARITH_SUM(41),           // sum of pairs a * t where a is a rational (and t is an arithmetic term)

    // products
    POWER_PRODUCT(42)        // power products: (t1^d1 * ... * t_n^d_n)
    ;

    private int index;
    private Constructor(int id) { this.index = id; }
    public int getIndex() { return index; }

    public static final int NUM_CONSTRUCTORS = 43;
    private static final Constructor[] table;

    static {
        table = new Constructor[NUM_CONSTRUCTORS];
        for (Constructor c: EnumSet.allOf(Constructor.class)) {
            int i = c.getIndex();
            if (i >= 0) table[i] = c;
        }
    }

    public static Constructor idToConstructor(int i) {
        if (i < 0 || i >= table.length) return CONSTRUCTOR_ERROR;
        return table[i];
    }
}
