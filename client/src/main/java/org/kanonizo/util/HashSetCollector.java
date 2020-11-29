package org.kanonizo.util;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Created by davidpaterson on 12/10/2015.
 */
public class HashSetCollector<T> implements Supplier<Set<T>>, Collector<Collection<T>, Set<T>, Set<T>>
{
    private final HashSet<T> set;

    public HashSetCollector()
    {
        set = new HashSet<>();
    }

    @Override
    public Supplier<Set<T>> supplier()
    {
        return this;
    }

    @Override
    public BiConsumer<Set<T>, Collection<T>> accumulator()
    {
        return Set::addAll;
    }

    @Override
    public BinaryOperator<Set<T>> combiner()
    {
        return (t, c) ->
        {
            t.addAll(c);
            return t;
        };
    }

    @Override
    public Function<Set<T>, Set<T>> finisher()
    {
        return t -> t;
    }

    @Override
    public Set<Characteristics> characteristics()
    {
        return EnumSet.of(Characteristics.UNORDERED);
    }

    @Override
    public Set<T> get()
    {
        return set;
    }
}
