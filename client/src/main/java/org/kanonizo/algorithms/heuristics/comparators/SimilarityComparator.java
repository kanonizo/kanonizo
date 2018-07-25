package org.kanonizo.algorithms.heuristics.comparators;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.kanonizo.Framework;
import org.kanonizo.framework.ObjectiveFunction;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.similarity.DistanceFunction;
import org.kanonizo.framework.similarity.JaccardDistance;

public class SimilarityComparator implements ObjectiveFunction {

  @Parameter(key = "similarity_min_test_cases", description = "When comparing siilarity of test cases, use this parameter to select a minimum number of test cases that must be chosen regardless of similarity", category = "similarity")
  public static int minTestCases = -1;
  @Parameter(key = "similarity_coverage_adequacy", description = "When comparing similarity of test cases, use this parameter to specify a minimum percetnage of the target class that must be covered", category = "similarity")
  public static int coverageAdequacy = -1;
  @Parameter(key = "similarity_distance_function", description = "Distance function to use when comparing test cases", category = "similarity")
  public static DistanceFunction<TestCase> dist = new JaccardDistance();

  @Override
  public List<TestCase> sort(List<TestCase> candidates) {
    // calculate similarity matrix
    List<TestCase> copy = new ArrayList<>(candidates);
    Map<Pair<TestCase, TestCase>, Double> similarity = new HashMap<>();
    for (TestCase candidate : candidates) {
      copy.remove(candidate);
      for (TestCase candidate2 : copy) {
        double sim = dist.getDistance(candidate, candidate2);
        similarity.put(new ImmutablePair(candidate, candidate2), sim);
      }
    }
    // pick starting test cases
    // remove similar test cases
    // keep selecting test cases until coverage criteria/fixed # of test cases selected
    return null;
  }

  @Override
  public String readableName() {
    return "similarity";
  }

}

