package org.kanonizo.display;

public interface Display
{
    enum Answer
    {
        YES,
        NO,
        INVALID
    }

    void initialise();

    void reportProgress(double current, double max);

    Answer ask(String question);

    void notifyTaskStart(String name, boolean progress);
}
