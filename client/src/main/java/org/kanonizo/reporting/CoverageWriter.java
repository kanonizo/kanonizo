package org.kanonizo.reporting;

import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.Branch;
import org.kanonizo.framework.objects.ClassUnderTest;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CoverageWriter extends CsvWriter
{
    private final SystemUnderTest system;
    private final Instrumenter instrumenter;

    public CoverageWriter(
            Path logFileDirectory,
            String logFilename,
            SystemUnderTest system,
            Instrumenter instrumenter
    )
    {
        super(logFileDirectory, logFilename);
        this.system = system;
        this.instrumenter = instrumenter;
    }

    @Override
    public String getDir()
    {
        return "coverage";
    }

    @Override
    public void prepareCsv()
    {
        String[] headers = new String[]{"Class", "ClassId", "NumLinesCovered", "NumLinesMissed", "LinesCovered", "LinesMissed", "PercentageLineCoverage",
                "Total Branches", "BranchesCovered", "BranchesMissed", "PercentageBranchCoverage"};
        setHeaders(headers);
        List<ClassUnderTest> cuts = system.getClassesUnderTest();
        List<TestCase> testCases = system.getTestSuite().getTestCases();
        for (ClassUnderTest cut : cuts)
        {
            if (!cut.getCUT().isInterface())
            {
                Set<Line> linesCovered = new HashSet<>();
                Set<Branch> branchesCovered = new HashSet<>();
                for (TestCase tc : testCases)
                {
                    Set<Line> lines = instrumenter.getLinesCovered(tc).stream().filter(line -> line.getParent().equals(cut)).collect(
                            Collectors.toSet());
                    Set<Branch> branches = instrumenter.getBranchesCovered(tc);
                    linesCovered.addAll(lines);
                    branchesCovered.addAll(branches);
                }
                int totalLines = instrumenter.getTotalLines(cut);
                int totalBranches = instrumenter.getTotalBranches(cut);
                Set<Line> linesMissed = cut.getLines().stream().filter(line -> !linesCovered.contains(line))
                        .collect(HashSet::new, HashSet::add, HashSet::addAll);
                Set<Branch> branchesMissed = cut.getBranches().stream()
                        .filter(branch -> !branchesCovered.contains(branch))
                        .collect(HashSet::new, HashSet::add, HashSet::addAll);
                List<Line> orderedLinesCovered = new ArrayList<>(linesCovered);
                Collections.sort(orderedLinesCovered);
                List<Line> orderedLinesMissed = new ArrayList<>(linesMissed);
                Collections.sort(orderedLinesMissed);
                double percentageCoverage = totalLines > 0 ? (double) linesCovered.size() / totalLines : 0;
                double percentageBranch = totalBranches > 0 ? (double) branchesCovered.size() / totalBranches : 0;
                addRow(
                        cut.getCUT().getName(),
                        cut.getId(),
                        linesCovered.size(),
                        linesMissed.size(),
                        linesCovered.size() > 0 ? orderedLinesCovered.stream().map(Line::getLineNumber).map(Object::toString).collect(Collectors.joining(":")) : "",
                        linesMissed.size() > 0 ? orderedLinesMissed.stream().map(Line::getLineNumber).map(Object::toString).collect(Collectors.joining(":")) : "",
                        percentageCoverage,
                        totalBranches,
                        branchesCovered.size(),
                        branchesMissed.size(),
                        percentageBranch
                );
            }
        }
    }

}