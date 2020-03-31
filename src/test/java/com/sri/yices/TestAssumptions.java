package com.sri.yices;

/**
 * @author Huascar Sanchez
 */
public class TestAssumptions {
  public static final boolean IS_YICES_INSTALLED = TestAssumptions.isYicesInstalled();

  private TestAssumptions(){}

  private static boolean isYicesInstalled() {
    try {
	boolean success = Yices.isReady();
	System.out.println(String.format("isYicesInstalled() evaluated to %b\n", success));

	return success;
    } catch (LinkageError e) {
        System.err.println(String.format("isYicesInstalled() threw %s\n", e.getMessage()));
        e.printStackTrace(System.err);
        return false;
    }
  }
}
