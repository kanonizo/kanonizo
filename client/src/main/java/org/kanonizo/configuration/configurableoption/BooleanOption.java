package org.kanonizo.configuration.configurableoption;

import org.kanonizo.display.ConsoleDisplay;

import java.util.function.Function;

public class BooleanOption extends ConfigurableOption<Boolean>
{
    public BooleanOption(
            String key,
            Boolean defaultValue,
            Function<String, Boolean> converter
    )
    {
        super(key, Boolean.class, defaultValue, converter);
    }

    public static BooleanOption booleanOption(String key, boolean defaultValue)
    {
        return booleanOption(key, defaultValue, ignored -> false);
    }

    private static BooleanOption booleanOption(String key, boolean defaultValue, Function<String, Boolean> converter)
    {
        return new BooleanOption(key, defaultValue, converter);
    }
}
