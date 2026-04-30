package com.sri.yices;

public class SumComponent<T> {
  private final T factor;
  private final int term;

  public SumComponent(T pFactor, int pTerm) {
    factor = pFactor;
    term = pTerm;
  }

  public T getFactor() {
    return factor;
  }

  public int getTerm() {
    return term;
  }
}
