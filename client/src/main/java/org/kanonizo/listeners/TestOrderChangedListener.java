package org.kanonizo.listeners;

import org.kanonizo.framework.objects.TestCase;

import java.util.List;

@FunctionalInterface
public interface TestOrderChangedListener
{
    void testOrderingChanged(List<TestCase> newTestOrdering);
}
