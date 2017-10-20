package org.kanonizo.mutation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import org.kanonizo.Properties;
import org.kanonizo.framework.TestCaseChromosome;
import org.kanonizo.framework.TestSuiteChromosome;
import org.kanonizo.util.Util;
import com.scythe.instrumenter.analysis.ClassAnalyzer;

public class Mutation {
  private static List<Mutant> mutants = new ArrayList<Mutant>();
  private static Map<TestCaseChromosome, List<Mutant>> killMap = new HashMap<TestCaseChromosome, List<Mutant>>();

  private Mutation() {
  }

  public static void initialise(TestSuiteChromosome tsc) {
    File mutantLog = Util.getFile(Properties.MUTANT_LOG);
    File killMap = Util.getFile(Properties.KILL_MAP);
    parseMutantLog(mutantLog);
    parseKillMap(killMap, tsc);
  }

  public static List<Mutant> getMutants() {
    return mutants;
  }

  private static void parseMutantLog(File mutantLog) {
    Scanner s = null;
    try {
      s = new Scanner(mutantLog);
      while (s.hasNextLine()) {
        String mutant = s.nextLine();
        String[] mut = mutant.split(":");
        int mutantId = Integer.parseInt(mut[0]);
        String target = mut[4];
        if (target.contains("@")) {
          target = target.substring(0, target.indexOf("@"));
        }
        Class<?> targetClass = Class.forName(target);
        int lineNumber = Integer.parseInt(mut[5]);
        ClassAnalyzer.out.println("Mutant added with id: " + mutantId + ", target class: " + targetClass.getSimpleName()
            + ", lineNumber: " + lineNumber);
        mutants.add(new Mutant(mutantId, targetClass, lineNumber));
      }
    } catch (FileNotFoundException e) {
      // something must have gone really wrong here....
      e.printStackTrace(ClassAnalyzer.out);
    } catch (ClassNotFoundException e) {
      // probably a parsing error
      e.printStackTrace(ClassAnalyzer.out);
    } finally {
      if (s != null) {
        s.close();
      }
    }
  }

  private static void parseKillMap(File kill, TestSuiteChromosome testSuite) {
    CSVParser parser = null;
    try {
      parser = new CSVParser(new FileReader(kill), CSVFormat.DEFAULT);
      for (CSVRecord record : parser.getRecords()) {
        if (record.getRecordNumber() == 0) {
          continue;
        }
        int testCase = Integer.parseInt(record.get(0));
        int mutantKilled = Integer.parseInt(record.get(1));
        TestCaseChromosome test = testSuite.getOriginalOrdering().get(testCase - 1);
        if (!killMap.containsKey(test)) {
          killMap.put(test, new ArrayList<Mutant>());
        }
        killMap.get(test).addAll(getMutants(mutant -> mutant.getMutantId() == mutantKilled));
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        parser.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static Map<TestCaseChromosome, List<Mutant>> getKillMap() {
    return killMap;
  }

  public static List<Mutant> getMutants(Predicate<Mutant> pred) {
    return mutants.stream().filter(pred).collect(Collectors.toList());
  }
}
