package com.sri.yices;

/**
 * Context status as defined in yices_types.h
 */
public enum Status {
    IDLE,
    SEARCHING,
    UNKNOWN,
    SAT,
    UNSAT,
    INTERRUPTED,
    ERROR;

    private static final Status[] table = Status.values();

    public static Status idToStatus(int i) {
        if (i < 0 || i >= table.length) return ERROR;
        return table[i];
    }

}
