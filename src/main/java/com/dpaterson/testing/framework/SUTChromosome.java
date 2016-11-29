package com.dpaterson.testing.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SUTChromosome extends Chromosome {
  private List<CUTChromosome> classesUnderTest = new ArrayList<CUTChromosome>();
  private List<Class<?>> extraClasses = new ArrayList<>();

  public SUTChromosome(List<CUTChromosome> classesUnderTest, List<Class<?>> extraClasses) {
    this.classesUnderTest.addAll(classesUnderTest);
    this.extraClasses.addAll(extraClasses);
  }

  public SUTChromosome() {

  }

  public List<Class<?>> getExtraClasses() {
    return extraClasses;
  }

  public void addCUT(CUTChromosome cut) {
    classesUnderTest.add(cut);
  }

  public List<CUTChromosome> getClassesUnderTest() {
    return Collections.unmodifiableList(classesUnderTest);
  }

  @Override
  public SUTChromosome mutate() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void crossover(Chromosome chr, int point1, int point2) {
    throw new UnsupportedOperationException();
  }

  @Override
  public SUTChromosome clone() {
    return this;
  }

  @Override
  public int size() {
    return classesUnderTest.size();
  }

}
