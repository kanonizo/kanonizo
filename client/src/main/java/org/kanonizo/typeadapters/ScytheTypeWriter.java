package org.kanonizo.typeadapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.framework.ClassStore;
import org.kanonizo.framework.TestCaseStore;
import org.kanonizo.framework.objects.Branch;
import org.kanonizo.framework.objects.BranchStore;
import org.kanonizo.framework.objects.ClassUnderTest;
import org.kanonizo.framework.objects.Goal;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.LineStore;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.instrumenters.ScytheInstrumenter;
import org.kanonizo.junit.KanonizoTestFailure;
import org.kanonizo.junit.KanonizoTestResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.kanonizo.framework.objects.TestCase.Builder.aTestCase;

public class ScytheTypeWriter extends TypeAdapter<ScytheInstrumenter>
{
    private static final Logger logger = LogManager.getLogger(ScytheTypeWriter.class);
    private static final Pattern TEST_NAME_PATTERN = Pattern.compile("([^(])+\\(([^)]+)\\)");

    private final Map<TestCase, Set<Line>> linesCovered;
    private final Map<TestCase, Set<Branch>> branchesCovered;
    private final List<TestCase> testCases;

    public ScytheTypeWriter(
            Map<TestCase, Set<Line>> linesCovered,
            Map<TestCase, Set<Branch>> branchesCovered,
            List<TestCase> testCases
    )
    {

        this.linesCovered = linesCovered;
        this.branchesCovered = branchesCovered;
        this.testCases = testCases;
    }

    @Override
    public void write(JsonWriter out, ScytheInstrumenter inst)
            throws IOException
    {
        out.beginObject();
        out.name("linesCovered");
        writeCoverage(out, linesCovered);
        out.name("branchesCovered");
        writeCoverage(out, branchesCovered);
        out.name("testSuite");
        out.beginArray();
        for (TestCase tc : testCases)
        {
            out.beginObject();
            out.name("name");
            out.value(tc.toString());
            out.name("result");
            out.value(tc.wasSuccessful());
            out.name("errors");
            out.beginArray();
            for (KanonizoTestFailure f : tc.getFailures())
            {
                out.beginObject();
                out.name("bytes");
                out.value(Arrays.toString(SerializationUtils.serialize(f)));
                out.endObject();
            }
            out.endArray();
            out.name("time");
            out.value(tc.getExecutionTime());
            out.endObject();
        }
        out.endArray();
        out.endObject();
    }

    private <T extends Goal> void writeCoverage(JsonWriter out, Map<TestCase, Set<T>> coverage)
            throws IOException
    {
        out.beginObject();
        List<TestCase> orderedTestCases = new ArrayList<>(coverage.keySet());
        Collections.sort(orderedTestCases, Comparator.comparing(TestCase::getId));
        Iterator<TestCase> testCases = orderedTestCases.iterator();
        // tests
        while (testCases.hasNext())
        {
            TestCase tc = testCases.next();
            out.name(tc.toString());
            Set<ClassUnderTest> classesCovered = coverage.get(tc).stream().map(goal -> goal.getParent())
                    .collect(Collectors.toSet());
            out.beginObject();
            for (ClassUnderTest cut : classesCovered)
            {
                out.name(Integer.toString(cut.getId()));
                out.beginArray();
                Set<Goal> goalsCovered = coverage.get(tc).stream()
                        .filter(goal -> goal.getParent().equals(cut)).collect(Collectors.toSet());
                for (Goal g : goalsCovered)
                {
                    if (g instanceof Line)
                    {
                        out.value(g.getLineNumber());
                    }
                    else if (g instanceof Branch)
                    {
                        Double value = Double
                                .parseDouble(((Branch) g).getLineNumber() + "." + ((Branch) g).getBranchNumber());
                        out.value(value);
                    }
                }
                out.endArray();
            }
            out.endObject();
        }
        out.endObject();
    }

    @Override
    public ScytheInstrumenter read(JsonReader in) throws IOException
    {
        TestSuite testSuite = new TestSuite();
        in.beginObject();
        while (in.hasNext())
        {
            switch (in.nextName())
            {
                case "linesCovered":
                    linesCovered.clear();
                    linesCovered.putAll(readCoverage(in));
                    break;
                case "branchesCovered":
                    branchesCovered.clear();
                    branchesCovered.putAll(readCoverage(in));
                    break;
                case "testSuite":
                    in.beginArray();
                    while (in.hasNext())
                    {
                        in.beginObject();
                        in.nextName();
                        Matcher testNameMatcher = TEST_NAME_PATTERN.matcher(in.nextString());
                        testNameMatcher.find();
                        in.nextName();
                        boolean result = in.nextBoolean();
                        in.nextName();
                        in.beginArray();
                        ArrayList<KanonizoTestFailure> failures = new ArrayList<>();
                        while (in.hasNext())
                        {
                            in.beginObject();
                            in.nextName();
                            String bytesString = in.nextString();
                            byte[] bytes = getBytes(bytesString);
                            KanonizoTestFailure f = (KanonizoTestFailure) SerializationUtils.deserialize(bytes);
                            failures.add(f);
                            in.endObject();
                        }
                        in.endArray();
                        in.nextName();
                        long executionTime = in.nextLong();
                        String testClassName = testNameMatcher.group(1);
                        String testMethodName = testNameMatcher.group(2);
                        TestCase test = aTestCase()
                                .withClassName(testClassName)
                                .withMethodName(testMethodName)
                                .withTestResult(
                                        new KanonizoTestResult(
                                                testClassName,
                                                testMethodName,
                                                result,
                                                failures,
                                                executionTime
                                        )
                                ).build();
                        if (test != null)
                        {
                            testSuite.addTestCase(test);
                        }
                        else
                        {
                            logger.debug("Error deserialising test case " + testClassName + "(" + testMethodName + ").");
                        }
                        in.endObject();
                    }
                    in.endArray();
            }
        }
        in.endObject();
        return new ScytheInstrumenter(linesCovered, branchesCovered, testSuite);
    }

    private byte[] getBytes(String bytesString)
    {
        String elemsPart = bytesString.substring(1, bytesString.length() - 1);
        String[] parts = elemsPart.split(",");
        int elems = parts.length;
        byte[] bytes = new byte[elems];
        for (int i = 0; i < elems; i++)
        {
            bytes[i] = new Byte(parts[i].trim());
        }
        return bytes;
    }

    private <T extends Goal> Map<TestCase, Set<T>> readCoverage(JsonReader in) throws IOException
    {
        HashMap<TestCase, Set<T>> returnMap = new HashMap<>();

        // test cases
        in.beginObject();
        while (in.hasNext())
        {
            String testString = in.nextName();
            TestCase tc = TestCaseStore.with(testString);
            if (tc == null)
            {
                logger.debug("Error deserialising test case " + testString + ".");
            }
            Set<T> linesCovered = new HashSet<>();
            // classes
            in.beginObject();
            while (in.hasNext())
            {
                int cutId = Integer.parseInt(in.nextName());
                in.beginArray();
                while (in.hasNext())
                {
                    ClassUnderTest cut = ClassStore.get(cutId);
                    double goalNumber = in.nextDouble();
                    if (goalNumber == (int) goalNumber)
                    {
                        linesCovered.add((T) LineStore.with(cut, (int) goalNumber));
                    }
                    else
                    {
                        int lineNumber = (int) goalNumber;
                        int branchNumber = (int) (goalNumber - lineNumber);
                        linesCovered.add((T) BranchStore.with(cut, lineNumber, branchNumber));
                    }
                }
                in.endArray();
            }
            in.endObject();
            returnMap.put(tc, linesCovered);
        }
        in.endObject();
        return returnMap;
    }
}
