package org.kanonizo.mutation;

import java.util.ArrayList;
import java.util.List;

import org.kanonizo.framework.objects.TestCase;

public class Mutant {
  private int mutantId;
  private Class<?> targetClass;
  private int lineNumber;
  private List<TestCase> killedBy = new ArrayList<TestCase>();

  public Mutant(int mutantId, Class<?> targetClass, int lineNumber) {
    this.mutantId = mutantId;
    this.targetClass = targetClass;
    this.lineNumber = lineNumber;
  }

  public void addKillingTest(TestCase tc) {
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

  public List<TestCase> getKillingTests() {
    return new ArrayList<TestCase>(killedBy);
  }
}
