package org.kanonizo;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.util.Util;

import static java.util.stream.Collectors.toList;
import static org.kanonizo.TestSuitePrioritisation.CommandLineOption.terminalOptions;

public class TestSuitePrioritisation
{
    private static Logger logger = LogManager.getLogger(TestSuitePrioritisation.class);

    public static void handleProperties(CommandLine line, Set<Field> parameters)
    {
        java.util.Properties properties = line.getOptionProperties("-D");
        if (properties != null)
        {
            for (String property : properties.stringPropertyNames())
            {
                Optional<Field> f = parameters.stream().filter(field -> (field.getAnnotation(Parameter.class)).key().equals(
                        property)).findFirst();
                if (!f.isPresent())
                {
                    logger.info("Ignoring parameter " + property + " because it could not be found in any class file");
                    continue;
                }
                try
                {
                    Util.setParameter(f.get(), properties.getProperty(property));
                }
                catch (IllegalAccessException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}
