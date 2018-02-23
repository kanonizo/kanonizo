package org.kanonizo.framework.objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Line extends Goal implements Comparable<Line> {

  public Line(ClassUnderTest parent, int lineNumber) {
    super(parent, lineNumber);
    LineStore.add(this);
  }

  public ClassUnderTest getParent() {
    return parent;
  }

  @Override
  public int compareTo(Line line) {
    return ((Integer) lineNumber).compareTo(line.lineNumber);
  }

  public int hashCode() {
    int prime = 31;
    int result = prime;
    result *= ((Integer) lineNumber).hashCode();
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
    return this.parent.getCUT().equals(((Line) other).getParent().getCUT()) && this.lineNumber == ((Line) other).lineNumber;
  }
}
