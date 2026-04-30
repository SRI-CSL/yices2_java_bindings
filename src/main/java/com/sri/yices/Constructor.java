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
    FF_CONSTANT(2),          // finite field constant
    BV_CONSTANT(3),          // bitvector constant
    SCALAR_CONSTANT(4),      // constant of uninterpreted or scalar type
    VARIABLE(5),             // variable in quantifiers
    UNINTERPRETED_TERM(6),   // (i.e., global variables, can't be bound)

    // composite terms
    ITE_TERM(7),             // if-then-else
    APP_TERM(8),             // application of an uninterpreted function
    UPDATE_TERM(9),          // function update
    TUPLE_TERM(10),          // tuple constructor
    EQ_TERM(11),             // equality
    DISTINCT_TERM(12),       // distinct t_1 ... t_n
    FORALL_TERM(13),         // quantifier
    LAMBDA_TERM(14),         // lambda
    NOT_TERM(15),            // (not t)
    OR_TERM(16),             // n-ary OR
    XOR_TERM(17),            // n-ary XOR

    BV_ARRAY(18),            // array of boolean terms
    BV_DIV(19),              // unsigned division
    BV_REM(20),              // unsigned remainder
    BV_SDIV(21),             // signed division
    BV_SREM(22),             // remainder in signed division (rounding to 0)
    BV_SMOD(23),             // remainder in signed division (rounding to -infinity)
    BV_SHL(24),              // shift left (padding with 0)
    BV_LSHR(25),             // logical shift right (padding with 0)
    BV_ASHR(26),             // arithmetic shift right (padding with sign bit)
    BV_GE_ATOM(27),          // unsigned comparison: (t1 >= t2)
    BV_SGE_ATOM(28),         // signed comparison (t1 >= t2)
    ARITH_GE_ATOM(29),       // atom (t1 >= t2) for arithmetic terms: t2 is always 0
    ARITH_ROOT_ATOM(30),     // atom (0 <= k <= root_count(p)) && (x r root(p, k)) for r in <, <=, ==, !=, >, >=

    ABS(31),                 // absolute value
    CEIL(32),                // ceil
    FLOOR(33),               // floor
    RDIV(34),                // real division (as in x/y)
    IDIV(35),                // integer division
    IMOD(36),                // modulo
    IS_INT_ATOM(37),         // integrality test: (is-int t)
    DIVIDES_ATOM(38),        // divisibility test: (divides t1 t2)

    // projections
    SELECT_TERM(39),         // tuple projection
    BIT_TERM(40),            // bit-select: extract the i-th bit of a bitvector

    // sums
    BV_SUM(41),              // sum of pairs a * t where a is a bitvector constant (and t is a bitvector term)
    ARITH_SUM(42),           // sum of pairs a * t where a is a rational (and t is an arithmetic term)
    FF_SUM(43),              // sum of pairs a * t where a is a finite-field constant (and t is a finite-field term)

    // products
    POWER_PRODUCT(44)        // power products: (t1^d1 * ... * t_n^d_n)
    ;

    private int index;
    Constructor(int id) { this.index = id; }
    public int getIndex() { return index; }

    public static final int NUM_CONSTRUCTORS = 45;
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
