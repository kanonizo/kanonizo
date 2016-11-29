package com.dpaterson.testing.mutation;

import java.util.ArrayList;
import java.util.List;

import com.dpaterson.testing.framework.TestCaseChromosome;

public class Mutant {
  private int mutantId;
  private Class<?> targetClass;
  private int lineNumber;
  private List<TestCaseChromosome> killedBy = new ArrayList<TestCaseChromosome>();

  public Mutant(int mutantId, Class<?> targetClass, int lineNumber) {
    this.mutantId = mutantId;
    this.targetClass = targetClass;
    this.lineNumber = lineNumber;
  }

  public void addKillingTest(TestCaseChromosome tc) {
    killedBy.add(tc);
  }

  public int getMutantId() {
    return mutantId;
  }

  public Class<?> getTargetClass() {
    return targetClass;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public List<TestCaseChromosome> getKillingTests() {
    return new ArrayList<TestCaseChromosome>(killedBy);
  }
}
