package org.kanonizo.display;

import org.kanonizo.framework.objects.TestSuite;

public class NullDisplay implements Display {

  @Override
  public void initialise() {

  }

  @Override
  public void fireTestSuiteChange(TestSuite ts) {

  }

  @Override
  public void reportProgress(double current, double max) {

  }

  @Override
  public int ask(String question) {
    return 0;
  }

  @Override
  public void notifyTaskStart(String name, boolean progress) {

  }
}
