package com.dpaterson.testing.util;

import java.util.Random;

public class RandomInstance {
  private static Random random = new Random();

  public static void setRandom(Random random) {
    RandomInstance.random = random;
  }

  public static void setSeed(long seed) {
    random.setSeed(seed);
  }

  public static int nextInt() {
    return random.nextInt();
  }

  public static int nextInt(int bound) {
    return random.nextInt(bound);
  }

  public static double nextDouble() {
    return random.nextDouble();
  }

  public static boolean nextBoolean() {
    return random.nextBoolean();
  }

}
