package org.kanonizo.configuration.configurableoption;

import java.util.function.Function;

public class StringOption extends ConfigurableOption<String>
{
    public StringOption(
            String key,
            String defaultValue,
            Function<String, String> converter
    )
    {
        super(key, String.class, defaultValue, converter);
    }

    public static StringOption stringOption(String key, String defaultValue)
    {
        return stringOption(key, defaultValue, Function.identity());
    }

    private static StringOption stringOption(String key, String defaultValue, Function<String, String> converter)
    {
        return new StringOption(key, defaultValue, converter);
    }
}
