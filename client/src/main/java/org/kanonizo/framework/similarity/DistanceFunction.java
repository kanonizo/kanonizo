package org.kanonizo.framework.similarity;

public interface DistanceFunction<T> {
  double getDistance(T first, T second);
}
