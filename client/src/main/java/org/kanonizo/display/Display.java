package org.kanonizo.display;

import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;

public interface Display {
  int RESPONSE_YES=0;
  int RESPONSE_NO=1;
  int RESPONSE_INVALID=-1;
  void initialise();
  void fireTestCaseSelected(TestCase tc);
  void fireTestSuiteChange(TestSuite ts);
  void reportProgress(double current, double max);
  int ask(String question);
}
