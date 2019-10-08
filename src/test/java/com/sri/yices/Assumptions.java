package com.sri.yices;

/**
 * @author Huascar Sanchez
 */
public class Assumptions {
  public static final boolean IS_YICES_INSTALLED = Assumptions.isYicesInstalled();

  private Assumptions(){}

  private static boolean isYicesInstalled() {
    try {
      return Yices.isReady();
    } catch (LinkageError e) {
      return false;
    }
  }
}
