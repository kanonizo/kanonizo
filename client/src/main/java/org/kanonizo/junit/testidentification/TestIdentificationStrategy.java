package org.kanonizo.junit.testidentification;

import org.kanonizo.framework.objects.TestCase;

import java.util.List;

public interface TestIdentificationStrategy
{
    boolean handles(Class<?> testClass);

    List<TestCase> testCasesFrom(Class<?> testClass);
}
