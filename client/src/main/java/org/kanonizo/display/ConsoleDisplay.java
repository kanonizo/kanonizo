package org.kanonizo.display;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.kanonizo.commandline.ProgressBar;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;

public class ConsoleDisplay implements Display {

  private ProgressBar bar = new ProgressBar(System.out);

  @Override
  public void initialise() {

  }

  @Override
  public void fireTestCaseSelected(TestCase tc) {

  }

  @Override
  public void fireTestSuiteChange(TestSuite ts) {

  }

  @Override
  public void reportProgress(double current, double max) {
    bar.reportProgress(current, max);
  }

  private List<String> positiveResponses = Arrays
      .asList("Yes", "yes", "y", "T", "t", "True", "true");
  private List<String> negativeResponses = Arrays
      .asList("No", "no", "N", "n", "F", "f", "False", "false");
  private List<String> validResponses = Stream.concat(positiveResponses.stream(), negativeResponses.stream()).collect(
      Collectors.toList());

  @Override
  public int ask(String question) {
    Scanner in = new Scanner(System.in);
    System.out.println(question + "\n> ");
    String resp = "";
    while (!valid(resp)) {
      System.out.println("Invalid repsonse - please enter one of " + validResponses.stream()
          .reduce((x, y) -> "\"" + x + "\"" + ", \"" + y + "\"") + "\n> ");
      resp = in.nextLine();
    }
    if(positiveResponses.contains(resp)){
      return Display.RESPONSE_YES;
    } else if(negativeResponses.contains(resp)){
      return Display.RESPONSE_NO;
    } else {
      return Display.RESPONSE_INVALID;
    }
  }

  private boolean valid(String resp) {
    return validResponses.contains(resp);
  }

}
