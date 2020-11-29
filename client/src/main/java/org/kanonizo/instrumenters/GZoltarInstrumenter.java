package org.kanonizo.instrumenters;


import com.gzoltar.core.AgentConfigs;
import com.gzoltar.core.instr.InstrumentationLevel;
import com.gzoltar.core.model.Node;
import com.gzoltar.core.model.Transaction;
import com.gzoltar.core.runtime.Probe;
import com.gzoltar.core.runtime.ProbeGroup;
import com.gzoltar.core.spectrum.Spectrum;
import com.gzoltar.core.spectrum.SpectrumReader;
import org.kanonizo.configuration.KanonizoConfigurationModel;
import org.kanonizo.configuration.configurableoption.ConfigurableOption;
import org.kanonizo.display.Display;
import org.kanonizo.framework.ClassStore;
import org.kanonizo.framework.TestCaseStore;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.Branch;
import org.kanonizo.framework.objects.ClassUnderTest;
import org.kanonizo.framework.objects.Line;
import org.kanonizo.framework.objects.LineStore;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.junit.KanonizoTestFailure;
import org.kanonizo.junit.KanonizoTestResult;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.kanonizo.Properties.PROFILE;
import static org.kanonizo.configuration.configurableoption.ConfigurableOption.configurableOptionFrom;
import static org.kanonizo.framework.objects.TestCase.Builder.aTestCase;
import static org.kanonizo.instrumenters.InstrumenterType.GZOLTAR_INSTRUMENTER;

public class GZoltarInstrumenter implements Instrumenter
{
    private static final ConfigurableOption<File> GZ_FILE_OPTION = configurableOptionFrom(
            "gz_file",
            File.class,
            null,
            File::new
    );

    private final File gzFile;
    private final Spectrum spectrum;
    private final Map<TestCase, Set<Line>> linesCovered = new HashMap<>();
    private final Set<Line> totalCoverage = new HashSet<>();
    private final File sourceFolder;

    public GZoltarInstrumenter(
            KanonizoConfigurationModel configurationModel,
            Display display,
            File sourceFolder
    ) throws Exception
    {
        this.gzFile = configurationModel.getConfigurableOptionValue(GZ_FILE_OPTION);
        this.sourceFolder = sourceFolder;
        this.spectrum = getSpectrum();
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException
    {
        return Thread.currentThread().getContextClassLoader().loadClass(className);
    }

    private Spectrum getSpectrum() throws Exception
    {
        long startTime = System.currentTimeMillis();
        AgentConfigs agentConfigs = new AgentConfigs();
        agentConfigs.setInstrumentationLevel(InstrumentationLevel.NONE);
        try
        {
            SpectrumReader reader = new SpectrumReader(
                    sourceFolder.getAbsolutePath(), agentConfigs,
                    new FileInputStream(gzFile)
            );
            reader.read();
            return reader.getSpectrum();
        }
        finally
        {
            if (PROFILE)
            {
                System.out.println("Initial spectrum read completed in " + (System.currentTimeMillis() - startTime) + "ms");
            }
        }
    }

    @Override
    public void collectCoverage(TestSuite testSuite)
    {
        long startTime = System.currentTimeMillis();
        if (PROFILE)
        {
            System.out.println("Spectrum loaded in " + (System.currentTimeMillis() - startTime) + "ms");
        }
        List<Transaction> transactions = spectrum
                .getTransactions(); // a transaction is in fact a test case execution

        for (Transaction transaction : transactions)
        {
            String tcName = transaction.getName();
            String testClassName = tcName.substring(0, tcName.indexOf("#"));
            String testMethodName = tcName.substring(tcName.indexOf("#") + 1);
            List<Node> hits = spectrum.getHitNodes(transaction);
            Set<Line> linesHitByTestCase = new HashSet<>();
            for (Node hit : hits)
            {
                int lineNumber = hit.getLineNumber();
                String className = hit.getName().substring(0, hit.getName().indexOf("#")).replaceFirst("\\$", ".");
                ClassUnderTest cut = ClassStore.get(className);
                Line l = LineStore.with(cut, lineNumber);
                linesHitByTestCase.add(l);
                totalCoverage.add(l);
            }

            List<KanonizoTestFailure> failures = transaction.hasFailed() ?
                    singletonList(new KanonizoTestFailure(new Exception(), transaction.getStackTrace())) :
                    emptyList();

            TestCase testCase = aTestCase()
                    .withClassName(testClassName)
                    .withMethodName(testMethodName)
                    .withTestResult(
                            new KanonizoTestResult(
                                    testClassName,
                                    testMethodName,
                                    transaction.hasFailed(),
                                    failures,
                                    transaction.getRuntime()
                            )
                    ).build();

            linesCovered.putIfAbsent(testCase, linesHitByTestCase);
        }
        if (PROFILE)
        {
            System.out.println("Coverage deserialised in " + (System.currentTimeMillis() - startTime) + "ms");
        }
    }

    @Override
    public Set<Line> getLinesCovered(TestCase testCase)
    {
        if (!linesCovered.containsKey(testCase))
        {
            return Collections.emptySet();
        }
        return linesCovered.get(testCase);
    }

    @Override
    public Set<Branch> getBranchesCovered(TestCase testCase)
    {
        return Collections.emptySet();
    }

    @Override
    public int getTotalLines(ClassUnderTest cut)
    {
        return getLines(cut).size();
    }

    @Override
    public int getTotalBranches(ClassUnderTest cut)
    {
        return 0;
    }

    @Override
    public Set<Line> getLines(ClassUnderTest cut)
    {
        List<ProbeGroup> probeGroups = new ArrayList<>(spectrum.getProbeGroups());
        Optional<ProbeGroup> optGroup = probeGroups.stream()
                .filter(pg -> pg.getName().equals(cut.getCUT().getName())).findFirst();
        if (optGroup.isPresent())
        {
            ProbeGroup group = optGroup.get();
            List<Probe> probes = group.getProbes();
            Set<Integer> lineNumbers = probes.stream().map(p -> p.getNode().getLineNumber())
                    .collect(Collectors
                                     .toSet());
            return lineNumbers.stream().map(l -> LineStore.with(cut, l)).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    @Override
    public Set<Branch> getBranches(ClassUnderTest cut)
    {
        return Collections.emptySet();
    }

    @Override
    public int getTotalLines(SystemUnderTest sut)
    {
        return sut.getClassesUnderTest().stream().mapToInt(this::getTotalLines).sum();
    }

    @Override
    public int getTotalBranches(SystemUnderTest sut)
    {
        return 0;
    }

    @Override
    public Set<Line> getLinesCovered(ClassUnderTest cut)
    {
        return totalCoverage.stream().filter(l -> l.getParent().equals(cut))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Branch> getBranchesCovered(ClassUnderTest cut)
    {
        return Collections.emptySet();
    }

    @Override
    public ClassLoader getClassLoader()
    {
        return Thread.currentThread().getContextClassLoader();
    }

    @Override
    public String commandLineSwitch()
    {
        return GZOLTAR_INSTRUMENTER.commandLineSwitch;
    }

    @Override
    public String readableName()
    {
        return "gzoltar";
    }
}
