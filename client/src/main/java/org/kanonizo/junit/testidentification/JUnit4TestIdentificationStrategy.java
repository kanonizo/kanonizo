package org.kanonizo.junit.testidentification;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.junit.TestingUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.kanonizo.junit.TestingUtils.isJUnit4Test;

public class JUnit4TestIdentificationStrategy implements TestIdentificationStrategy
{
    @Override
    public boolean handles(Class<?> testClass)
    {
        return !testClass.isAnnotationPresent(RunWith.class) &&
                Arrays.stream(testClass.getMethods()).anyMatch(method -> method.isAnnotationPresent(Test.class));
    }

    @Override
    public List<TestCase> testCasesFrom(Class<?> testClass)
    {
        return Arrays.stream(testClass.getMethods())
                .filter(TestingUtils::isJUnit4Test)
                .map(TestCase::from)
                .collect(Collectors.toList());
    }
}
