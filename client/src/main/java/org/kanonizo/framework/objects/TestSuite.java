package org.kanonizo.framework.objects;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import com.scythe.instrumenter.analysis.ClassAnalyzer;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.Disposable;
import org.kanonizo.Framework;
import org.kanonizo.Properties;
import org.kanonizo.algorithms.metaheuristics.fitness.APBCFunction;
import org.kanonizo.algorithms.metaheuristics.fitness.APLCFunction;
import org.kanonizo.algorithms.metaheuristics.fitness.FitnessFunction;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.util.RandomInstance;

public class TestSuite implements Comparable<TestSuite>, Disposable {
  //Probability of a test case being removed during mutation
  @Parameter(key = "removal_chance", description = "The probability of a test case being removed during mutation. When removing test cases, where there are n test cases, each test case is removed with probability 1/n. This means that in some cases 2 test cases may be removed, and in others none will be", category = "TCP")
  public static double REMOVAL_CHANCE = 0d;

  //Probability of a test case being inserted during mutation
  @Parameter(key = "insertion_chance", description = "The probability of a test case being inserted during mutation. This relies on tests having already been removed, as we cannot introduce new tests during TCP", category = "TCP")
  public static double INSERTION_CHANCE = 0d;

  //Probability of re-ordering some existing test cases during mutation
  @Parameter(key = "reorder_chance", description = "The probability of test cases being reordered during mutation. If this chance is passed, two tests are selected at random and will have their places switched in the test suite", category = "TCP")
  public static double REORDER_CHANCE = 1d;
  private SystemUnderTest parent;
  // needs to represent the test case ordering
  private List<TestCase> removedTestCases = new ArrayList<TestCase>();
  private double fitness;
  private List<TestCase> testCases = new ArrayList<>();
  private FitnessFunction<SystemUnderTest> func;
  private static Logger logger = LogManager.getLogger(TestSuite.class);
  private int fitnessEvaluations;
  private boolean changed = false;

  public TestSuite() {
  }

  public void setParent(SystemUnderTest parent){
    this.parent = parent;
  }

  public SystemUnderTest getParent() {
    return parent;
  }

  public int size() {
    return testCases.size();
  }

  public List<TestCase> getTestCases() {
    return new ArrayList<>(testCases);
  }

  public void clear(){
    testCases.clear();
  }

  public List<Integer> getIds() {
    return testCases.stream().map(TestCase::getId).collect(Collectors.toList());
  }

  public void addTestCase(TestCase tc){
    tc.setParent(this);
    if(Modifier.isAbstract(tc.getMethod().getModifiers())){
      logger.debug("Not adding "+tc+" because it is not runnable");
    } else {
      this.testCases.add(tc);
    }
  }

  protected void setChanged(boolean changed) {
    this.changed = changed;
  }

  /**
   * This method will overwrite the current ordering of the test cases in the class and as such should only be used from within the algorithm to define a new order of test cases. Usage of this method
   * can cause erroneous behaviour
   */
  public void setTestCases(List<TestCase> testCases) {
    this.testCases.clear();
    testCases.forEach(testCase -> this.testCases.add(testCase));
    evaluateFitness();
  }

  public TestSuite mutate() {
    long startTime = java.lang.System.currentTimeMillis();
    TestSuite clone = this.clone();
    if (RandomInstance.nextDouble() < REMOVAL_CHANCE) {
      clone.removeTestCase();
    }
    if (RandomInstance.nextDouble() < INSERTION_CHANCE) {
      clone.insertTestCase();
    }
    if (RandomInstance.nextDouble() < REORDER_CHANCE) {
      clone.reorderTestCase();
    }
    clone.setChanged(true);
    if (Properties.PROFILE) {
      ClassAnalyzer.out.println("Mutation completed in: " + (java.lang.System.currentTimeMillis() - startTime) + "ms");
    }
    return clone;
  }

  private void removeTestCase() {
    int numCases = testCases.size();
    // cases are removed with probability 1/n where n is the number of
    // cases.
    List<TestCase> removedCases = testCases.stream().filter(tc -> RandomInstance.nextDouble() < 1d / numCases)
        .collect(Collectors.toList());
    // ensure we aren't removing all cases
    if (testCases.size() - removedCases.size() > 0) {
      removedTestCases.addAll(removedCases);
      testCases.removeAll(removedCases);
    }
  }

  private void insertTestCase() {
    if (removedTestCases.size() > 0) {
      int numRemovedCases = removedTestCases.size();
      // cases are added with probability 1/n where n is the number of
      // missing cases
      List<TestCase> addedCases = removedTestCases.stream()
          .filter(tc -> RandomInstance.nextDouble() < 1d / numRemovedCases).collect(Collectors.toList());
      testCases.addAll(addedCases);
      removedTestCases.removeAll(addedCases);
    }
  }

  private void reorderTestCase() {
    List<Integer> points = new ArrayList<Integer>();
    double mutationChance = 2 * Properties.NUMBER_OF_MUTATIONS / (double) testCases.size();
    for (int i = 0; i < testCases.size(); i++) {
      if (RandomInstance.nextDouble() <= mutationChance) {
        points.add(i);
      }
    }
    for (int i = 0; i < points.size() - 1; i += 2) {
      if (!(points.size() > i + 1)) {
        break;
      }
      int point1 = points.get(i);
      int point2 = points.get(i + 1);
      TestCase tc1 = testCases.get(point1);
      TestCase tc2 = testCases.get(point2);
      testCases.remove(tc1);
      testCases.remove(tc2);
      testCases.add(point1, tc2);
      testCases.add(point2, tc1);
    }

  }

  public void crossover(TestSuite chr, int point1, int point2) {
    long startTime = java.lang.System.currentTimeMillis();
    // crossover ordering according to Antonial et al
    while (testCases.size() > point1) {
      testCases.remove(point1);
    }
    for (TestCase tcc : chr.testCases) {
      if (!testCases.contains(tcc)) {
        testCases.add(tcc);
      }
    }
    setChanged(true);
    if (Properties.PROFILE) {
      ClassAnalyzer.out.println("Crossover completed in: " + (java.lang.System.currentTimeMillis() - startTime) + "ms");
    }
  }

  public double getFitness() {
    return fitness;
  }

  public void evolutionComplete() {
    if (changed) {
      evaluateFitness();
    }
  }

  /**
   * Used to set what should be stored in this classes {@link#fitness} variable, which will be returned when {@link#getFitness()} is called. This method by default delegates a call to the
   * {@link#getFitnessFunction()} method
   */
  protected void evaluateFitness() {
    long startTime = java.lang.System.currentTimeMillis();
    fitness = getFitnessFunction().evaluateFitness();
    fitnessEvaluations++;
    setChanged(false);
    if (Properties.PROFILE) {
      java.lang.System.out.println("Fitness evaluation completed in: " + (java.lang.System.currentTimeMillis() - startTime) + "ms");
    }
  }

  public int getFitnessEvaluations() {
    return fitnessEvaluations;
  }

  /**
   * Returns a fitness function used for evaluating fitness on this object. By default this returns an APFD fitness function, but can be modified by subclasses to return a fitness function of their
   * own choosing.
   *
   * @return a fitness function, which must implement the #{FitnessFunction} interface and not return null
   */
  public FitnessFunction<SystemUnderTest> getFitnessFunction() {
    if (func == null) {
      switch (Properties.COVERAGE_APPROACH) {
        case LINE:
          func = new APLCFunction(parent);
          break;
        case BRANCH:
          func = new APBCFunction(parent);
          break;
        default:
          func = new APLCFunction(parent);
      }
    }
    return func;
  }

  public void setFitnessFunction(FitnessFunction<SystemUnderTest> func) {
    this.func = func;
    evaluateFitness();
  }

  public boolean contains(TestCase tc){
    return testCases.stream().anyMatch(tc2 -> tc2.equals(tc));
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("\n-------------------------------------------\nMAXIMUM FITNESS: " + String.format("%.4f", getFitness())
        + "\n-------------------------------------------\n");
    Set<Line> covered = new HashSet<>();
    Instrumenter inst = Framework.getInstance().getInstrumenter();
    testCases.stream().forEach(tc -> {
      Set<Line> branches = inst.getLinesCovered(tc);
      covered.addAll(branches);
    });
    int coveredBranches = covered.size();
    int totalBranches = parent.getClassesUnderTest().stream().mapToInt(cut -> inst.getTotalLines(cut)).sum();
    sb.append("Line Coverage: " + (double) coveredBranches / (double) totalBranches);
    sb.append("\n-------------------------------------------\nMaximum fitness found by "
        + getFitnessFunction().getClass().getSimpleName() + "\n-------------------------------------------\n");
    return sb.toString();
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TestSuite testSuite = (TestSuite) o;

    return testCases != null ? testCases.equals(testSuite.testCases) : testSuite.testCases == null;
  }

  @Override
  public int hashCode() {
    int result = 0;
    for (TestCase t : testCases){
      //independent of order
      result += t.hashCode();
    }
    return result;
  }

  @Override
  public TestSuite clone() {
    long startTime = System.currentTimeMillis();
    TestSuite clone = new TestSuite();
    clone.parent = parent;
    clone.testCases = new ArrayList<>(testCases);
    clone.removedTestCases = new ArrayList<>(removedTestCases);
    clone.fitness = fitness;
    clone.func = func == null ? null : func.clone(parent);
    if(Properties.PROFILE){
      System.out.println("Cloned test suite in "+(System.currentTimeMillis() - startTime)+"ms");
    }
    return clone;
  }

  /**
   * Returns the fitter {@link TestSuite} instance, which will either be <code>this</code> or <code>other</code>. The reason for this method being located here instead of somewhere else is
   * that the {@link FitnessFunction} object for this class isn't known by any other class, and may be a minimisation function (i.e. aiming for a 0 fitness) or a maximisation function (i.e. aiming for
   * highest possible fitness). This method determines the fitter individual according to the current {@link #func}
   *
   * @param other - the {@link TestSuite} object to compare to <code>this</code>
   * @return the fitter individual according to the current fitness function. Always <code>this</code> or <code>other</code>, never null or a new object. This allows for object comparison if required
   */
  public TestSuite fitter(TestSuite other) {
    if (func.isMaximisationFunction()) {
      return fitness > other.fitness ? this : other;
    } else {
      return fitness < other.fitness ? this : other;
    }
  }

  @Override
  public int compareTo(TestSuite o) {
    if (fitness == o.fitness) {
      return 0;
    }
    if (fitter(o).equals(this)) {
      return -1;
    } else {
      return 1;
    }
  }

  public boolean isDisposed() {
    return disposed;
  }

  private boolean disposed = false;

  @Override
  public void dispose() {
    if (!disposed) {
      disposed = true;
      func.dispose();
      func = null;
      parent = null;
      testCases = null;
    }
  }

}
