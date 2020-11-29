package org.kanonizo.configuration;

import org.apache.commons.cli.CommandLine;
import org.kanonizo.configuration.configurableoption.BooleanOption;
import org.kanonizo.configuration.configurableoption.ConfigurableOption;
import org.kanonizo.configuration.configurableoption.DoubleOption;
import org.kanonizo.configuration.configurableoption.FileOption;
import org.kanonizo.configuration.configurableoption.IntOption;
import org.kanonizo.configuration.configurableoption.StringOption;
import sun.awt.geom.AreaOp;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class KanonizoConfigurationModel
{
    private final Map<Object, Object> configuredOptions;

    public KanonizoConfigurationModel(HashMap<Object, Object> configuredOptions)
    {
        this.configuredOptions = configuredOptions;
    }

    public <T> T getConfigurableOptionValue(ConfigurableOption<T> configurableOption)
    {
        Object configuredOptionValue = configuredOptions.get(configurableOption.key);
        if (configuredOptionValue.getClass().isAssignableFrom(configurableOption.requiredType))
        {
            //noinspection unchecked
            return (T) configuredOptions.getOrDefault(configurableOption.key, configurableOption.defaultValue);
        }
        return configurableOption.defaultValue;
    }

    public int getIntOption(IntOption intOption)
    {
        return getConfigurableOptionValue(intOption);
    }

    public String getStringOption(StringOption stringOption)
    {
        return getConfigurableOptionValue(stringOption);
    }

    public boolean getBooleanOption(BooleanOption booleanOption)
    {
        return getConfigurableOptionValue(booleanOption);
    }

    public double getDoubleOption(DoubleOption doubleOption)
    {
        return getConfigurableOptionValue(doubleOption);
    }

    public File getFileOption(FileOption fileOption)
    {
        return getConfigurableOptionValue(fileOption);
    }

    public static KanonizoConfigurationModel fromCommandLine(CommandLine line)
    {
        return new KanonizoConfigurationModel(new HashMap<>(line.getOptionProperties("-D")));
    }

}
