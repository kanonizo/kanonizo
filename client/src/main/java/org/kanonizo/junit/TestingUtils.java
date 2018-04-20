package org.kanonizo.junit;

import com.scythe.util.ArrayUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import junit.framework.TestSuite;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kanonizo.util.Util;

/**
 * Created by davidpaterson on 14/12/2016.
 */
public class TestingUtils {

  public static boolean isJUnit4Class(Class<?> cl) {
    if (!Util.isTestClass(cl)) {
      return false;
    }
    List<Method> methods = Arrays.asList(cl.getMethods());
    boolean anyTestMethods = methods.stream().anyMatch(m -> isJUnit4Test(m));
    boolean constructorCheck = cl.getConstructors().length == 1 && Modifier.isPublic(cl.getConstructors()[0].getModifiers());
    return constructorCheck && (anyTestMethods || (cl.isMemberClass() && isJUnit4Class(cl.getEnclosingClass())));
  }

  public static boolean isJUnit4Test(Method method) {
    Class<?> testClass = method.getDeclaringClass();
    Class<?> superClass = testClass.getSuperclass();
    while (superClass != null) {
      Method[] methods = superClass.getDeclaredMethods();
      Optional<Method> superMethod = Arrays.asList(methods).stream()
          .filter(m -> m.getName().equals(method.getName())).findFirst();
      if (superMethod.isPresent() && superMethod.get().getAnnotation(Test.class) != null) {
        return true;
      } else {
        superClass = superClass.getSuperclass();
      }
    }
    return method.getAnnotation(Test.class) != null;
  }

  public static boolean isParameterizedTest(Class<?> cl, Method m) {
    if (cl.isAnnotationPresent(RunWith.class)) {
      RunWith runner = cl.getAnnotation(RunWith.class);
      if (runner.value().equals(Parameterized.class)) {
        return true;
      }
    }
    return false;
  }

  public static List<Method> getTestMethods(Class<?> cl) {
    List<Method> testMethods = Arrays.asList(cl.getMethods());
    if (isJUnit4Class(cl)) {
      testMethods = testMethods.stream()
          .filter(method -> isJUnit4Test(method) && method.getAnnotation(Ignore.class) == null)
          .collect(Collectors.toList());
    } else {
      testMethods = testMethods.stream().filter(method -> method.getName().startsWith("test") &&
          Modifier.isPublic(method.getModifiers()) &&
          method.getParameterCount() == 0
      ).collect(Collectors.toList());
    }
    return testMethods;
  }

  public static boolean isSuiteContainer(TestSuite suite) {
    return suite.testCount() > 1 && Util.enumerationToList(suite.tests()).stream()
        .anyMatch(t -> t instanceof TestSuite && ((TestSuite) t).testCount() > 1);
  }


  public static TestSuite getTestSuite(Class<?> cl) {
    try {
      Method suite = cl.getMethod("suite");
      junit.framework.Test testSuite = (junit.framework.Test) suite.invoke(null);
      if (testSuite instanceof TestSuite) {
        TestSuite ts = (TestSuite) testSuite;
        return ts;
      }
    } catch (NoSuchMethodException e) {

    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
    return null;
  }

}
