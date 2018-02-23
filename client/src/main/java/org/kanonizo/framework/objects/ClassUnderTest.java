package org.kanonizo.framework.objects;

import java.util.Set;
import org.kanonizo.Framework;
import org.kanonizo.framework.ClassStore;
import org.kanonizo.util.Util;

public class ClassUnderTest {
  private Class<?> cut;
  private int id;
  private static int count = 0;
  private SystemUnderTest parent;
  private Set<Line> lines;
  private Set<Branch> branches;

  public ClassUnderTest(Class<?> cut) {
    this.cut = cut;
    this.id = ++count;
    this.lines = Framework.getInstrumenter().getLines(this);
    this.branches = Framework.getInstrumenter().getBranches(this);
    ClassStore.add(cut.getName(), this);
  }

  private Line findLine(int lineNumber){
    return lines.stream().filter(line -> line.getLineNumber() == lineNumber).findFirst().get();
  }

  public void setParent(SystemUnderTest parent){
    this.parent = parent;
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

  public Set<Line> getLines(){
    return lines;
  }

  public Set<Branch> getBranches() { return branches; }

  public ClassUnderTest clone() {
    return ClassStore.get(cut.getName());
  }

  @Override
  public String toString() {
    return Util.getName(cut);
  }

  public int size() {
    return Framework.getInstrumenter().getTotalLines(this);
  }

  public boolean equals(Object other){
    if(this == other){
      return true;
    }
    if (other == null){
      return false;
    }
    if (getClass() != other.getClass()){
      return false;
    }
    ClassUnderTest oc = (ClassUnderTest) other;
    return oc.cut == cut && oc.id == id;
  }

  public int hashCode(){
    int prime = 37;
    int result = prime * ((Integer)id).hashCode();
    result *= toString().hashCode();
    return result;
  }
}