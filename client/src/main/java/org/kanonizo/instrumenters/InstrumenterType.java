package org.kanonizo.instrumenters;

import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.display.Display;
import org.kanonizo.framework.instrumentation.Instrumenter;

import java.io.File;
import java.util.Arrays;

public enum InstrumenterType
{
    NULL_INSTRUMENTER(NullInstrumenter::new, "null"),
    SCYTHE_INSTRUMENTER(ScytheInstrumenter::new, "scythe"),
    GZOLTAR_INSTRUMENTER(GZoltarInstrumenter::new, "gzoltar");


    private final InstrumenterFactory<?> instrumenterFactory;
    public final String commandLineSwitch;

    <T extends Instrumenter> InstrumenterType(InstrumenterFactory<T> instrumenterFactory, String commandLineSwitch)
    {
        this.instrumenterFactory = instrumenterFactory;
        this.commandLineSwitch = commandLineSwitch;
    }

    public static InstrumenterType fromCommandLineSwitch(String commandLineSwitch)
    {
        return Arrays.stream(InstrumenterType.values())
                .filter(instrumenter -> instrumenter.commandLineSwitch.equalsIgnoreCase(commandLineSwitch))
                .findFirst()
                .orElse(SCYTHE_INSTRUMENTER);
    }

    public <T extends Instrumenter> InstrumenterFactory<T> getInstrumenterFactory()
    {
        return (InstrumenterFactory<T>) instrumenterFactory;
    }

    @FunctionalInterface
    public interface InstrumenterFactory<T extends Instrumenter>
    {
        T from(KanonizoConfigurationModel configurationModel, Display display, File sourceFolder) throws Exception;
    }
}
