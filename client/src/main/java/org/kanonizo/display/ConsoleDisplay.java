package org.kanonizo.display;

import org.kanonizo.commandline.DefaultProgressBar;
import org.kanonizo.commandline.ProgressBar;
import org.kanonizo.commandline.ProgressBar.InertProgressBar;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.listeners.TestCaseSelectionListener;

import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static org.kanonizo.commandline.ProgressBar.PROGRESS_BAR_ENABLED_OPTION;
import static org.kanonizo.display.Display.Answer.NO;
import static org.kanonizo.display.Display.Answer.YES;

public class ConsoleDisplay implements Display, TestCaseSelectionListener
{
    private final ProgressBar progressBar;

    public ConsoleDisplay(KanonizoConfigurationModel configModel)
    {
        this.progressBar = configModel.getConfigurableOptionValue(PROGRESS_BAR_ENABLED_OPTION) ?
                new DefaultProgressBar(System.out) :
                new InertProgressBar();
    }

    @Override
    public void initialise()
    {

    }

    @Override
    public void testCaseSelected(TestCase tc)
    {

    }

    @Override
    public void reportProgress(double current, double max)
    {
        progressBar.reportProgress(current, max);
    }

    @Override
    public Answer ask(String question)
    {
        Scanner in = new Scanner(System.in);
        System.out.println(question + "\n> ");
        Response response = Response.from(in.nextLine());
        while (!response.isValid)
        {
            System.out.println("Invalid repsonse - please enter one of " +
                                       Response.validResponses
                                               .map(this::quote)
                                               .collect(joining(",")) +
                               "\n> "
            );
            response = Response.from(in.nextLine());
        }
        return response.isPositive ? YES : NO;
    }

    private String quote(String input)
    {
        return "\"" + input + "\"";
    }

    @Override
    public void notifyTaskStart(String name, boolean progress)
    {
        progressBar.setTitle(name);
    }

    private static class Response
    {
        private static final List<String> positiveResponses = asList("Yes", "yes", "y", "T", "t", "True", "true");
        private static final List<String> negativeResponses = asList("No", "no", "N", "n", "F", "f", "False", "false");
        private static final Stream<String> validResponses = Stream.concat(positiveResponses.stream(), negativeResponses.stream());

        private static final Predicate<String> responseIsPositive = positiveResponses::contains;
        private static final Predicate<String> responseIsNegative = negativeResponses::contains;
        private final boolean isPositive;
        private final boolean isValid;

        public Response(String input)
        {
            this.isPositive = responseIsPositive.test(input);
            this.isValid = !responseIsPositive.test(input) && !responseIsNegative.test(input);
        }

        private static Response from(String input)
        {
            return new Response(input);
        }
    }
}
