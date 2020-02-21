package org.kanonizo.algorithms.heuristics.historybased;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import junit.framework.AssertionFailedError;
import org.junit.Before;
import org.junit.Test;
import org.kanonizo.framework.objects.TestCase;

public class HuangTest {
  private final Huang huang = mock(Huang.class);
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

    doCallRealMethod().when(huang).init(allTests);
    doCallRealMethod().when(huang).selectTestCase(allTests);
    doNothing().when(huang).readHistoryFile();
  }

  @Test
  public void givenATestThatFailsWithNPE_ThatTestIsReturnedFirst()
  {
    when(huang.getFailingTestCases(anyInt())).thenReturn(new ArrayList<>(asList(testCase1, testCase2)));
    when(huang.getCause(eq(testCase2), anyInt())).thenReturn(new NullPointerException());
    when(huang.getCause(eq(testCase1), anyInt())).thenReturn(new AssertionFailedError());

    huang.init(allTests);

    assertThat(huang.selectTestCase(allTests)).isEqualTo(testCase2);
    assertThat(huang.selectTestCase(allTests)).isEqualTo(testCase1);
  }

  @Test
  public void givenNoTestHasHigherSeverityFailure_thenTestsReturnedInOriginalOrder()
  {
    when(huang.getFailingTestCases(anyInt())).thenReturn(new ArrayList<>(asList(testCase1, testCase2, testCase3)));
    when(huang.getCause(any(), anyInt())).thenReturn(new NullPointerException());

    huang.init(allTests);

    assertThat(huang.selectTestCase(allTests)).isEqualTo(testCase1);
    assertThat(huang.selectTestCase(allTests)).isEqualTo(testCase2);
    assertThat(huang.selectTestCase(allTests)).isEqualTo(testCase3);
  }

}