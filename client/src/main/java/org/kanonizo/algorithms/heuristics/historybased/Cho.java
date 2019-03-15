package org.kanonizo.algorithms.heuristics.historybased;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import org.kanonizo.annotations.Algorithm;
import org.kanonizo.framework.objects.TestCase;

@Algorithm
public class Cho extends HistoryBased {

  @Parameter(key = "cho_alpha", description = "In the Cho et al. history-based test prioritisation algorithm, there are values for alpha, beta, gamma and delta representing how much weight to assign to recently consecutive failures for test cases. This value represents the alpha weight", category="cho")
  public static double alpha = 1;
  @Parameter(key = "cho_beta", description = "In the Cho et al. history-based test prioritisation algorithm, there are values for alpha, beta, gamma and delta representing how much weight to assign to recently consecutive failures for test cases. This value represents the beta weight", category="cho")
  public static double beta = 0.7;
  @Parameter(key = "cho_gamma", description = "In the Cho et al. history-based test prioritisation algorithm, there are values for alpha, beta, gamma and delta representing how much weight to assign to recently consecutive failures for test cases. This value represents the gamma weight", category="cho")
  public static double gamma = 0.4;
  @Parameter(key = "cho_delta", description = "In the Cho et al. history-based test prioritisation algorithm, there are values for alpha, beta, gamma and delta representing how much weight to assign to recently consecutive failures for test cases. This value represents the delta weight", category="cho")
  public static double delta = 0.1;

  @Parameter(key = "cho_omega", description = "In the Cho et al. history-based test prioritisation algorithm, previous failures that are not part of the current failure chain (assuming >= consecutive failures) are weighted by an omega value", category="cho")
  public static double omega = 0.1;

  private static final int TEST_FAILURE_WEIGHT = -1;
  private HashMap<TestCase, Double> priority = new HashMap<>();
  private List<TestCase> ordering = new ArrayList<>();

  @Override
  public void init(List<TestCase> testCases) {
    super.init(testCases);
    testCases.forEach(tc -> priority.put(tc, getPriority(tc)));
    ordering.addAll(testCases);
    Collections.sort(ordering, Comparator.comparingDouble(t -> priority.get(t)));
  }

  private double getPriority(TestCase tc){
    List<Boolean> results = getResults(tc);
    if(results.size() == 0){
      return 0;
    }

    int frMin = Integer.MAX_VALUE;
    int frMax = 0;
    double frSum = 0.0;
    int frCount = 0;
    int failures = 0;
    int consecutiveFails = results.indexOf(true);
    // only consider historic failures when calculating averages
    for(int i = consecutiveFails; i < results.size(); i++){
      boolean result = results.get(i);
      if(result){
        if(failures > 0){
          if(failures < frMin){
            frMin = failures;
          }
          if (failures > frMax){
            frMax = failures;
          }
          frSum += failures;
          frCount++;
          failures = 0;
        }
      } else {
        failures++;
      }
    }

    double frAvg = frSum / frCount;
    double priority = consecutiveFails * (TEST_FAILURE_WEIGHT * omega);
    for(int i = 0; i < consecutiveFails; i++){
      if(i < frMin){
        priority += TEST_FAILURE_WEIGHT * alpha;
      } else if (i < frAvg){
        priority += TEST_FAILURE_WEIGHT * beta;
      } else if (i < frMax){
        priority += TEST_FAILURE_WEIGHT * gamma;
      } else {
        priority += TEST_FAILURE_WEIGHT * delta;
      }
    }
    return priority;
  }

  @Override
  public TestCase selectTestCase(List<TestCase> testCases) {
    TestCase next = ordering.get(0);
    ordering.remove(next);
    return next;
  }

  @Override
  public String readableName() {
    return "cho";
  }
}
