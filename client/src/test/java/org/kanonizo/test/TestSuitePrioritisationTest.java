package org.kanonizo.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.kanonizo.algorithms.metaheuristics.GeneticAlgorithm.POPULATION_SIZE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kanonizo.Properties;
import org.kanonizo.Properties.CoverageApproach;
import org.kanonizo.TestSuitePrioritisation;
import org.kanonizo.util.Util;
import org.reflections.Reflections;

public class TestSuitePrioritisationTest extends MockitoTest {
  private CommandLine line;
  private java.util.Properties props = new java.util.Properties();
  private Reflections r = Util.getReflections();
  private Set<Field> parameters;
  private Map<Field, Object> values = new HashMap<>();

  @Before
  public void setup() {
    line = mock(CommandLine.class);
    props.clear();
    parameters = r.getFieldsAnnotatedWith(Parameter.class);
    for (Field f : parameters) {
      try {
        if(!f.isAccessible()){
          f.setAccessible(true);
        }
        values.put(f, f.get(null));
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
    when(line.getOptionProperties("D")).thenReturn(props);
  }

  @After
  public void tearDown() throws IllegalAccessException {
    for(Entry<Field, Object> param : values.entrySet()){
      Object p = param.getValue();
      if(p == null){
        p = "null";
      }
      if(!Modifier.isFinal(param.getKey().getModifiers())) {
        Util.setParameter(param.getKey(), p.toString());
      }
    }
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
    assertEquals(50, POPULATION_SIZE);
    Field populationSize = parameters.stream().filter(field -> field.getAnnotation(Parameter.class).key().equals("population_size")).findFirst().get();
    Util.setParameter(populationSize, "100");
    assertEquals(100, POPULATION_SIZE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetIllegalType() throws IllegalAccessException {
    Field populationSize = parameters.stream().filter(field -> field.getAnnotation(Parameter.class).key().equals("population_size")).findFirst().get();
    Util.setParameter(populationSize, "INVALID_TYPE");
    fail("Population size should not be settable to a string");
  }

}
