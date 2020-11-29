package org.kanonizo.display;

import org.kanonizo.configuration.KanonizoConfigurationModel;

import static org.kanonizo.display.Display.Answer.NO;

public class NullDisplay implements Display
{

    public NullDisplay(KanonizoConfigurationModel configModel)
    {

    }

    @Override
    public void initialise()
    {

    }

    @Override
    public void reportProgress(double current, double max)
    {

    }

    @Override
    public Answer ask(String question)
    {
        return NO;
    }

    @Override
    public void notifyTaskStart(String name, boolean progress)
    {

    }
}
