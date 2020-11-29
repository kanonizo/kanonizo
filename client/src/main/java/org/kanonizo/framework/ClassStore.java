package org.kanonizo.framework;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kanonizo.framework.objects.ClassUnderTest;

public class ClassStore
{
    private static final Logger logger = LogManager.getLogger(ClassStore.class);
    private static final Map<String, ClassUnderTest> cuts = new HashMap<String, ClassUnderTest>();

    public static void add(String name, ClassUnderTest chrom)
    {
        cuts.put(name, chrom);
    }

    public static ClassUnderTest get(String name)
    {
        return cuts.get(name.replaceAll("/", "."));
    }

    public static ClassUnderTest get(int id)
    {
        Optional<ClassUnderTest> cut = cuts.values().stream().filter(cl -> cl.getId() == id).findFirst();
        if (cut.isPresent())
        {
            return cut.get();
        }
        logger.error("Trying to return CUT that doesn't exist!");
        return null;
    }
}
