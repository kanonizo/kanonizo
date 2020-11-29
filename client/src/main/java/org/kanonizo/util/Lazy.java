package org.kanonizo.util;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Lazy<T> implements Supplier<T>
{
    private final Supplier<T> lazySupplier;
    private T instance;

    private Lazy(Supplier<T> lazySupplier)
    {
        this.lazySupplier = lazySupplier;
    }

    @Override
    public T get()
    {
        if (instance == null)
        {
            instance = lazySupplier.get();
        }
        return instance;
    }

    public void ifInstantiated(Consumer<T> actionIfInstantiated)
    {
        if (instance != null)
        {
            actionIfInstantiated.accept(instance);
        }
    }

    public void reset()
    {
        instance = null;
    }

    public static <T> Lazy<T> of(Supplier<T> lazySupplier)
    {
        return new Lazy<>(lazySupplier);
    }
}
