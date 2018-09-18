package org.kanonizo.algorithms.heuristics.historybased;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.kanonizo.annotations.Algorithm;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.util.Util;

@Algorithm
public class Marijan extends HistoryBased{
  private Map<TestCase, Double> priority = new HashMap<>();
  private List<TestCase> order = new ArrayList<>();
  @Override
  public void init(List<TestCase> testCases){
    super.init(testCases);
    createFailureMatrix(testCases);
  }

  private void createFailureMatrix(List<TestCase> testCases){
    double weight = 0.7;
    for(int i = 0; i < testCases.size(); i++){
      double p = 0;
      if(i > 2){
        weight = 0.1;
      } else if (i > 1){
        weight = 0.2;
      }
      TestCase tc = testCases.get(i);
      List<Boolean> results = getResults(tc);
      for(int j = 0; j < results.size(); j++){
        boolean res = results.get(j);
        if(res){
          p += 1 * weight;
        } else {
          p -= 1 * weight;
        }
      }
      priority.put(tc, p);
    }
    priority = Util.sortByValue(priority);
    Set<Double> values = new HashSet<>(priority.values());
    for(Double val : values){
      List<TestCase> testCases1 = priority.entrySet().stream().filter(entry -> entry.getValue().equals(val)).map(Entry::getKey).collect(Collectors.toList());
      testCases1 = testCases1.stream().filter(tc -> getRuntimes(tc).size() > 1).collect(Collectors.toList());
      if(testCases1.size() > 0) {
        long maxExecutionTime = testCases1.stream().mapToLong(tc -> getRuntimes(tc).get(0)).max()
            .getAsLong();
        for (TestCase tc : testCases1) {
          long executionTime = getRuntimes(tc).get(0);
          priority.put(tc, priority.get(tc) + executionTime / maxExecutionTime);
        }
      }
    }
    priority = Util.sortByValue(priority);
  }

  @Override
  public TestCase selectTestCase(List<TestCase> testCases) {
    TestCase tc = priority.keySet().iterator().next();
    priority.remove(tc);
    return tc;
  }

  @Override
  public String readableName() {
    return "marijan";
  }
}
