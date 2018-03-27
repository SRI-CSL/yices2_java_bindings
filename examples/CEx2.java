import com.sri.yices.Terms;
import com.sri.yices.Types;
import com.sri.yices.Model;

/*
In this example we use the iterative constraint solver suggested  by BD:

    Let x be a vector of integer and real variables. Suppose we want to
    solve a set of constraints C[x] while minimizing the value of F[x].
    
    One simple strategy is to first find an initial solution x0 to C:

    C[x0]

    then iteratively find x1, x2, x3, x4 ....

    such that

    C[x{n+1}]  and F(x{n+1}) < F(xn) - delta

    for some suitable delta.

In the example we do here 

    C is the conjuction of 
     -  b is in bit_range (2, 3, 4, 5, 6, 7, 8)
     -  t is in time_range (10, 20, 30, 40)
     -  e * t = k * b,   where k comes in from the command line
     -  e < d,   where d comes in from the command line


This should have the same behaviour as src/bindings/python/examples/constraints/exp2.py

*/


public class CEx2 {

    private static int[] BITRANGE = { 2, 3, 4, 5, 6, 7, 8 };

    private static int[] TRANGE = { 10, 20, 30, 40 };


    private static int make_in_range(int var, int[] range){
	int[] a = new int[range.length];
	for(int i = 0; i < range.length; i++){
	    a[i] = Terms.arithEq(var, Terms.intConst(range[i]));
	}
	return Terms.or(a);
    }
    

    public static void main(String[] args) throws Exception {
	String ks = "2", ds = "0.4";
	
	if(args.length != 2){
	    System.out.println("Usage: CEx0 <K a float> <D a float>\n");
	    System.out.println("\t(using defaults: CEx0 2 0.4)\n");
	    
	} else {

	System.out.format("Using: CEx0 %s %s\n", args[0], args[1]);
	
	try {

	    float pk = Float.parseFloat(args[0]);

	    float pd = Float.parseFloat(args[1]);

	} catch (NumberFormatException e){
	    System.out.println("Usage: CEx0 <K a FLOAT> <D a FLOAT>\n");
	    return;
	}
	
	ks = args[0];
	ds = args[1];

	}
	
	int k = Terms.parseFloat(ks);
	int d = Terms.parseFloat(ds);

	int b = Terms.newUninterpretedTerm("b", Types.intType());
	int t = Terms.newUninterpretedTerm("t", Types.intType());
	int e = Terms.newUninterpretedTerm("e", Types.realType());



	//construct the constraint set as a boolean term
	
	int c = makeC(b, t, e, k, d);

	//construct the measure we want to minimize

	int f = makeF(b, t, e, k, d);



	Solver solver = new Solver(c, f, d);

    
	Model solution = solver.iterate();


	if(solution != null){
	    solution.close();
	}
    }

    /* 
       Construct the boolean constraint C.
       
       C is the conjuction of 
       
       b is in bit_range
       t is in time_range
       e * t = k * b
       e < d

    */
    private static int makeC(int b, int t, int e, int k, int d){


	return Terms.and(make_in_range(b, BITRANGE),
			 make_in_range(t, TRANGE),
			 Terms.arithEq(Terms.mul(e, t), Terms.mul(k, b)),
			 Terms.arithLt(e, d));

    }

    /*
      Construct F the real valued term we want to minimize.

      F is the sum of:
      
      100 * t
      144 / b
      e
      
      This is designed so that we want to make t as small as possible, 
      then make b as big as possible, finally using e to break any ties.
      
      Other measures are certainly possible, for example we may want to
      maximize b above all, so the term would have to be adjusted so 
      that the b term dominated.

    */

    private static int makeF(int b, int t, int e, int k, int d){
	return Terms.add(Terms.mul(Terms.intConst(100), t),
			 Terms.div(Terms.intConst(144), b),
			 e);	
    }


}

