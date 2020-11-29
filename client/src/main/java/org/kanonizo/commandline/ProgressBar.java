package org.kanonizo.commandline;

import org.kanonizo.configuration.configurableoption.ConfigurableOption;

import static org.kanonizo.configuration.configurableoption.ConfigurableOption.configurableOptionFrom;

public interface ProgressBar
{
    ConfigurableOption<Boolean> PROGRESS_BAR_ENABLED_OPTION = configurableOptionFrom("progressbar_enable",
                                                                                     Boolean.class,
                                                                                     true
    );

    void setTitle(String title);

    void reportProgress(double currentPoint, double totalPoints);

    void complete();

    class InertProgressBar implements ProgressBar
    {

        @Override
        public void setTitle(String title)
        {
        }

        @Override
        public void reportProgress(double currentPoint, double totalPoints)
        {
        }

        @Override
        public void complete()
        {
        }
    }
}
