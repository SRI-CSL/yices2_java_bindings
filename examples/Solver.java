import com.sri.yices.Model;
import com.sri.yices.Context;
import com.sri.yices.Status;
import com.sri.yices.Terms;


/*
    This solver implements BD's suggestion:

        Let x be a vector of integer and real variables. Suppose we want to
        solve a set of constraints C[x] while minimizing the value of F[x].
        
        One simple strategy is to first find an initial solution x0 to C:
    
        C[x0]
    
        then iteratively find x1, x2, x3, x4 ....
    
        such that
    
        C[x{n+1}]  and F(x{n+1}) < F(xn) - delta
    
        for some suitable delta.

    We do it parametrically in order to decouple the algorithm actual from 
    the particular choice of C and F that we use in the example exp2.py.

*/


public class Solver {

    private final boolean DEBUG = false;


    private int c;
    private int f;
    private double delta;
    
    /*
     * Initializes the solver.
     *            - C is a boolean yices term
     *            - F is a real valued yices term
     *            - delta is a yices double constant
     */
    public Solver(int c, int f, double delta){
	this.c = c;
	this.f = f;
	this.delta = delta;
	if(DEBUG){
	    System.out.format("C = %s\nF = %s\ndelta = %s\n", Terms.toString(c), Terms.toString(f), delta);
	}
    }
    

    public Model solve(int phi){
	Model model = null;
	/* make the mcsat context */
	Context c = YLib.makeContext();

	c.assertFormula(phi);

	Status stat = c.check();
	if(stat == Status.SAT){
	    model = c.getModel();
	    if(DEBUG){
		System.out.format("Model for %s:\n%s\n", Terms.toString(phi), model);
	    }
	} else {
            if(DEBUG){
                System.out.format("The term:\n%s\n has NO solutions: smt_stat = %s\n", Terms.toString(phi), stat.name());
	    }
	}
	c.close();
	return model;
    }

    private int model2Constraint(Model model, int iteration){
	int retval = this.c;
	if(model != null){
	    int t = model.valueAsTerm(this.f);
	    double d = model.doubleValue(this.f);
	    System.out.format("Iteration %s: Bound = %s\n", iteration, d);
	    retval = Terms.and(this.c, Terms.arithLt(this.f, t));
	}
	return retval;
    }

    public Model iterate(){
	int iteration = 0;
	Model model = null;
	Model nextModel = null;
	int phi;
	
	while(true){
	    phi = model2Constraint(model, iteration);
	    nextModel = solve(phi);
	    if(nextModel != null){
		if(model != null){ model.close(); }
		model = nextModel;
		iteration++;
	    } else {
		break;
	    }
	}
	
	System.out.format("Iteration: %s\nModel: %s\n", iteration, model);
	return model;
    }



}

