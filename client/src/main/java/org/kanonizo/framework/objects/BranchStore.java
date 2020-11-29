package org.kanonizo.framework.objects;

import java.util.HashMap;
import java.util.Map;

public class BranchStore
{
    private final static Map<String, Branch> branches = new HashMap<>();

    public static void add(Branch b)
    {
        String key = b.getParent().getCUT().getName() + ":" + b.getLineNumber() + ":" + b.getBranchNumber();
        branches.put(key, b);
    }

    public static Branch with(ClassUnderTest parent, int lineNumber, int branchNumber)
    {
        String key = parent.getCUT().getName() + ":" + lineNumber + ":" + branchNumber;
        return branches.computeIfAbsent(key, (ignored) -> new Branch(parent, lineNumber, branchNumber));
    }
}
