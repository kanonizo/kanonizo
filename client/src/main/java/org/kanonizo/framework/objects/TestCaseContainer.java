package org.kanonizo.framework.objects;

import java.util.List;

public interface TestCaseContainer
{
    List<TestCase> getTestCases();

    List<ClassUnderTest> getClassesUnderTest();
}
