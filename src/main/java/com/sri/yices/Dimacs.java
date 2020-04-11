package com.sri.yices;

/*
 * Bit-blast then export the CNF to a file
 */

public class Dimacs {


    /*
     * Bit-blast then export the CNF to a file
     * - f = a Boolean formula (in the QF_BV theory)
     * - filename = name of the ouput file
     * - simplify_cnf = boolean flag
     * - status = an array to store the formula's status.
     *  returns:
     *   true if the DIMACS file was constructed
     *   false if the formula is solved without CNF or after simplifying
     *   throws an exception if there's an error
     */
     public static boolean export(int term, String filename, boolean simplify, Status[] status) throws YicesException {
        if (status == null || status.length == 0){
            throw new IllegalArgumentException("status array null or too small");
        }
        int stat[] = { -1 };
        int code = Yices.exportToDimacs(term, filename, simplify, stat);
        if (code < 0) throw new YicesException();
        status[0] = Status.idToStatus(stat[0]);
        return code == 1;
    }

    /*
     * Bit-blast n formulas then export the CNF to a file
     * - f = array of n Boolean formula (in the QF_BV theory)
     * - filename = name of the ouput file
     * - simplify_cnf = boolean flag
     * - status = an array to store the status of the formulas.
     *  returns:
     *   true if the DIMACS file was constructed
     *   false if the formula is solved without CNF or after simplifying
     *   throws an exception if there's an error
     */
    public static boolean export(int[] terms, String filename, boolean simplify, Status[] status) throws YicesException {
        if (status == null || status.length == 0){
            throw new IllegalArgumentException("status array null or too small");
        }
        int stat[] = { -1 };
        int code = Yices.exportToDimacs(terms, filename, simplify, stat);
        if (code < 0) throw new YicesException();
        status[0] = Status.idToStatus(stat[0]);
        return code == 1;
    }




}
