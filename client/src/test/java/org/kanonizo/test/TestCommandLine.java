package org.kanonizo.test;

import static org.mockito.Mockito.when;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingOptionException;
import org.junit.Before;
import org.junit.Test;
import org.kanonizo.Framework;
import org.kanonizo.Main;
import org.mockito.Mock;

public class TestCommandLine extends MockitoTest {
  private static final String SOURCE_OPTION = "s";
  private static final String SOURCE_FOLDER = "testing/sample_classes";
  private static final String TEST_OPTION = "t";
  private static final String TEST_FOLDER = "testing/sample_tests";
  private static final String ALGORITHM_OPTION = "a";
  private static final String ALGORITHM_CHOICE = "greedy";
  @Mock private CommandLine line;
  private Framework framework = Framework.getInstance();

  @Before
  public void setup() {
    when(line.hasOption(SOURCE_OPTION)).thenReturn(true);
    when(line.getOptionValue(SOURCE_OPTION)).thenReturn(SOURCE_FOLDER);
    when(line.hasOption(TEST_OPTION)).thenReturn(true);
    when(line.getOptionValue(TEST_OPTION)).thenReturn(TEST_FOLDER);
    when(line.hasOption(ALGORITHM_OPTION)).thenReturn(true);
    when(line.getOptionValue(ALGORITHM_OPTION)).thenReturn(ALGORITHM_CHOICE);
  }

  @Test
  public void testSetupFramework() throws MissingOptionException {
    Main.setupFramework(line, framework);
  }

  @Test(expected = MissingOptionException.class)
  public void testMissingSource() throws MissingOptionException {
    when(line.hasOption(SOURCE_OPTION)).thenReturn(false);
    Main.setupFramework(line, framework);
  }

  @Test(expected = MissingOptionException.class)
  public void testMissingTest() throws MissingOptionException {
    when(line.hasOption(TEST_OPTION)).thenReturn(false);
    Main.setupFramework(line, framework);
  }

  @Test
  public void testHelpOption() {
    // ensure that the main class can be run using just the -h flag to display
    // help (and that no other prioritisation happens in that case)
    Main.main(new String[] { "-h" });

  }
}
