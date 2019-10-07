package com.sri.yices;

import java.util.EnumSet;

/**
 * Context status as defined in yices_types.h
 */
public enum Status {
    IDLE(0),
    SEARCHING(1),
    UNKNOWN(2),
    SAT(3),
    UNSAT(4),
    INTERRUPTED(5),
    ERROR(6);

    private int index;
    private Status(int id) { this.index = id; }
    public int getIndex() { return index; }

    public static final int NUM_STATUSES = 7;
    private static final Status[] table;

    static {
        table = new Status[NUM_STATUSES];
        for (Status s: EnumSet.allOf(Status.class)) {
            table[s.getIndex()] = s;
        }
    }

    public static Status idToStatus(int i) {
        if (i < 0 || i >= table.length) return ERROR;
        return table[i];
    }

}
