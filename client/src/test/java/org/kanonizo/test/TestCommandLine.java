package org.kanonizo.test;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingOptionException;
import org.junit.Before;
import org.junit.Test;
import org.kanonizo.Framework;
import org.kanonizo.Main;

public class TestCommandLine {

  private static final String SOURCE_OPTION = "s";
  private static final String SOURCE_FOLDER = "testing/src/sample_classes";
  private static final String TEST_OPTION = "t";
  private static final String TEST_FOLDER = "testing/test/sample_tests";
  private static final String ALGORITHM_OPTION = "a";
  private static final String ALGORITHM_CHOICE = "greedy";

  private CommandLine line;
  private Framework framework = Framework.getInstance();

  @Before
  public void setup() {
    line = mock(CommandLine.class);
    when(line.hasOption(SOURCE_OPTION)).thenReturn(true);
    when(line.getOptionValue(SOURCE_OPTION)).thenReturn(SOURCE_FOLDER);
    when(line.hasOption(TEST_OPTION)).thenReturn(true);
    when(line.getOptionValue(TEST_OPTION)).thenReturn(TEST_FOLDER);
    when(line.hasOption(ALGORITHM_OPTION)).thenReturn(true);
    when(line.getOptionValue(ALGORITHM_OPTION)).thenReturn(ALGORITHM_CHOICE);
  }

  @Test
  public void testSetupFramework() {
    try {
      Main.setupFramework(line, framework);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Not expecting exception to be thrown");
    }
  }

  @Test
  public void testMissingSource() {
    when(line.hasOption(SOURCE_OPTION)).thenReturn(false);
    try {
      Main.setupFramework(line, framework);
      fail("Expected Missing Option exception");
    } catch (MissingOptionException e) {

    } catch (Exception e) {
      fail("Got the wrong type of exception");
    }
  }

  @Test
  public void testMissingTest() {
    when(line.hasOption(TEST_OPTION)).thenReturn(false);
    try {
      Main.setupFramework(line, framework);
      fail("Expected Missing Option Exception");
    } catch (MissingOptionException e) {

    } catch (Exception e) {
      fail("Got the wrong type of exception");
    }
  }

  @Test
  public void testHelpOption() {
    // ensure that the main class can be run using just the -h flag to display
    // help (and that no other prioritisation happens in that case)
    Main.main(new String[]{"-h"});

  }
}
