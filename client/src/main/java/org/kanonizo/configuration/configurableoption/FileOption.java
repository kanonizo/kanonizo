package org.kanonizo.configuration.configurableoption;

import java.io.File;
import java.util.function.Function;

public class FileOption extends ConfigurableOption<File>
{
    public FileOption(
            String key,
            Function<String, File> converter
    )
    {
        super(key, File.class, null, converter);
    }

    public static FileOption fileOption(String key)
    {
        return fileOption(key, File::new);
    }

    public static FileOption fileOption(String key, Function<String, File> converter)
    {
        return new FileOption(key, converter);
    }
}
