package org.kanonizo.framework;

import java.util.List;
import org.kanonizo.framework.objects.TestCase;

public interface ObjectiveFunction extends Readable {
    List<TestCase> sort(List<TestCase> candidates);
}
