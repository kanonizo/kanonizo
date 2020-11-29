package org.kanonizo.reporting;

import org.kanonizo.algorithms.TestSuitePrioritiser.EvolutionListener;

import java.util.function.Supplier;

public class FitnessWriter extends CsvWriter implements EvolutionListener
{
    private final Supplier<Integer> ageSupplier;
    private final Supplier<Double> fitnessSupplier;

    public FitnessWriter(Supplier<Integer> ageSupplier, Supplier<Double> fitnessSupplier)
    {
        this.ageSupplier = ageSupplier;
        this.fitnessSupplier = fitnessSupplier;
    }

    @Override
    public String getDir()
    {
        return "fitness";
    }

    @Override
    protected void prepareCsv()
    {
        setHeaders("Iteration", "Best Individual Fitness");
    }

    public void addRow(int iteration, double fitness)
    {
        super.addRow(Integer.toString(iteration), Double.toString(1 - fitness));
    }

    @Override
    public void evolutionComplete()
    {
        addRow(ageSupplier.get(), fitnessSupplier.get());
    }
}
