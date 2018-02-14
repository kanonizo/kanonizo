package org.kanonizo.framework;

import com.scythe.instrumenter.analysis.ClassAnalyzer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.kanonizo.framework.instrumentation.Instrumented;
import org.kanonizo.util.Util;

public class CUTChromosome extends Chromosome implements Instrumented {
  private Class<?> cut;
  private int totalLines;
  private List<Integer> coverableLines = new ArrayList<>();
  private List<Integer> coverableBranches = new ArrayList<>();

  public CUTChromosome(Class<?> cut) {
    this.cut = cut;
    CUTChromosomeStore.add(cut.getName(), this);
    instrumentationFinished();
  }

  public Class<?> getCUT() {
    return cut;
  }

  public void setCUT(Class<?> cut) {
    this.cut = cut;
  }

  public int getTotalLines() {
    return totalLines;
  }

  @Override
  public void instrumentationFinished() {
    coverableLines = ClassAnalyzer.getCoverableLines(cut.getName()).stream().map(line -> line.getLineNumber()).collect(Collectors.toList());
    coverableBranches = ClassAnalyzer.getCoverableBranches(cut.getName()).stream().map(branch -> branch.getGoalId()).collect(Collectors.toList());
    totalLines = coverableLines.size();
  }

  public List<Integer> getCoverableLines() {
    return new LinkedList<>(coverableLines);
  }

  public List<Integer> getCoverableBranches() {
    return new ArrayList<>(coverableBranches);
  }

  public int getTotalBranches() {
    return 2 * coverableBranches.size();
  }

  @Override
  public CUTChromosome mutate() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void crossover(Chromosome chr, int point1, int point2) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CUTChromosome clone() {
    return CUTChromosomeStore.get(cut.getName());
  }

  @Override
  public String toString() {
    return Util.getName(cut);
  }

  @Override
  public int size() {
    return totalLines;
  }
}