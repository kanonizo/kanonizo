package org.kanonizo.framework.similarity;

import java.util.Set;
import org.kanonizo.Framework;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.TestCase;

public class JaccardDistance implements DistanceFunction<TestCase> {

  private Instrumenter inst = Framework.getInstance().getInstrumenter();

  public JaccardDistance() {
    Framework.getInstance().addPropertyChangeListener(Framework.INSTRUMENTER_PROPERTY_NAME,
        evt -> inst = (Instrumenter) evt.getNewValue()
    );
  }

  @Override
  public double getDistance(TestCase first, TestCase second) {
    Set<Line> covered1 = inst.getLinesCovered(first);
    Set<Line> covered2 = inst.getLinesCovered(second);
    //size of the intersection
    double intersection = covered1.stream().mapToDouble(l -> covered2.contains(l) ? 1 : 0).sum();
    //size of the union
    double union = covered1.size() + covered2.size() - intersection;
    if (union == 0 && intersection == 0){
      // no lines covered by either test
      return 0;
    } else if (union == 0){
      // some lines covered but all intersection
      return 1;
    } else {
      return intersection / union;
    }
  }
}
