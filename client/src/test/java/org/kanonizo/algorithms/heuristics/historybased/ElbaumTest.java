package org.kanonizo.algorithms.heuristics.historybased;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.kanonizo.algorithms.heuristics.historybased.Elbaum.newTestCaseLimit;
import static org.kanonizo.algorithms.heuristics.historybased.Elbaum.timeLimit;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.kanonizo.framework.objects.TestCase;

public class ElbaumTest {

  private final Elbaum elbaum = mock(Elbaum.class);
  private final TestCase testCase1 = mock(TestCase.class);
  private final TestCase testCase2 = mock(TestCase.class);
  private final TestCase testCase3 = mock(TestCase.class);
  private final List<TestCase> allTests = asList(testCase1, testCase2, testCase3);

  @Before
  public void setup() throws IOException {
    File tmpFile = Files.createTempFile("tmp", "history-file").toFile();
    tmpFile.deleteOnExit();

    HistoryBased.HISTORY_FILE = new File(tmpFile.getAbsolutePath());

    when(elbaum.getTimeSinceLastFailure(testCase1)).thenReturn(timeLimit + 1);
    when(elbaum.getTimeSinceLastFailure(testCase2)).thenReturn(timeLimit + 1);
    when(elbaum.getTimeSinceLastFailure(testCase3)).thenReturn(timeLimit + 1);

    when(elbaum.getNumExecutions(testCase1)).thenReturn(newTestCaseLimit + 1);
    when(elbaum.getNumExecutions(testCase2)).thenReturn(newTestCaseLimit + 1);
    when(elbaum.getNumExecutions(testCase3)).thenReturn(newTestCaseLimit + 1);

    when(testCase1.getId()).thenReturn(1);
    when(testCase2.getId()).thenReturn(2);
    when(testCase3.getId()).thenReturn(3);

    doCallRealMethod().when(elbaum).init(allTests);
    doCallRealMethod().when(elbaum).selectTestCase(allTests);
    doNothing().when(elbaum).readHistoryFile();

  }

  @Test
  public void givenATestWithARecentFailure_thenThatTestIsReturnedFirst()
  {
    when(elbaum.getTimeSinceLastFailure(testCase2)).thenReturn(2);
    elbaum.init(allTests);

    TestCase next = elbaum.selectTestCase(allTests);
    assertThat(testCase2).isEqualTo(next);
  }

  @Test
  public void givenATestWithALowNumberOfExecutions_thenThatTestIsReturnedFirst()
  {
    when(elbaum.getNumExecutions(testCase1)).thenReturn(2);
    elbaum.init(allTests);

    TestCase next = elbaum.selectTestCase(allTests);
    assertThat(testCase1).isEqualTo(next);
  }


}