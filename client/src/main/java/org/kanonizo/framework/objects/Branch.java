package org.kanonizo.framework.objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Branch extends Goal implements Comparable<Branch> {
  private List<TestCase> coveringTestCases = new ArrayList<>();
  private int branchNumber;

  public Branch(ClassUnderTest parent, int lineNumber, int branchNumber) {
    super(parent, lineNumber);
    this.branchNumber = branchNumber;
  }

  public int getBranchNumber() { return branchNumber; }

  public ClassUnderTest getParent() {
    return parent;
  }

  public void addCoveringTest(TestCase testCase) {
    coveringTestCases.add(testCase);
  }

  public List<TestCase> getCoveringTests() {
    return Collections.unmodifiableList(coveringTestCases);
  }

  @Override
  public int compareTo(Branch branch) {
    return (Double.compare(Double.parseDouble(lineNumber + "." + branchNumber), Double.parseDouble(branch.lineNumber + "." + branch.branchNumber)));
  }

  public int hashCode() {
    int prime = 31;
    int result = prime;
    result *= ((Integer) lineNumber).hashCode();
    result *= ((Integer) branchNumber).hashCode();
    result *= parent.getCUT().getName().hashCode();
    return result;
  }

  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null) {
      return false;
    }
    if (getClass() != other.getClass()) {
      return false;
    }
    return this.parent.getCUT().equals(((Branch) other).getParent().getCUT()) && this.lineNumber == ((Branch) other).lineNumber && this.branchNumber == ((Branch) other).branchNumber;
  }
}
