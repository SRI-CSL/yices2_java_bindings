import com.sri.yices.Yices;
import com.sri.yices.Context;
import com.sri.yices.Terms;
import com.sri.yices.Model;
import com.sri.yices.Status;



public class MCSat {

    public static void main(String[] args){

	try {

	    int realType = Yices.realType();
	    int x = Terms.newUninterpretedTerm("x", realType);
	    int p = Terms.parse("(= (* x x) 2)");

	    /* make the mcsat context */
	    Context c = YLib.makeContext();

	    c.assertFormula(p);

	    Status stat = c.check();
	    if(stat == Status.SAT){
		Model m = c.getModel();
		System.out.format("Model for %s\n", Terms.toString(p));
		System.out.println(m);
	    }
	    
	    
	} catch(Exception e){
	    System.err.println(e);
	}
	


    }




}
