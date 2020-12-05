package org.kanonizo.listeners;

import org.kanonizo.framework.objects.TestCase;

@FunctionalInterface
public interface TestCaseSelectionListener
{
    void testCaseSelected(TestCase tc);
}
