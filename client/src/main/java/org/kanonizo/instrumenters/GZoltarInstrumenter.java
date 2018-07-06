package org.kanonizo.instrumenters;


import com.gzoltar.core.AgentConfigs;
import com.gzoltar.core.instr.InstrumentationLevel;
import com.gzoltar.core.model.Node;
import com.gzoltar.core.model.Transaction;
import com.gzoltar.core.runtime.Probe;
import com.gzoltar.core.runtime.ProbeGroup;
import com.gzoltar.core.spectrum.Spectrum;
import com.gzoltar.core.spectrum.SpectrumReader;
import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.kanonizo.Framework;
import org.kanonizo.Properties;
import org.kanonizo.framework.ClassStore;
import org.kanonizo.framework.TestCaseStore;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.Branch;
import org.kanonizo.framework.objects.ClassUnderTest;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.LineStore;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.junit.KanonizoTestFailure;
import org.kanonizo.junit.KanonizoTestResult;
import org.kanonizo.util.HashSetCollector;

@org.kanonizo.annotations.Instrumenter
public class GZoltarInstrumenter implements Instrumenter {

  @Parameter(key = "gz_file", description = "Gzoltar creates a seralized file containing coverage information as collected by a build system. This file must be provided in order to use the instrumenter", category = "GZoltar")
  public static File gzFile = null;

  private HashMap<TestCase, Set<Line>> linesCovered = new HashMap<>();
  private Set<Line> totalCoverage = new HashSet<>();

  @Override
  public Class<?> loadClass(String className) throws ClassNotFoundException {
    return Thread.currentThread().getContextClassLoader().loadClass(className);
  }

  @Override
  public void setTestSuite(TestSuite ts) {

  }

  private Spectrum spectrum = null;

  private Spectrum getSpectrum() {
    if (spectrum == null) {
      long startTime = System.currentTimeMillis();
      final AgentConfigs agentConfigs = new AgentConfigs();
      agentConfigs.setInstrumentationLevel(InstrumentationLevel.NONE);
      try {
        final SpectrumReader reader = new SpectrumReader(
            Framework.getInstance().getSourceFolder().getAbsolutePath(), agentConfigs,
            new FileInputStream(gzFile));
        reader.read();
        spectrum = reader.getSpectrum();
        if(Properties.PROFILE){
          System.out.println("Initial spectrum read completed in "+(System.currentTimeMillis() - startTime) + "ms");
        }
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return spectrum;
  }

  @Override
  public void collectCoverage() {
    long startTime = System.currentTimeMillis();
    Spectrum spectrum = getSpectrum();
    if(Properties.PROFILE){
      System.out.println("Spectrum loaded in "+(System.currentTimeMillis() - startTime)+"ms");
    }
    List<Transaction> transactions = spectrum
        .getTransactions(); // a transaction is in fact a test case execution

    for (Transaction transaction : transactions) {
      String tcName = transaction.getName();
      String testClassName = tcName.substring(0, tcName.indexOf("#"));
      String testMethodName = tcName.substring(tcName.indexOf("#") + 1);
      TestCase tc = TestCaseStore.with(testMethodName + "(" + testClassName + ")");
      List<Node> hits = spectrum.getHitNodes(transaction);

      for (Node hit : hits) {
        int lineNumber = hit.getLineNumber();
        String className = hit.getName().substring(0, hit.getName().indexOf("#")).replaceFirst("\\$", ".");
        ClassUnderTest cut = ClassStore.get(className);
        Line l = LineStore.with(cut, lineNumber);
        if (!linesCovered.containsKey(tc)) {
          linesCovered.put(tc, new HashSet<>());
        }
        linesCovered.get(tc).add(l);
        totalCoverage.add(l);
      }

      List<KanonizoTestFailure> failures = new ArrayList<KanonizoTestFailure>();
      if (transaction.hasFailed()) {
        failures.add(new KanonizoTestFailure(new Exception(), transaction.getStackTrace()));
      }

      tc.setResult(
            new KanonizoTestResult(tc.getTestClass(), tc.getMethod(), transaction.hasFailed(),
                failures, transaction.getRuntime()));
    }
    if(Properties.PROFILE){
      System.out.println("Coverage deserialised in "+(System.currentTimeMillis() - startTime)+"ms");
    }
  }

  @Override
  public Set<Line> getLinesCovered(TestCase testCase) {
    if (!linesCovered.containsKey(testCase)) {
      return Collections.emptySet();
    }
    return linesCovered.get(testCase);
  }

  @Override
  public Set<Branch> getBranchesCovered(TestCase testCase) {
    return Collections.emptySet();
  }

  @Override
  public int getTotalLines(ClassUnderTest cut) {
    return getLines(cut).size();
  }

  @Override
  public int getTotalBranches(ClassUnderTest cut) {
    return 0;
  }

  @Override
  public Set<Line> getLines(ClassUnderTest cut) {
    Spectrum spectrum = getSpectrum();
    List<ProbeGroup> probeGroups = new ArrayList<>(spectrum.getProbeGroups());
    Optional<ProbeGroup> optGroup = probeGroups.stream()
        .filter(pg -> pg.getName().equals(cut.getCUT().getName())).findFirst();
    if (optGroup.isPresent()) {
      ProbeGroup group = optGroup.get();
      List<Probe> probes = group.getProbes();
      Set<Integer> lineNumbers = probes.stream().map(p -> p.getNode().getLineNumber())
          .collect(Collectors
              .toSet());
      return lineNumbers.stream().map(l -> LineStore.with(cut, l)).collect(Collectors.toSet());
    }
    return Collections.emptySet();
  }

  @Override
  public Set<Branch> getBranches(ClassUnderTest cut) {
    return Collections.emptySet();
  }

  @Override
  public int getTotalLines(SystemUnderTest sut) {
    return sut.getClassesUnderTest().stream().mapToInt(cut -> getTotalLines(cut)).sum();
  }

  @Override
  public int getLinesCovered(TestSuite testSuite) {
    return testSuite.getTestCases().stream().mapToInt(tc -> getLinesCovered(tc).size()).sum();
  }

  @Override
  public int getTotalBranches(SystemUnderTest sut) {
    return 0;
  }

  @Override
  public int getBranchesCovered(TestSuite testSuite) {
    return 0;
  }

  @Override
  public Set<Line> getLinesCovered(ClassUnderTest cut) {
    return totalCoverage.stream().filter(l -> l.getParent().equals(cut))
        .collect(Collectors.toSet());
  }

  @Override
  public Set<Line> getLinesCovered(SystemUnderTest sut) {
    return sut.getClassesUnderTest().stream().map(cut -> getLinesCovered(cut))
        .collect(new HashSetCollector<>());
  }

  @Override
  public Set<Branch> getBranchesCovered(ClassUnderTest cut) {
    return Collections.emptySet();
  }

  @Override
  public Set<Branch> getBranchesCovered(SystemUnderTest sut) {
    return Collections.emptySet();
  }

  @Override
  public ClassLoader getClassLoader() {
    return Thread.currentThread().getContextClassLoader();
  }

  @Override
  public String readableName() {
    return "gzoltar";
  }
}
