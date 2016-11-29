package com.dpaterson.testing.framework;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dpaterson.testing.Disposable;
import com.dpaterson.testing.Properties;
import com.dpaterson.testing.algorithms.metaheuristics.fitness.APBCFunction;
import com.dpaterson.testing.algorithms.metaheuristics.fitness.APLCFunction;
import com.dpaterson.testing.algorithms.metaheuristics.fitness.FitnessFunction;
import com.dpaterson.testing.algorithms.metaheuristics.fitness.InstrumentedFitnessFunction;
import com.dpaterson.testing.util.RandomInstance;
import com.sheffield.instrumenter.analysis.ClassAnalyzer;
import com.sheffield.instrumenter.instrumentation.objectrepresentation.Branch;
import com.sheffield.instrumenter.instrumentation.objectrepresentation.Line;

public class TestSuiteChromosome extends Chromosome implements Comparable<TestSuiteChromosome>, Disposable {
  private SUTChromosome sut;
  // needs to represent the test case ordering
  private List<TestCaseChromosome> testCases = new ArrayList<>();
  private List<TestCaseChromosome> removedTestCases = new ArrayList<TestCaseChromosome>();
  private double fitness;
  private FitnessFunction<TestSuiteChromosome> func;
  private final List<TestCaseChromosome> runnableTestCases;
  private List<TestCaseChromosome> originalOrdering = new ArrayList<>();
  private static Logger logger = LogManager.getLogger(TestSuiteChromosome.class);
  private int totalLines;
  private int totalBranches;
  private int fitnessEvaluations;
  private boolean changed = false;

  public TestSuiteChromosome(SUTChromosome sut, List<TestCaseChromosome> testCases) {
    logger.info("Entering tsc constructor.");
    testCases.stream().forEachOrdered(testCase -> {
      this.testCases.add(testCase);
      this.originalOrdering.add(testCase);
    });

    runnableTestCases = new ArrayList<>();
    for (TestCaseChromosome testCase : testCases) {
      if (!Modifier.isAbstract(testCase.getTestClass().getModifiers())) {
        runnableTestCases.add(testCase);
      }
    }
    // runnableTestCases = this.testCases.stream()
    // .filter(testCase ->
    // !Modifier.isAbstract(testCase.getTestClass().getModifiers()))
    // .collect(Collectors.toList());
    // Collections.sort(runnableTestCases, new Comparator<TestCaseChromosome>()
    // {
    //
    // @Override
    // public int compare(TestCaseChromosome o1, TestCaseChromosome o2) {
    // return o1.getMethod().getName().compareTo(o2.getMethod().getName());
    // }
    //
    // });
    this.sut = sut;
  }

  private TestSuiteChromosome() {
    runnableTestCases = new ArrayList<>();
  }

  public SUTChromosome getSUT() {
    return sut;
  }

  public int getTotalLines() {
    if (totalLines == 0) {
      totalLines = sut.getClassesUnderTest().stream().mapToInt(CUTChromosome::getTotalLines).sum();
    }
    return totalLines;
  }

  public int getCoveredLines() {
    Set<Line> coveredLines = new HashSet<>();
    for (CUTChromosome cut : sut.getClassesUnderTest()) {
      for (TestCaseChromosome tc : testCases) {
        coveredLines.addAll(tc.getAllLinesCovered(cut));
      }
    }
    return coveredLines.size();
  }

  public int getTotalBranches() {
    if (totalBranches == 0) {
      totalBranches = sut.getClassesUnderTest().stream().mapToInt(CUTChromosome::getTotalBranches).sum();
    }
    return totalBranches;
  }

  public int getCoveredBranches() {
    Set<Branch> fullyCovered = new HashSet<>();
    Map<Branch, Boolean> partiallyCovered = new HashMap<>();
    for (CUTChromosome cut : sut.getClassesUnderTest()) {
      for (TestCaseChromosome tc : testCases) {
        for (Branch b : tc.getAllBranchesFullyCovered(cut)) {
          fullyCovered.add(b);
          if (partiallyCovered.containsKey(b)) {
            partiallyCovered.remove(b);
          }
        }
        for (Branch b : tc.getAllBranchesPartiallyCovered(cut)) {
          if (!fullyCovered.contains(b)) {
            if (partiallyCovered.containsKey(b)) {
              if (partiallyCovered.get(b) && b.getFalseHits() > 0) {
                partiallyCovered.remove(b);
                fullyCovered.add(b);
              } else if (!partiallyCovered.get(b) && b.getTrueHits() > 0) {
                partiallyCovered.remove(b);
                fullyCovered.add(b);
              }
            } else {
              partiallyCovered.put(b, b.getTrueHits() > 0);
            }
          }
        }
      }
    }
    return (2 * fullyCovered.size()) + partiallyCovered.size();
  }

  public double getLineCoverage(TestCaseChromosome tcc) {
    int linesCovered = tcc.getLineNumbersCovered().values().stream().mapToInt(List::size).sum();
    return ((double) linesCovered) / getTotalLines();
  }

  public double getBranchCoverage(TestCaseChromosome tcc) {
    int branchesCovered = 2 * tcc.getAllBranchesFullyCovered().values().stream().mapToInt(List::size).sum()
        + tcc.getAllBranchesPartiallyCovered().values().stream().mapToInt(List::size).sum();
    return ((double) branchesCovered) / getTotalBranches();
  }

  @Override
  public int size() {
    return runnableTestCases.size();
  }

  public List<TestCaseChromosome> getRunnableTestCases() {
    return Collections.unmodifiableList(runnableTestCases);
  }

  public List<TestCaseChromosome> getTestCases() {
    return new ArrayList<>(testCases);
  }

  public List<Integer> getIds() {
    return testCases.stream().map(TestCaseChromosome::getId).collect(Collectors.toList());
  }

  protected void setChanged(boolean changed) {
    this.changed = changed;
  }

  /**
   * This method will overwrite the current ordering of the test cases in the class and as such should only be used from within the algorithm to define a new order of test cases. Usage of this method
   * can cause erroneous behaviour
   */
  public void setTestCases(List<TestCaseChromosome> testCases) {
    this.testCases.clear();
    testCases.forEach(testCase -> this.testCases.add(testCase));
    evaluateFitness();
  }

  @Override
  public TestSuiteChromosome mutate() {
    long startTime = System.currentTimeMillis();
    TestSuiteChromosome clone = this.clone();
    if (RandomInstance.nextDouble() < Properties.REMOVAL_CHANCE) {
      clone.removeTestCase();
    }
    if (RandomInstance.nextDouble() < Properties.INSERTION_CHANCE) {
      clone.insertTestCase();
    }
    if (RandomInstance.nextDouble() < Properties.REORDER_CHANCE) {
      clone.reorderTestCase();
    }
    clone.setChanged(true);
    if (Properties.PROFILE) {
      ClassAnalyzer.out.println("Mutation completed in: " + (System.currentTimeMillis() - startTime) + "ms");
    }
    return clone;
  }

  private void removeTestCase() {
    int numCases = testCases.size();
    // cases are removed with probability 1/n where n is the number of
    // cases.
    List<TestCaseChromosome> removedCases = testCases.stream().filter(tc -> RandomInstance.nextDouble() < 1d / numCases)
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
      List<TestCaseChromosome> addedCases = removedTestCases.stream()
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
      TestCaseChromosome tc1 = testCases.get(point1);
      TestCaseChromosome tc2 = testCases.get(point2);
      testCases.remove(tc1);
      testCases.remove(tc2);
      testCases.add(point1, tc2);
      testCases.add(point2, tc1);
    }

  }

  @Override
  public void crossover(Chromosome chr, int point1, int point2) {
    long startTime = System.currentTimeMillis();
    // crossover ordering according to Antonial et al
    if (!(chr instanceof TestSuiteChromosome)) {
      throw new IllegalArgumentException(
          "Chromosome for crossover must be of type TestSuiteChromosome: actual type " + chr.getClass().getName());
    }
    TestSuiteChromosome other = (TestSuiteChromosome) chr;
    while (testCases.size() > point1) {
      testCases.remove(point1);
    }
    for (TestCaseChromosome tcc : other.testCases) {
      if (!testCases.contains(tcc)) {
        testCases.add(tcc);
      }
    }
    setChanged(true);
    if (Properties.PROFILE) {
      ClassAnalyzer.out.println("Crossover completed in: " + (System.currentTimeMillis() - startTime) + "ms");
    }
  }

  @Override
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
    long startTime = System.currentTimeMillis();
    fitness = getFitnessFunction().evaluateFitness();
    fitnessEvaluations++;
    setChanged(false);
    if (Properties.PROFILE) {
      System.out.println("Fitness evaluation completed in: " + (System.currentTimeMillis() - startTime) + "ms");
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
  public FitnessFunction<TestSuiteChromosome> getFitnessFunction() {
    if (func == null) {
      switch (Properties.COVERAGE_APPROACH) {
      case LINE:
        func = new APLCFunction(this);
        break;
      case BRANCH:
        func = new APBCFunction(this);
        break;
      default:
        func = new APLCFunction(this);
      }
    }
    return func;
  }

  public void setFitnessFunction(FitnessFunction<TestSuiteChromosome> func) {
    this.func = func;
    if (func instanceof InstrumentedFitnessFunction) {
      ((InstrumentedFitnessFunction) func).instrument(this);
    }
    evaluateFitness();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("\n-------------------------------------------\nMAXIMUM FITNESS: " + String.format("%.4f", getFitness())
        + "\n-------------------------------------------\n");
    Map<CUTChromosome, Set<Line>> branchesCovered = new HashMap<>();
    testCases.stream().forEach(tc -> {
      Map<CUTChromosome, List<Line>> branches = tc.getLineNumbersCovered();
      branches.entrySet().stream().forEach(entry -> {
        CUTChromosome cut = entry.getKey();
        if (branchesCovered.containsKey(cut)) {
          branchesCovered.get(cut).addAll(entry.getValue());
        } else {
          branchesCovered.put(cut, new HashSet<Line>(entry.getValue()));
        }
      });
    });
    int coveredBranches = branchesCovered.entrySet().stream().mapToInt(entry -> entry.getValue().size()).sum();
    int totalBranches = sut.getClassesUnderTest().stream().mapToInt(CUTChromosome::getTotalLines).sum();
    sb.append("Line Coverage: " + (double) coveredBranches / (double) totalBranches);
    sb.append("\n-------------------------------------------\nMaximum fitness found by "
        + getFitnessFunction().getClass().getSimpleName() + "\n-------------------------------------------\n");
    // IntStream.range(0, testCases.size()).forEach(index ->
    // appendTestCaseData(sb, ++index));
    return sb.toString();
  }

  // private void appendTestCaseData(StringBuilder sb, int index) {
  // TestCaseChromosome testCase = testCases.get(index - 1);
  // sb.append("\n" + testCase.getTestClass().getName() + "." +
  // testCase.getMethod().getName()
  // + "\n-------------------------------------------\n");
  // if (testCase != null) {
  // OptionalDouble bc =
  // testCase.getBranchesCovered().entrySet().parallelStream()
  // .mapToDouble(Map.Entry::getValue).average();
  // OptionalDouble lc =
  // testCase.getLinesCovered().entrySet().parallelStream().mapToDouble(Map.Entry::getValue)
  // .average();
  // OptionalDouble mc =
  // testCase.getMethodsCovered().entrySet().parallelStream()
  // .mapToDouble(Map.Entry::getValue).average();
  // sb.append("Branch Coverage: " + String.format("%.2f", bc.isPresent() ?
  // bc.getAsDouble() * 100 : 0) + "%\n");
  // sb.append("Line Coverage: " + String.format("%.2f", lc.isPresent() ?
  // lc.getAsDouble() * 100 : 0) + "%\n");
  // sb.append("Method Coverage: " + String.format("%.2f", mc.isPresent() ?
  // mc.getAsDouble() * 100 : 0) + "%\n");
  // }
  // sb.append("-------------------------------------------\n");
  // }

  @Override
  public TestSuiteChromosome clone() {
    TestSuiteChromosome clone = new TestSuiteChromosome();
    clone.sut = sut.clone();
    clone.testCases = new ArrayList<>(testCases);
    clone.removedTestCases = new ArrayList<>(removedTestCases);
    clone.originalOrdering = new ArrayList<>(originalOrdering);
    clone.runnableTestCases.addAll(runnableTestCases);
    clone.fitness = fitness;
    clone.func = func.clone(clone);
    return clone;
  }

  /**
   * Returns the fitter {@link TestSuiteChromosome} instance, which will either be <code>this</code> or <code>other</code>. The reason for this method being located here instead of somewhere else is
   * that the {@link FitnessFunction} object for this class isn't known by any other class, and may be a minimisation function (i.e. aiming for a 0 fitness) or a maximisation function (i.e. aiming for
   * highest possible fitness). This method determines the fitter individual according to the current {@link #func}
   *
   * @param other
   *          - the {@link TestSuiteChromosome} object to compare to <code>this</code>
   * @return the fitter individual according to the current fitness function. Always <code>this</code> or <code>other</code>, never null or a new object. This allows for object comparison if required
   */
  public TestSuiteChromosome fitter(TestSuiteChromosome other) {
    if (func.isMaximisationFunction()) {
      return fitness > other.fitness ? this : other;
    } else {
      return fitness < other.fitness ? this : other;
    }
  }

  @Override
  public int compareTo(TestSuiteChromosome o) {
    if (fitness == o.fitness) {
      return 0;
    }
    if (fitter(o).equals(this)) {
      return -1;
    } else {
      return 1;
    }
  }

  public List<TestCaseChromosome> getOriginalOrdering() {
    return Collections.unmodifiableList(originalOrdering);
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
      sut = null;
      testCases = null;
      runnableTestCases.clear();
      originalOrdering = null;
    }
  }

}
