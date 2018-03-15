package org.kanonizo.framework.objects;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ParameterisedTestCase extends TestCase {
  private Object[] parameters;
  private static Map<String, Integer> identifiers = new HashMap<>();
  private int id;
  public ParameterisedTestCase(Class<?> testClass, Method testMethod, Object[] parameters) {
    super(testClass, testMethod);
    String identifier = testClass.getName() + "." + testMethod.getName();
    if (identifiers.containsKey(identifier)) {
      identifiers.put(identifier, identifiers.get(identifier) + 1);
    } else {
      identifiers.put(identifier, 0);
    }
    this.id = identifiers.get(identifier);
    this.parameters = parameters;
  }

  public Object[] getParameters() {
    return parameters;
  }

  public String toString() {
    return super.toString().replace("(", "["+this.id+"](");
  }

}
