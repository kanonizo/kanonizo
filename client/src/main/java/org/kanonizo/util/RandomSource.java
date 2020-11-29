package org.kanonizo.util;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;

import java.util.Random;

public class RandomSource
{
    @Parameter(key = "random_seed", description = "Some algorithms rely on making random choices to help explore unpredictable behaviour. However, to test functionality, we can specify a random seed for consistency", category = "Randomness")
    public static long RANDOM_SEED = -1;

    private static Random random = new Random();

    public static void setRandom(Random random)
    {
        if (RANDOM_SEED != -1)
        {
            random.setSeed(RANDOM_SEED);
        }
        RandomSource.random = random;
    }

    public static void setSeed(long seed)
    {
        random.setSeed(seed);
    }

    public static int nextInt()
    {
        if (RANDOM_SEED != -1)
        {
            random.setSeed(RANDOM_SEED);
        }
        return random.nextInt();
    }

    public static int nextInt(int bound)
    {
        if (RANDOM_SEED != -1)
        {
            random.setSeed(RANDOM_SEED);
        }
        return random.nextInt(bound);
    }

    public static double nextDouble()
    {
        if (RANDOM_SEED != -1)
        {
            random.setSeed(RANDOM_SEED);
        }
        return random.nextDouble();
    }

    public static boolean nextBoolean()
    {
        if (RANDOM_SEED != -1)
        {
            random.setSeed(RANDOM_SEED);
        }
        return random.nextBoolean();
    }

}
