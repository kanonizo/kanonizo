package org.kanonizo.algorithms.metaheuristics.selection;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import java.util.List;
import org.kanonizo.util.RandomInstance;

public class RankSelection<T extends Comparable<T>> implements SelectionFunction<T> {
  @Parameter(key = "rank_bias", description = "When using the rank selection strategy, each individual is given a rank based on its fitness. This is then used to form a proportion of numbers that will be used to select it. Depending on this number, the rank will be given more or less bias for how many numbers will result in the fitter individuals", category = "TCP")
  public static double RANK_BIAS = 1.7;
  @Override
  public int getIndex(List<T> population) {
    double r = RandomInstance.nextDouble();
    double d = (RANK_BIAS
        - Math.sqrt(Math.pow(RANK_BIAS, 2) - (4.0 * r * (RANK_BIAS - 1)))) / 2.0;
    int index = (int) ((population.size() - 1) * d);
    return index;
  }

}
