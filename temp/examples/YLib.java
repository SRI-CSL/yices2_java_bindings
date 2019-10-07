import com.sri.yices.Config;
import com.sri.yices.Context;

/*
 * Some useful routines for my examples.
 *
 */


public class YLib {

    /* Create a QF_NRA, non-linear real arithmetic, context. */
    public static Context makeContext(){
	Config cfg = new Config("QF_NRA");
	cfg.set("mode", "one-shot");
	Context ctx = new Context(cfg);
	cfg.close();
	return ctx;
    } 


}
