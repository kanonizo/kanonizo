package org.kanonizo.junit.testidentification;

import org.kanonizo.framework.objects.TestCase;

import java.util.List;

import static java.util.Collections.emptyList;

public class JUnit3TestCaseIdentificationStrategy implements TestIdentificationStrategy
{
    @Override
    public boolean handles(Class<?> testClass)
    {
        return false;
    }

    @Override
    public List<TestCase> testCasesFrom(Class<?> testClass)
    {
        return emptyList();
    }
}
