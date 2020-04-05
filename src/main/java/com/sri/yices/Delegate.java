package com.sri.yices;

/**
 *  Providing access to yices's third party SAT solvers.
 *
 */
public class  Delegate {

    public static final String CADICAL = "cadical";
    public static final String CRYPTOMINISAT = "cryptominisat";
    public static final String Y2SAT = "y2sat";

    public static Status checkFormula(int term, String logic, String delegate){
	int status =  Yices.checkFormula(term, logic, delegate);
	return Status.idToStatus(status);
    }
    
    public static Model getModel(int term, String logic, String delegate){
	long mdl = Yices.getModelForFormula(term, logic, delegate);
	if (mdl != 0){
	    return new Model(mdl);
	}
	return null;
    }
    
    public static Status checkFormulas(int[] terms, String logic, String delegate){
	int status =  Yices.checkFormulas(terms, logic, delegate);
	return Status.idToStatus(status);
    }

    public static Model getModel(int[] terms, String logic, String delegate){
	long mdl = Yices.getModelForFormulas(terms, logic, delegate);
	if (mdl != 0){
	    return new Model(mdl);
	}
	return null;
    }
     




}
