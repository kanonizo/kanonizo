package org.kanonizo.framework.objects;

import java.util.Objects;
import java.util.Set;

import org.kanonizo.Framework;
import org.kanonizo.framework.ClassStore;
import org.kanonizo.util.Util;

public class ClassUnderTest
{
    private static int count = 0;

    private final int id;
    private final Set<Line> lines;
    private final Set<Branch> branches;
    private final Class<?> cut;

    public ClassUnderTest(Class<?> cut)
    {
        this.cut = cut;
        this.id = ++count;
        this.lines = Framework.getInstance().getInstrumenter().getLines(this);
        this.branches = Framework.getInstance().getInstrumenter().getBranches(this);
        ClassStore.add(cut.getName(), this);
    }

    public void setParent(SystemUnderTest parent)
    {
    }

    public int getId()
    {
        return id;
    }

    public Class<?> getCUT()
    {
        return cut;
    }

    public Set<Line> getLines()
    {
        return lines;
    }

    public Set<Branch> getBranches()
    {
        return branches;
    }

    public ClassUnderTest clone()
    {
        return ClassStore.get(cut.getName());
    }

    @Override
    public String toString()
    {
        return Util.getName(cut);
    }

    public int size()
    {
        return Framework.getInstance().getInstrumenter().getTotalLines(this);
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
        ClassUnderTest oc = (ClassUnderTest) other;
        return oc.cut == cut && oc.id == id;
    }

    public int hashCode()
    {
        return Objects.hash(cut, id);
    }

    public static void resetCount()
    {
        count = 0;
    }
}