package org.kanonizo.framework.objects;

public final class Pair<T>  extends org.apache.commons.lang3.tuple.Pair{
  private T left;
  private T right;

  public Pair(T left, T right){
    this.left = left;
    this.right = right;
  }
  @Override
  public T getLeft() {
    return left;
  }

  @Override
  public T getRight() {
    return right;
  }

  @Override
  public Object setValue(Object value) {
    throw new UnsupportedOperationException();
  }

  public boolean contains(T obj){
    return obj != null && (obj.equals(left) || obj.equals(right));
  }
}
