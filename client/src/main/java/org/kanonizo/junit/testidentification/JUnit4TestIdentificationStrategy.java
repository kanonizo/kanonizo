package org.kanonizo.junit.testidentification;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.junit.TestingUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JUnit4TestIdentificationStrategy implements TestIdentificationStrategy
{
    private final KanonizoConfigurationModel configModel;

    public JUnit4TestIdentificationStrategy(KanonizoConfigurationModel configModel)
    {
        this.configModel = configModel;
    }

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
                .map(method -> new TestCase(testClass, method, configModel))
                .collect(Collectors.toList());
    }
}
