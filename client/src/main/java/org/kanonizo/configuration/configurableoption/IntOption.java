package org.kanonizo.configuration.configurableoption;

import java.util.function.Function;

public class IntOption extends ConfigurableOption<Integer>
{
    public IntOption(
            String key,
            Integer defaultValue,
            Function<String, Integer> converter
    )
    {
        super(key, Integer.class, defaultValue, converter);
    }

    public static IntOption intOption(String key, int defaultValue)
    {
        return intOption(key, defaultValue, ignored -> 1);
    }

    private static IntOption intOption(String key, int defaultValue, Function<String, Integer> converter)
    {
        return new IntOption(key, defaultValue, converter);
    }
}
