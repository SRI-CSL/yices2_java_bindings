package com.sri.yices;

import java.util.EnumSet;

/*
 * These codes define a generalization algorithm for functions
 *      yices_generalize_model
 * and  yices_generalize_model_array
 *
 * There are currently two algorithms: generalization by
 * substitution and generalization by projection.
 * The default is to select the algorithm based on variables
 * to eliminate.
 *
 * FIXME: This is based on Status.java which to me (iam) looks
 * extremely prosaic.
 */
public enum GeneralizationMode {
    GEN_DEFAULT(0),
    GEN_BY_SUBST(1),
    GEN_BY_PROJ(2);


    private int index;
    GeneralizationMode(int id) { this.index = id; }
    public int getIndex() { return index; }

    public static final int NUM_MODES = 3;
    private static final GeneralizationMode[] table;

    static {
        table = new GeneralizationMode[NUM_MODES];
        for (GeneralizationMode s: EnumSet.allOf(GeneralizationMode.class)) {
            table[s.getIndex()] = s;
        }
    }
    
    public static GeneralizationMode idToMode(int i) {
        if (i < 0 || i >= table.length) return GEN_DEFAULT;
        return table[i];
    }
    
}
