package com.sri.yices;

/*
 * The value of a term in a model can be an atomic value, a tuple, or
 * a function. Internally, Yices represents tuple and function values
 * as nodes in a DAG. The API provides functions to compute and
 * examine these nodes, which gives access to the values of terms of
 * function or tuple types. Every node in this DAG has a unique id and
 * a tag of type YValTag that defines the node type
 */
public enum YValTag {
    UNKNOWN,
    BOOL,
    RATIONAL,
    ALGEBRAIC,
    BV,
    SCALAR,
    TUPLE,
    FUNCTION,
    MAPPING;

    private static final YValTag[] table = YValTag.values();

    public static YValTag idToTag(int i) {
        if (i < 0 || i >= table.length) return UNKNOWN;
        return table[i];
    }

}
