package org.kanonizo.framework.objects;

public final class Pair<T>
{
    private final T left;
    private final T right;

    private Pair(T left, T right)
    {
        this.left = left;
        this.right = right;
    }

    public T getLeft()
    {
        return left;
    }

    public T getRight()
    {
        return right;
    }

    public boolean contains(T obj)
    {
        return obj != null && (obj.equals(left) || obj.equals(right));
    }

    public static <T> Pair<T> of(T left, T right)
    {
        return new Pair<>(left, right);
    }
}
