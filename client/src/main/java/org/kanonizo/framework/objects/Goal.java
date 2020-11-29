package org.kanonizo.framework.objects;

public class Goal
{
    protected ClassUnderTest parent;
    protected int lineNumber;

    public Goal(ClassUnderTest parent, int lineNumber)
    {
        this.parent = parent;
        this.lineNumber = lineNumber;
    }

    public ClassUnderTest getParent()
    {
        return parent;
    }

    public int getLineNumber()
    {
        return lineNumber;
    }

}
