package org.kanonizo.framework.objects;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.UnaryOperator;

import static java.util.Collections.singletonList;

public class Population<T> implements List<T>
{
    private final List<T> population;

    public Population()
    {
        this.population = new LinkedList<>();
    }

    public Population(Collection<T> existing)
    {
        this.population = new LinkedList<>(existing);
    }

    public T getBest()
    {
        return population.get(0);
    }

    public static <T> Population<T> singleton(T singleValue)
    {
        return new Population<>(singletonList(singleValue));
    }

    @Override
    public int size()
    {
        return population.size();
    }

    @Override
    public boolean isEmpty()
    {
        return population.isEmpty();
    }

    @Override
    public boolean contains(Object o)
    {
        return population.contains(o);
    }

    @Override
    public Iterator<T> iterator()
    {
        return population.iterator();
    }

    @Override
    public Object[] toArray()
    {
        return population.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a)
    {
        return population.toArray(a);
    }

    @Override
    public boolean add(T t)
    {
        return population.add(t);
    }

    @Override
    public boolean remove(Object o)
    {
        return population.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        return population.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c)
    {
        return population.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c)
    {
        return population.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        return population.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        return population.retainAll(c);
    }

    @Override
    public void replaceAll(UnaryOperator<T> operator)
    {
        population.replaceAll(operator);
    }

    @Override
    public void sort(Comparator<? super T> c)
    {
        population.sort(c);
    }

    @Override
    public void clear()
    {
        population.clear();
    }

    @Override
    public boolean equals(Object o)
    {
        return population.equals(o);
    }

    @Override
    public int hashCode()
    {
        return population.hashCode();
    }

    @Override
    public T get(int index)
    {
        return population.get(index);
    }

    @Override
    public T set(int index, T element)
    {
        return population.set(index, element);
    }

    @Override
    public void add(int index, T element)
    {
        population.add(index, element);
    }

    @Override
    public T remove(int index)
    {
        return population.remove(index);
    }

    @Override
    public int indexOf(Object o)
    {
        return population.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o)
    {
        return population.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator()
    {
        return population.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index)
    {
        return population.listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex)
    {
        return population.subList(fromIndex, toIndex);
    }
}
