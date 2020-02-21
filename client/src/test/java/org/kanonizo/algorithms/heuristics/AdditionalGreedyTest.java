package org.kanonizo.algorithms.heuristics;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.kanonizo.Framework;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.ClassUnderTest;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.TestCase;

public class AdditionalGreedyTest {
  private final Instrumenter inst = mock(Instrumenter.class);
  private final Framework fw = Framework.getInstance();
  private final TestCase testCase1 = mock(TestCase.class);
  private final TestCase testCase2 = mock(TestCase.class);
  private final TestCase testCase3 = mock(TestCase.class);
  private final List<TestCase> allTests = new ArrayList<>(asList(testCase1, testCase2, testCase3));
  private final ClassUnderTest parent = mock(ClassUnderTest.class);
  private final Set<Line> linesInClass = new HashSet<>();
  private AdditionalGreedyAlgorithm alg;

  @Before
  public void setup()
  {
    fw.setInstrumenter(inst);
    alg = new AdditionalGreedyAlgorithm();

    when(parent.getCUT()).then((invocation) -> AdditionalGreedyTest.class);

    for(int i = 0; i < 100; i++)
    {
      linesInClass.add(new Line(parent, i));
    }
    when(inst.getLines(any())).thenReturn(linesInClass);
  }

  @Test
  public void givenATestWithHigherCoverage_thatTestIsReturnedFirst()
  {
    when(inst.getLinesCovered(eq(testCase2))).thenReturn(linesInClass);

    alg.init(allTests);
    assertThat(alg.selectTestCase(allTests)).isEqualTo(testCase2);
    assertThat(alg.selectTestCase(allTests)).as("Expected test case 1 to be returned second").isEqualTo(testCase1);
    assertThat(alg.selectTestCase(allTests)).as("Expected test case 3 to be returned last").isEqualTo(testCase3);
  }

  @Test
  public void givenTestsWithDescendingCoverage_testsAreReturnedInCorrectOrder()
  {
    Set<Line> linesCovered1 = new HashSet<>();
    IntStream.range(0, 50).forEach(i -> linesCovered1.add(new Line(parent, i)));
    Set<Line> linesCovered2 = new HashSet<>();
    IntStream.range(50, 80).forEach(i -> linesCovered2.add(new Line(parent, i)));
    Set<Line> linesCovered3 = new HashSet<>();
    IntStream.range(80, 100).forEach(i -> linesCovered3.add(new Line(parent, i)));

    when(inst.getLinesCovered(testCase1)).thenReturn(linesCovered1);
    when(inst.getLinesCovered(testCase2)).thenReturn(linesCovered2);
    when(inst.getLinesCovered(testCase3)).thenReturn(linesCovered3);

    alg.init(allTests);
    assertThat(alg.selectTestCase(allTests)).isEqualTo(testCase1);
    assertThat(alg.selectTestCase(allTests)).isEqualTo(testCase2);
    assertThat(alg.selectTestCase(allTests)).isEqualTo(testCase3);
  }

  @Test
  public void givenTestWhereTestHasFewerLinesCoveredButMoreNewLinesCovered_testAreReturnedInCorrectOrder()
  {
    Set<Line> linesCovered1 = new HashSet<>();
    IntStream.range(0, 80).forEach(i -> linesCovered1.add(new Line(parent, i)));
    Set<Line> linesCovered2 = new HashSet<>();
    IntStream.range(50, 80).forEach(i -> linesCovered2.add(new Line(parent, i)));
    Set<Line> linesCovered3 = new HashSet<>();
    IntStream.range(80, 100).forEach(i -> linesCovered3.add(new Line(parent, i)));

    when(inst.getLinesCovered(testCase1)).thenReturn(linesCovered1);
    when(inst.getLinesCovered(testCase2)).thenReturn(linesCovered2);
    when(inst.getLinesCovered(testCase3)).thenReturn(linesCovered3);

    alg.init(allTests);
    assertThat(alg.selectTestCase(allTests)).isEqualTo(testCase1);
    fw.notifyTestCaseSelection(testCase1);
    assertThat(alg.selectTestCase(allTests)).isEqualTo(testCase3);
    fw.notifyTestCaseSelection(testCase3);
    assertThat(alg.selectTestCase(allTests)).isEqualTo(testCase2);
  }

  @Test
  public void givenTestsWithIdenticalCoverage_testsAreReturnedInOriginalOrder()
  {
    Set<Line> linesCovered1 = new HashSet<>();
    IntStream.range(0, 30).forEach(i -> linesCovered1.add(new Line(parent, i)));
    Set<Line> linesCovered2 = new HashSet<>();
    IntStream.range(30, 60).forEach(i -> linesCovered2.add(new Line(parent, i)));
    Set<Line> linesCovered3 = new HashSet<>();
    IntStream.range(60, 90).forEach(i -> linesCovered3.add(new Line(parent, i)));

    when(inst.getLinesCovered(testCase1)).thenReturn(linesCovered1);
    when(inst.getLinesCovered(testCase2)).thenReturn(linesCovered2);
    when(inst.getLinesCovered(testCase3)).thenReturn(linesCovered3);

    alg.init(allTests);
    assertThat(alg.selectTestCase(allTests)).isEqualTo(testCase1);
    assertThat(alg.selectTestCase(allTests)).isEqualTo(testCase2);
    assertThat(alg.selectTestCase(allTests)).isEqualTo(testCase3);
  }
}
