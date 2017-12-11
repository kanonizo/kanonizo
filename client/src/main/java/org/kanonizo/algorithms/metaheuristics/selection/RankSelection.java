package org.kanonizo.algorithms.metaheuristics.selection;

import java.util.List;

import org.kanonizo.Properties;
import org.kanonizo.util.RandomInstance;

public class RankSelection<T extends Comparable<T>> implements SelectionFunction<T> {

  @Override
  public int getIndex(List<T> population) {
    double r = RandomInstance.nextDouble();
    double d = (Properties.RANK_BIAS
        - Math.sqrt(Math.pow(Properties.RANK_BIAS, 2) - (4.0 * r * (Properties.RANK_BIAS - 1)))) / 2.0;
    int index = (int) ((population.size() - 1) * d);
    return index;
  }

}
