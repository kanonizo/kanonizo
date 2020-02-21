package org.kanonizo.algorithms.heuristics;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.kanonizo.Framework;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.TestCase;

public class GreedyTest {

  private final Instrumenter inst = mock(Instrumenter.class);
  private final Framework fw = Framework.getInstance();
  private final TestCase testCase1 = mock(TestCase.class);
  private final TestCase testCase2 = mock(TestCase.class);
  private final TestCase testCase3 = mock(TestCase.class);
  private final List<TestCase> allTests = new ArrayList<>(asList(testCase1, testCase2, testCase3));
  private final GreedyAlgorithm alg = new GreedyAlgorithm();

  @Before
  public void setup()
  {
    fw.setInstrumenter(inst);
  }

  @Test
  public void givenATestWithHigherCoverage_thatTestIsReturnedFirst()
  {
    Set linesCovered = mock(Set.class);
    when(linesCovered.size()).thenReturn(100);

    when(inst.getLinesCovered((TestCase) any())).thenReturn(emptySet());
    when(inst.getLinesCovered(eq(testCase2))).thenReturn(linesCovered);

    alg.init(allTests);
    assertThat(alg.selectTestCase(allTests)).isEqualTo(testCase2);
    assertThat(alg.selectTestCase(allTests)).as("Expected test case 1 to be returned second").isEqualTo(testCase1);
    assertThat(alg.selectTestCase(allTests)).as("Expected test case 3 to be returned last").isEqualTo(testCase3);
  }

  @Test
  public void givenTestsWithDescendingCoverage_testsAreReturnedInCorrectOrder()
  {
    Set linesCovered1 = mock(Set.class);
    when(linesCovered1.size()).thenReturn(100);
    Set linesCovered2 = mock(Set.class);
    when(linesCovered2.size()).thenReturn(70);
    Set linesCovered3 = mock(Set.class);
    when(linesCovered3.size()).thenReturn(50);

    when(inst.getLinesCovered(testCase1)).thenReturn(linesCovered1);
    when(inst.getLinesCovered(testCase2)).thenReturn(linesCovered2);
    when(inst.getLinesCovered(testCase3)).thenReturn(linesCovered3);

    alg.init(allTests);
    assertThat(alg.selectTestCase(allTests)).isEqualTo(testCase1);
    assertThat(alg.selectTestCase(allTests)).isEqualTo(testCase2);
    assertThat(alg.selectTestCase(allTests)).isEqualTo(testCase3);
  }

  @Test
  public void givenTestsWithIdenticalCoverage_testsAreReturnedInOriginalOrder()
  {
    Set linesCovered1 = mock(Set.class);
    when(linesCovered1.size()).thenReturn(100);
    Set linesCovered2 = mock(Set.class);
    when(linesCovered2.size()).thenReturn(100);
    Set linesCovered3 = mock(Set.class);
    when(linesCovered3.size()).thenReturn(100);

    when(inst.getLinesCovered(testCase1)).thenReturn(linesCovered1);
    when(inst.getLinesCovered(testCase2)).thenReturn(linesCovered2);
    when(inst.getLinesCovered(testCase3)).thenReturn(linesCovered3);

    alg.init(allTests);
    assertThat(alg.selectTestCase(allTests)).isEqualTo(testCase1);
    assertThat(alg.selectTestCase(allTests)).isEqualTo(testCase2);
    assertThat(alg.selectTestCase(allTests)).isEqualTo(testCase3);
  }
}
