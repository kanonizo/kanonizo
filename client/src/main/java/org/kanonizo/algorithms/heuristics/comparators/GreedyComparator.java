package org.kanonizo.algorithms.heuristics.comparators;

import java.util.Comparator;
import org.kanonizo.Framework;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.TestCase;

public class GreedyComparator implements Comparator<TestCase> {

  Instrumenter inst;
  public GreedyComparator(){
    inst = Framework.getInstance().getInstrumenter();
  }
  @Override
  public int compare(TestCase o1, TestCase o2) {
    int fitness1 = inst.getLinesCovered(o1).size();
    int fitness2 = inst.getLinesCovered(o2).size();
    return Integer.compare(fitness1, fitness2);
  }
}
