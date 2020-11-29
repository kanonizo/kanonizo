package org.kanonizo.display;

import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.display.fx.KanonizoFrame;

import java.util.Arrays;

public enum DisplayType
{
    NULL_DISPLAY(NullDisplay::new, "null"),
    CONSOLE_DISPLAY(ConsoleDisplay::new, "commandLine"),
    GUI_DISPLAY(KanonizoFrame::new, "gui");

    private final DisplayFactory<?> displayFactory;
    public final String commandLineSwitch;

    <T extends Display> DisplayType(DisplayFactory<T> displayFactory, String commandLineSwitch)
    {
        this.displayFactory = displayFactory;
        this.commandLineSwitch = commandLineSwitch;
    }

    public <T extends Display> DisplayFactory<T> getDisplayFactory()
    {
        return (DisplayFactory<T>) displayFactory;
    }

    public static DisplayType fromCommandLineSwitch(String commamndLineSwitch)
    {
        return Arrays.stream(values()).filter(display -> display.commandLineSwitch.equalsIgnoreCase(commamndLineSwitch)).findFirst().orElse(CONSOLE_DISPLAY);
    }

    @FunctionalInterface
    public interface DisplayFactory<T extends Display>
    {
        T from(KanonizoConfigurationModel configModel);
    }
}
