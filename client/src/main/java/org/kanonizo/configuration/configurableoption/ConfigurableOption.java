package org.kanonizo.configuration.configurableoption;

import java.util.function.Function;

public class ConfigurableOption<T>
{
    public final String key;
    public final Class<T> requiredType;
    public final T defaultValue;
    public final Function<String, T> converter;

    public ConfigurableOption(
            String key,
            Class<T> requiredType,
            T defaultValue,
            Function<String, T> converter
    )
    {
        this.key = key;
        this.requiredType = requiredType;
        this.defaultValue = defaultValue;
        this.converter = converter;
    }

    public static <T> ConfigurableOption<T> configurableOptionFrom(
            String optionKey,
            Class<T> requiredType,
            T defaultValue
    )
    {
        return configurableOptionFrom(optionKey, requiredType, defaultValue, t -> null);
    }

    public static <T> ConfigurableOption<T> configurableOptionFrom(
            String optionKey,
            Class<T> requiredType,
            T defaultValue,
            Function<String, T> converter
    )
    {
        return new ConfigurableOption<>(optionKey, requiredType, defaultValue, converter);
    }
}
