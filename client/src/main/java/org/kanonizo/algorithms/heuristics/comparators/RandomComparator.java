package org.kanonizo.algorithms.heuristics.comparators;

import java.util.ArrayList;
import java.util.List;
import org.kanonizo.framework.ObjectiveFunction;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.util.RandomInstance;

public class RandomComparator implements ObjectiveFunction {

  @Override
  public List<TestCase> sort(List<TestCase> candidates) {
    ArrayList<TestCase> candidatesClone = new ArrayList<>(candidates);
    ArrayList<TestCase> newOrder = new ArrayList<>();
    while(!candidatesClone.isEmpty()){
      int index = RandomInstance.nextInt(candidatesClone.size());
      TestCase t = candidatesClone.get(index);
      candidatesClone.remove(t);
      newOrder.add(t);
    }
    return newOrder;
  }

  @Override
  public String readableName() {
    return "random";
  }
}
