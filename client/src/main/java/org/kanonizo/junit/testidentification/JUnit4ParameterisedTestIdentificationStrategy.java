package org.kanonizo.junit.testidentification;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kanonizo.framework.objects.TestCase;

import java.util.List;

import static java.util.Collections.emptyList;

public class JUnit4ParameterisedTestIdentificationStrategy implements TestIdentificationStrategy
{
    @Override
    public boolean handles(Class<?> testClass)
    {
        return testClass.isAnnotationPresent(RunWith.class) &&
                testClass.getAnnotation(RunWith.class).value() == Parameterized.class;
    }

    @Override
    public List<TestCase> testCasesFrom(Class<?> testClass)
    {
        return emptyList();
    }
}
