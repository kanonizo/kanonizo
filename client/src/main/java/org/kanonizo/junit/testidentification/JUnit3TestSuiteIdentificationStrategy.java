package org.kanonizo.junit.testidentification;

import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.util.Util;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.kanonizo.util.Util.getConstructorWithParameterTypes;

public class JUnit3TestSuiteIdentificationStrategy implements TestIdentificationStrategy
{
    @Override
    public boolean handles(Class<?> testClass)
    {
        return getConstructorWithParameterTypes(testClass).isPresent();
    }

    @Override
    public List<TestCase> testCasesFrom(Class<?> testClass)
    {
        return emptyList();
    }
}
