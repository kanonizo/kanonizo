package org.kanonizo.framework.objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Branch extends Goal implements Comparable<Branch>
{
    private final List<TestCase> coveringTestCases = new LinkedList<>();
    private final int branchNumber;

    public Branch(ClassUnderTest parent, int lineNumber, int branchNumber)
    {
        super(parent, lineNumber);
        this.branchNumber = branchNumber;
    }

    public int getBranchNumber()
    {
        return branchNumber;
    }

    public ClassUnderTest getParent()
    {
        return parent;
    }

    public List<TestCase> getCoveringTests()
    {
        return Collections.unmodifiableList(coveringTestCases);
    }

    @Override
    public int compareTo(Branch branch)
    {
        return (Double.compare(
                Double.parseDouble(lineNumber + "." + branchNumber),
                Double.parseDouble(branch.lineNumber + "." + branch.branchNumber)
        ));
    }

    public int hashCode()
    {
        return Objects.hash(parent, lineNumber, branchNumber);
    }

    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (other == null)
        {
            return false;
        }
        if (getClass() != other.getClass())
        {
            return false;
        }
        Branch otherBranch = ((Branch) other);
        return Objects.equals(otherBranch.getParent(), this.parent) &&
                Objects.equals(otherBranch.lineNumber, this.lineNumber) &&
                Objects.equals(otherBranch.branchNumber, this.branchNumber);
    }
}
