package org.kanonizo.junit.testidentification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.kanonizo.framework.objects.TestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;

public class JUnit5TestIdentificationStrategy implements TestIdentificationStrategy
{
    @Override
    public boolean handles(Class<?> testClass)
    {
        return Arrays.stream(testClass.getMethods())
                .anyMatch(method ->
                                  method.isAnnotationPresent(Test.class)
                                          || method.isAnnotationPresent(ParameterizedTest.class)
                );
    }

    @Override
    public List<TestCase> testCasesFrom(Class<?> testClass)
    {
        return emptyList();
    }
}
