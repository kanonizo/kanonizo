package com.dpaterson.testing.junit;

import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameter;

import com.dpaterson.testing.framework.TestCaseChromosome;

/**
 * Created by davidpaterson on 14/12/2016.
 */
public class TestingUtils {
    public static boolean isJUnit4Test(Method method) {
        return method.getAnnotation(Test.class) != null;
    }

    public static boolean isParameterizedTest(TestCaseChromosome tc) {
        return tc.getTestCase().getTestMethod().isAnnotationPresent(Parameter.class);
    }
}
