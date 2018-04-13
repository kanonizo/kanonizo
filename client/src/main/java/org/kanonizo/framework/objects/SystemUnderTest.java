package org.kanonizo.framework.objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SystemUnderTest implements Cloneable {
  private List<ClassUnderTest> classesUnderTest = new ArrayList<ClassUnderTest>();
  private List<Class<?>> extraClasses = new ArrayList<>();
  private TestSuite suite = new TestSuite();

  public SystemUnderTest() {
    suite.setParent(this);
  }

  public List<Class<?>> getExtraClasses() {
    return extraClasses;
  }

  public void addClass(ClassUnderTest cut) {
    cut.setParent(this);
    classesUnderTest.add(cut);
  }

  public void addTestCase(TestCase testCase) {
    if(!suite.contains(testCase)) {
      suite.addTestCase(testCase);
    }
  }

  public TestSuite getTestSuite() {
    return suite;
  }

  public void addExtraClass(Class<?> extra) {
    this.extraClasses.add(extra);
  }

  public List<ClassUnderTest> getClassesUnderTest() {
    return Collections.unmodifiableList(classesUnderTest);
  }

  public int size() {
    return classesUnderTest.size();
  }

  public SystemUnderTest clone() {
    SystemUnderTest clone = new SystemUnderTest();
    for (ClassUnderTest cut : classesUnderTest) {
      clone.addClass(cut.clone());
    }
    for (Class<?> extra : extraClasses) {
      clone.extraClasses.add(extra);
    }
    clone.suite = suite.clone();
    clone.suite.setParent(clone);
    return clone;
  }

}
