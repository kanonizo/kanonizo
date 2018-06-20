package org.kanonizo.framework.objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;

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
    if (!suite.contains(testCase)) {
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
    clone.classesUnderTest.addAll(classesUnderTest);
    clone.extraClasses.addAll(extraClasses);
    suite.getTestCases().forEach(tc -> clone.suite.addTestCase(tc));
    return clone;
  }

  @Override
  public int hashCode() {
    int result = classesUnderTest.hashCode();
    result = 31 * result + extraClasses.hashCode();
    result = 31 * result + suite.hashCode();
    return result;
  }

  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null) {
      return false;
    }
    if (other.getClass() != getClass()) {
      return false;
    }
    SystemUnderTest otherSUT = (SystemUnderTest) other;
    List<ClassUnderTest> classes = otherSUT.classesUnderTest;
    List<TestCase> testCases = otherSUT.suite.getTestCases();
    return CollectionUtils.isEqualCollection(classes, classesUnderTest) &&
        CollectionUtils.isEqualCollection(testCases, suite.getTestCases());

  }
}
