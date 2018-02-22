package org.kanonizo.junit;

import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameter;

import org.kanonizo.framework.objects.TestCase;

/**
 * Created by davidpaterson on 14/12/2016.
 */
public class TestingUtils {
    public static boolean isJUnit4Test(Method method) {
        return method.getAnnotation(Test.class) != null;
    }

    public static boolean isParameterizedTest(TestCase tc) {
        return tc.getMethod().isAnnotationPresent(Parameter.class);
    }
}
