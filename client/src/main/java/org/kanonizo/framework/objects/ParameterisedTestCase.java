package org.kanonizo.framework.objects;

import java.lang.reflect.Method;

public class ParameterisedTestCase extends TestCase {
  private Object[] parameters;

  public ParameterisedTestCase(Class<?> testClass, Method testMethod, Object[] parameters){
    super(testClass, testMethod);
    this.parameters = parameters;
  }

}
