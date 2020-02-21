package org.kanonizo.algorithms.heuristics.comparators;

import java.beans.PropertyEditor;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.kanonizo.Framework;
import org.kanonizo.framework.ObjectiveFunction;
import org.kanonizo.framework.Readable;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.listeners.TestCaseSelectionListener;

public class AdditionalGreedyComparator implements Comparator<TestCase>, TestCaseSelectionListener,
    ObjectiveFunction {

  private Set<Line> cache;
  private Instrumenter inst;
  private Framework fw = Framework.getInstance();

  public AdditionalGreedyComparator() {
    fw.addSelectionListener(this);
    fw.addPropertyChangeListener(Framework.INSTRUMENTER_PROPERTY_NAME, (e) -> {
      inst = (Instrumenter) e.getNewValue();
    });
    inst = fw.getInstrumenter();
    cache = new HashSet<>();
  }

  @Override
  public int compare(TestCase o1, TestCase o2) {
    // which test case covers more lines that have not already been covered?
    long linesCovered1 = inst.getLinesCovered(o1).stream().filter(l -> !cache.contains(l)).count();
    long linesCovered2 = inst.getLinesCovered(o2).stream().filter(l -> !cache.contains(l)).count();
    return -Long.compare(linesCovered1, linesCovered2);
  }

  @Override
  public void testCaseSelected(TestCase tc) {
    cache.addAll(inst.getLinesCovered(tc));
  }

  @Override
  public String readableName() {
    return "additional";
  }

  @Override
  public List<TestCase> sort(List<TestCase> candidates) {
    candidates.sort(this);
    return candidates;
  }
}
