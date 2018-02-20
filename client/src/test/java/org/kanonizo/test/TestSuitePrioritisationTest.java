package org.kanonizo.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import java.lang.reflect.Field;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.junit.Before;
import org.junit.Test;
import org.kanonizo.Properties;
import org.kanonizo.Properties.CoverageApproach;
import org.kanonizo.TestSuitePrioritisation;
import org.kanonizo.util.Util;
import org.mockito.Mock;
import org.reflections.Reflections;

public class TestSuitePrioritisationTest extends MockitoTest {
  @Mock
  private CommandLine line;
  private java.util.Properties props = new java.util.Properties();
  private Reflections r = Util.getReflections();
  private Set<Field> parameters;

  @Before
  public void setup() {
    props.clear();
    parameters = r.getFieldsAnnotatedWith(Parameter.class);
    when(line.getOptionProperties("D")).thenReturn(props);
  }

  @Test
  public void testSetCoverageApproach() {
    props.put("coverage_approach", "branch");
    assertEquals(CoverageApproach.LINE, Properties.COVERAGE_APPROACH);
    TestSuitePrioritisation.handleProperties(line, parameters);
    assertEquals(CoverageApproach.BRANCH, Properties.COVERAGE_APPROACH);
  }

  @Test
  public void testSetPropertyFromSource() throws IllegalAccessException {
    assertEquals(50, Properties.POPULATION_SIZE);
    Field populationSize = parameters.stream().filter(field -> field.getAnnotation(Parameter.class).key().equals("population_size")).findFirst().get();
    Util.setParameter(populationSize, "100");
    assertEquals(100, Properties.POPULATION_SIZE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetIllegalType() throws IllegalAccessException {
    Field populationSize = parameters.stream().filter(field -> field.getAnnotation(Parameter.class).key().equals("population_size")).findFirst().get();
    Util.setParameter(populationSize, "INVALID_TYPE");
    fail("Population size should not be settable to a string");
  }

}
