package org.kanonizo.junit.runners;

import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.junit.KanonizoTestResult;

public interface KanonizoTestRunner {
  KanonizoTestResult runTest(TestCase tc);
}
