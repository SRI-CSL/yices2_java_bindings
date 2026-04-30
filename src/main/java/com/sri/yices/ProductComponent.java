package com.sri.yices;

public class ProductComponent {
  private final int power;
  private final int term;

  public ProductComponent(int pTerm, int pPower) {
    power = pPower;
    term = pTerm;
  }

  public int getTerm() {
    return term;
  }

  public int getPower() {
    return power;
  }
}
