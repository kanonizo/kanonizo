package org.kanonizo.algorithms.heuristics.historybased;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.kanonizo.framework.objects.TestCase;

public class ChoTest {

  private final Cho cho = mock(Cho.class);
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

    when(cho.getPriority(any())).thenCallRealMethod();

    doCallRealMethod().when(cho).init(allTests);
    doCallRealMethod().when(cho).selectTestCase(allTests);
    doNothing().when(cho).readHistoryFile();

  }

  @Test
  public void givenATestWithMoreFailuresThanEverBefore_thatTestIsReturnedFirst()
  {
    when(cho.getResults(testCase2)).thenReturn(asList(true, true, false, true, true, false, false));
    cho.init(allTests);

    TestCase next = cho.selectTestCase(allTests);
    assertEquals(next, testCase2);
  }


  @Test
  public void givenATestWithMoreFailuresThanAverageBefore_thatTestIsReturnedFirst()
  {
    when(cho.getResults(testCase2)).thenReturn(new ArrayList<>(asList(false, false, true, false, true, true, true)));
    cho.init(allTests);

    TestCase next = cho.selectTestCase(allTests);
    assertEquals(next, testCase2);
  }

  @Test
  public void givenATestWithFewerFailuresThanAverageBefore_andOtherTestWithMoreFailuresThanEverBefore_firstTestIsReturned()
  {
    when(cho.getResults(testCase1)).thenReturn(new ArrayList<>(asList(false, false, false, true, false, true, true, true, false, true, true, false, true, true, true)));
    when(cho.getResults(testCase2)).thenReturn(new ArrayList<>(asList(false, false, true, false, false, true, true, true, false, true, true, false, true, true, true)));
    when(cho.getResults(testCase3)).thenReturn(new ArrayList<>(asList(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true)));
    cho.init(allTests);

    TestCase next = cho.selectTestCase(allTests);
    assertThat(next.getId()).isEqualTo(1);
    assertThat(next).isEqualTo(testCase1);

    next = cho.selectTestCase(allTests);
    assertThat(next.getId()).isEqualTo(2);
    assertThat(next).isEqualTo(testCase2);
  }

}