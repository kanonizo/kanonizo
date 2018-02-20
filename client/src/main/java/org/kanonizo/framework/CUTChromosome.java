package org.kanonizo.framework;

import org.kanonizo.Framework;
import org.kanonizo.util.Util;

public class CUTChromosome extends Chromosome {
  private Class<?> cut;
  private int id;
  private static int count = 0;

  public CUTChromosome(Class<?> cut) {
    this.cut = cut;
    this.id = ++count;
    CUTChromosomeStore.add(cut.getName(), this);
  }

  public int getId(){
    return id;
  }

  public Class<?> getCUT() {
    return cut;
  }

  public void setCUT(Class<?> cut) {
    this.cut = cut;
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
    return Framework.getInstrumenter().getTotalLines(this);
  }
}