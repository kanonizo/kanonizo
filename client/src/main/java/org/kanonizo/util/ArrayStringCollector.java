package org.kanonizo.util;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class ArrayStringCollector implements Supplier<StringBuilder>, Collector<String, StringBuilder, String> {
  private StringBuilder ret;

  public ArrayStringCollector() {
    ret = new StringBuilder("[");
  }

  @Override
  public Supplier<StringBuilder> supplier() {
    // TODO Auto-generated method stub
    return this;
  }

  @Override
  public BiConsumer<StringBuilder, String> accumulator() {
    // TODO Auto-generated method stub
    return (t, c) -> t.append(c).append(", ");
  }

  @Override
  public BinaryOperator<StringBuilder> combiner() {
    // TODO Auto-generated method stub
    return (t, c) -> t.append(c).append(", ");
  }

  @Override
  public Function<StringBuilder, String> finisher() {
    return t -> t.append("]").toString();
  }

  @Override
  public Set<Characteristics> characteristics() {
    // TODO Auto-generated method stub
    return EnumSet.of(Characteristics.UNORDERED);
  }

  @Override
  public StringBuilder get() {
    return ret;
  }

}
