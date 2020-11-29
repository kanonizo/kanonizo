package org.kanonizo.configuration.configurableoption;

import java.util.function.Function;

public class DoubleOption extends ConfigurableOption<Double>
{
    public DoubleOption(
            String key,
            Double defaultValue,
            Function<String, Double> converter
    )
    {
        super(key, Double.class, defaultValue, converter);
    }

    public static DoubleOption doubleOption(String key, Double defaultValue)
    {
        return doubleOption(key, defaultValue, ignored -> 0.0);
    }

    private static DoubleOption doubleOption(String key, Double defaultValue, Function<String, Double> converter)
    {
        return new DoubleOption(key, defaultValue, converter);
    }
}
