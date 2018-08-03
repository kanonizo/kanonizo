package org.kanonizo.algorithms.heuristics.comparators;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.kanonizo.framework.ObjectiveFunction;
import org.kanonizo.framework.objects.Pair;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.similarity.DistanceFunction;
import org.kanonizo.framework.similarity.JaccardDistance;
import org.kanonizo.util.RandomInstance;

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
    Map<Pair<TestCase>, Double> similarity = new HashMap<>();
    for (TestCase candidate : candidates) {
      copy.remove(candidate);
      for (TestCase candidate2 : copy) {
        double sim = dist.getDistance(candidate, candidate2);
        similarity.put(new Pair<>(candidate, candidate2), sim);
      }
    }
    List<TestCase> selected = new ArrayList<>();
    // pick starting test cases

    while (!shouldFinish(selected) && similarity.size() > 0) {
      // select most dissimilar test case
      double minSimilarity = similarity.values().parallelStream().mapToDouble(a->a).min()
          .getAsDouble();
      List<Pair<TestCase>> leastSimilar = similarity.entrySet().stream()
          .filter(entry -> entry.getValue() == minSimilarity).map(entry -> entry.getKey()).collect(
              Collectors.toList());
      Pair<TestCase> selectedPair;
      if (leastSimilar.size() > 1) {
        selectedPair = leastSimilar.get(RandomInstance.nextInt(leastSimilar.size()));
      } else {
        selectedPair = leastSimilar.get(0);
      }
      TestCase selectedTest =
          RandomInstance.nextBoolean() ? selectedPair.getLeft() : selectedPair.getRight();

      // add test case to list of selected
      selected.add(selectedTest);
      // remove all instances of that test case
      similarity = similarity.entrySet().stream().filter(
          entry -> !entry.getKey().contains(selectedTest)).collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
    }

    // keep selecting test cases until coverage criteria/fixed # of test cases selected
    return selected;
  }

  private boolean shouldFinish(List<TestCase> selected) {
    return (minTestCases != -1 && selected.size() > minTestCases);
  }

  @Override
  public String readableName() {
    return "similarity";
  }

}

