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
    double sim = covered1.stream().mapToDouble(l -> covered2.contains(l) ? 1 : 0).sum();
    //size of the union
    sim /= (covered1.size() + covered2.size() - sim);
    return sim;
  }
}
