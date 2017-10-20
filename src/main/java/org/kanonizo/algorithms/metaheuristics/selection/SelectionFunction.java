package org.kanonizo.algorithms.metaheuristics.selection;

import java.util.ArrayList;
import java.util.List;

public interface SelectionFunction<T> {
  default List<T> select(List<T> population, int numParents) {
    if (population.size() < numParents) {
      throw new IllegalArgumentException("Can't select an individual when there is none to select from");
    }
    List<T> selected = new ArrayList<T>();
    for (int i = 0; i < numParents; i++) {
      selected.add(population.get(getIndex(population)));
    }
    return selected;
  }

  default T select(List<T> population) {
    return select(population, 1).get(0);
  }

  public int getIndex(List<T> population);
}
