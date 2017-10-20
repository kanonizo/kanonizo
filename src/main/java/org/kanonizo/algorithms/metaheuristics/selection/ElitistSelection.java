package org.kanonizo.algorithms.metaheuristics.selection;

import java.util.List;

public class ElitistSelection<T extends Comparable<T>> implements SelectionFunction<T> {

  @Override
  public int getIndex(List<T> population) {
    return 0;
  }

}
