package org.kanonizo.algorithms.heuristics.historybased;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.kanonizo.framework.objects.TestCase;

public class MarijanTest {
  private final Marijan marijan = mock(Marijan.class);
  private final TestCase testCase1 = mock(TestCase.class);
  private final TestCase testCase2 = mock(TestCase.class);
  private final TestCase testCase3 = mock(TestCase.class);
  private final List<TestCase> allTests = asList(testCase1, testCase2, testCase3);

  @Before
  public void setup() throws IOException {
    File tmpFile = Files.createTempFile("tmp", "history-file").toFile();
    tmpFile.deleteOnExit();

    HistoryBased.HISTORY_FILE = new File(tmpFile.getAbsolutePath());

    when(testCase1.getId()).thenReturn(1);
    when(testCase2.getId()).thenReturn(2);
    when(testCase3.getId()).thenReturn(3);

    doCallRealMethod().when(marijan).init(allTests);
    doCallRealMethod().when(marijan).selectTestCase(allTests);
    doNothing().when(marijan).readHistoryFile();
  }

  @Test
  public void givenATestThatFailedRecently_thatTestIsReturned()
  {
    when(marijan.getResults(testCase2)).thenReturn(asList(true, false, false, true, true, true, true));
    when(marijan.getResults(testCase1)).thenReturn(asList(true, true, true, true, true, true, true));
    when(marijan.getResults(testCase3)).thenReturn(asList(true, true, true, true, true, true, true));

    marijan.init(allTests);

    assertThat(marijan.selectTestCase(allTests)).isEqualTo(testCase2);
  }

  @Test
  public void givenATestThatFailedALongTimeAgo_andOneThatFailedRecently_recentlyFailingTestCaseReturnedFirst()
  {
    when(marijan.getResults(testCase2)).thenReturn(asList(true, true, false, true, true, true, true));
    when(marijan.getResults(testCase1)).thenReturn(asList(true, true, true, true, true, false, true));
    when(marijan.getResults(testCase3)).thenReturn(asList(true, true, true, true, true, true, true));

    marijan.init(allTests);

    assertThat(marijan.selectTestCase(allTests)).isEqualTo(testCase2);
    assertThat(marijan.selectTestCase(allTests)).isEqualTo(testCase1);
  }

}