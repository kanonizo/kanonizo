package org.kanonizo.framework.objects;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ParameterisedTestCase extends TestCase
{
    private final Object[] parameters;
    private static final Map<String, Integer> identifiers = new HashMap<>();
    private final int id;

    public ParameterisedTestCase(Class<?> testClass, Method testMethod, Object[] parameters)
    {
        super(testClass, testMethod);
        String identifier = testClass.getName() + "." + testMethod.getName();
        identifiers.compute(identifier, (key, currentValue) -> currentValue == null ? 0 : currentValue + 1);
        this.id = identifiers.get(identifier);
        this.parameters = parameters;
    }

    public Object[] getParameters()
    {
        return parameters;
    }

    public String toString()
    {
        return super.toString().replace("(", "[" + this.id + "](");
    }

}
