package com.sri.yices;

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
 */
public enum GeneralizationMode {
    GEN_DEFAULT,
    GEN_BY_SUBST,
    GEN_BY_PROJ;

    private static final GeneralizationMode[] table = GeneralizationMode.values();

    public static GeneralizationMode idToMode(int i) {
        if (i < 0 || i >= table.length) return GEN_DEFAULT;
        return table[i];
    }

}
