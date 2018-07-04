package org.kanonizo.instrumenters;


import com.gzoltar.core.AgentConfigs;
import com.gzoltar.core.instr.InstrumentationLevel;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.kanonizo.Framework;
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

@org.kanonizo.annotations.Instrumenter
public class GZoltarInstrumenter implements Instrumenter {

  @Parameter(key = "gz_file", description = "Gzoltar creates a seralized file containing coverage information as collected by a build system. This file must be provided in order to use the instrumenter", category = "GZoltar")
  public static File gzFile = null;

  private HashMap<TestCase, Set<Line>> linesCovered = new HashMap<>();

  @Override
  public Class<?> loadClass(String className) throws ClassNotFoundException {
    return Thread.currentThread().getContextClassLoader().loadClass(className);
  }

  @Override
  public void setTestSuite(TestSuite ts) {

  }

  @Override
  public void collectCoverage() {
    final AgentConfigs agentConfigs = new AgentConfigs();
    agentConfigs.setInstrumentationLevel(InstrumentationLevel.NONE);
    try {
      final SpectrumReader reader = new SpectrumReader(Framework.getInstance().getSourceFolder().getAbsolutePath(), agentConfigs,
            new FileInputStream(gzFile));
      final Spectrum spectrum = reader.getSpectrum();
      List<ProbeGroup> probeGroups = new ArrayList<>(spectrum.getProbeGroups()); // a ProbeGroup contains all probes (i.e., lines that have been instrumented) of each class
      List<Transaction> transactions = spectrum.getTransactions(); // a transaction is in fact a test case execution

      for (Transaction transaction : transactions) {
        String tcName = transaction.getName();
        String testClassName = tcName.substring(0, tcName.indexOf("#"));
        String testMethodName = tcName.substring(tcName.indexOf("#")+1);
        TestCase tc = TestCaseStore.with(testMethodName+"("+testClassName+")");
        for (ProbeGroup probeGroup : probeGroups) {
          for (Probe probe : probeGroup.getProbes()) {
            if (transaction.isProbeActived(probeGroup, probe.getArrayIndex())) {
              String className = probeGroup.getName();
              ClassUnderTest cut = ClassStore.get(className);
              int lineNumber = probe.getNode().getLineNumber();
              Line l = LineStore.with(cut, lineNumber);
              if(!linesCovered.containsKey(tc)){
                linesCovered.put(tc, new HashSet<>());
              }
              linesCovered.get(tc).add(l);
              // transaction covered this particular probe, i.e., line
            }
          }
        }

        if (transaction.hasFailed()) {
          // transaction, i.e., test case failed
        } else {
          // transaction, i.e., test case passed
        }
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  @Override
  public Set<Line> getLinesCovered(TestCase testCase) {
    return null;
  }

  @Override
  public Set<Branch> getBranchesCovered(TestCase testCase) {
    return null;
  }

  @Override
  public int getTotalLines(ClassUnderTest cut) {
    return 0;
  }

  @Override
  public int getTotalBranches(ClassUnderTest cut) {
    return 0;
  }

  @Override
  public Set<Line> getLines(ClassUnderTest cut) {
    return null;
  }

  @Override
  public Set<Branch> getBranches(ClassUnderTest cut) {
    return null;
  }

  @Override
  public int getTotalLines(SystemUnderTest sut) {
    return 0;
  }

  @Override
  public int getLinesCovered(TestSuite testSuite) {
    return 0;
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
    return null;
  }

  @Override
  public Set<Line> getLinesCovered(SystemUnderTest sut) {
    return null;
  }

  @Override
  public Set<Branch> getBranchesCovered(ClassUnderTest cut) {
    return null;
  }

  @Override
  public Set<Branch> getBranchesCovered(SystemUnderTest sut) {
    return null;
  }

  @Override
  public ClassLoader getClassLoader() {
    return null;
  }

  @Override
  public String readableName() {
    return "gzoltar";
  }
}
