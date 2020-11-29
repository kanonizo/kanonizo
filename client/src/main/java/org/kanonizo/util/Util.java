package org.kanonizo.util;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.Runner;
import org.kanonizo.annotations.OptionProvider;
import org.kanonizo.framework.Readable;
import org.kanonizo.junit.TestingUtils;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static java.lang.String.format;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.asList;

public class Util
{
    private static final Logger logger = LogManager.getLogger(Util.class);

    private static PrintStream defaultSysOut, defaultSysErr;

    private static final List<StringArgumentConverter<?>> ARGUMENT_CONVERTERS = asList(
            new StringArgumentConverter<>(Long::parseLong, Field::setLong, Long.class, long.class),
            new StringArgumentConverter<>(Double::parseDouble, Field::setDouble, Double.class, double.class),
            new StringArgumentConverter<>(Integer::parseInt, Field::setInt, Integer.class, int.class),
            new StringArgumentConverter<>(Boolean::parseBoolean, Field::setBoolean, Boolean.class, boolean.class),
            new StringArgumentConverter<>(Float::parseFloat, Field::setFloat, Float.class, float.class),
            new StringArgumentConverter<>(Function.identity(), Field::set, String.class),
            new StringArgumentConverter<>(File::new, Field::set, File.class)
    );

    static
    {
        defaultSysOut = System.out;
        defaultSysErr = System.err;
    }

    public static PrintStream getSysOut()
    {
        return defaultSysOut;
    }

    public static PrintStream getSysErr()
    {
        return defaultSysErr;
    }

    public static void suppressOutput()
    {
        System.setOut(NullPrintStream.instance);
        System.setErr(NullPrintStream.instance);
    }

    public static void resumeOutput()
    {
        System.setOut(defaultSysOut);
        System.setErr(defaultSysErr);
    }

    public static String getName(Class<?> cl)
    {
        return (cl.isAnonymousClass() || cl.isMemberClass() || cl.isLocalClass()
                ? cl.getName().substring(cl.getName().lastIndexOf(".") + 1) : cl.getSimpleName())
                + ".class";
    }

    public static String humanise(String paramName)
    {
        String[] parts = paramName.split("_");
        return Arrays.stream(parts)
                .map(str -> str.charAt(0) + str.substring(1).toLowerCase())
                .reduce((a, b) -> a + " " + b).orElse(paramName);
    }

    private static final List<File> userEntries = new ArrayList<>();

    /**
     * Method to add a folder or a jar file to the classpath. Invokes {@link URLClassLoader#addURL}
     * via reflection using the URL from the file object
     *
     * @param file - either a jar file or a directory to be added to the classpath
     * @throws SecurityException - if protected java classes are trying to be added back into the
     *                           classpath
     */
    public static void addToClassPath(File file) throws SecurityException
    {
        userEntries.add(file);
        if (file.isDirectory() || file.getName().endsWith(".jar"))
        {
            logger.info("Adding " + file.getName() + " to class path");
            try
            {
                File absoluteFile = file.getAbsoluteFile();
                if (file.getPath().startsWith("./"))
                {
                    file = new File(file.getPath().substring(2));
                    absoluteFile = file.getAbsoluteFile();
                }
                ClassLoader urlClassLoader = ClassLoader.getSystemClassLoader();
                Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(urlClassLoader, absoluteFile.toURI().toURL());
            }
            catch (NoSuchMethodException | MalformedURLException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
            {
                e.printStackTrace();
            }
        }
    }

    public static void removeFromClassPath(File file) throws SecurityException
    {
        if (file != null)
        {
            userEntries.remove(file);
            logger.info("Removed " + file.getName() + " from class path");
        }
    }

    /**
     * This method serves as a utility for finding files defined by the command line. It first checks
     * in the current directory for a relative path, then checks globally on the file system. If the
     * file doesn't exist in either location, an IllegalArgumentException is thrown.
     *
     * @param property - usually one of the command line arguments defined that represent files.
     * @throws IllegalArgumentException - if the file does not exist in the current directory or
     *                                  globally on the file system
     */
    public static File getFile(String property)
    {
        if (property == null || property.isEmpty())
        {
            throw new IllegalArgumentException("Property must not be null or empty");
        }
        if (property.startsWith("./"))
        {
            property = property.substring(2);
        }
        File f = new File("./" + property);
        if (!f.exists())
        {
            f = new File(property);
            if (!f.exists())
            {
                throw new IllegalArgumentException(
                        "File " + property
                                + " could not be found in the current directory or on the global file system");
            }
        }
        return f;
    }

    public static <T> T convert(String valueToBeConverted, Class<T> toBeConvertedTo)
    {
        Optional<StringArgumentConverter<?>> matchingArgumentConverter = ARGUMENT_CONVERTERS.stream().filter(converter -> converter.matches(toBeConvertedTo)).findFirst();
        //noinspection unchecked
        return matchingArgumentConverter.map(converter -> (T) converter.convert(valueToBeConverted)).orElse(null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> void setParameter(Field field, String valueAsString)
            throws IllegalArgumentException, IllegalAccessException
    {
        if (!isStatic(field.getModifiers()))
        {
            return;
        }
        Object old = field.get(null);
        Class<T> fieldType = (Class<T>) field.getType();
        if (fieldType.isAssignableFrom(Number.class) || fieldType.isPrimitive())
        {
            ARGUMENT_CONVERTERS.forEach(argumentConverter -> {
                if (argumentConverter.matches(fieldType))
                {
                    argumentConverter.setValueOnField(field, valueAsString);
                }
            });
        }

        if (field.getType().isEnum())
        {
            field.set(null, Enum.valueOf((Class<Enum>) field.getType(), valueAsString.toUpperCase()));
        }
        if (field.getAnnotation(Parameter.class).hasOptions())
        {
            Method m = findOptionProvider(field);
            if (m != null)
            {
                try
                {
                    List<?> options = (List<?>) m.invoke(null);
                    for (Object opt : options)
                    {
                        if (!(opt instanceof Readable))
                        {
                            logger.error("Can't compare option " + opt + " as it is not readable");
                        }
                        if (((Readable) opt).readableName().equals(valueAsString))
                        {
                            field.set(null, opt);
                            break;
                        }
                    }

                }
                catch (InvocationTargetException e)
                {
                    e.printStackTrace();
                }
            }
        }
        changeSupport.firePropertyChange(field.getName(), old, valueAsString);
    }

    public static Method findOptionProvider(Field f)
    {
        List<Method> methods = asList(f.getDeclaringClass().getMethods());
        String paramKey = f.getAnnotation(Parameter.class).key();
        Optional<Method> opt = methods.stream().filter(
                m -> m.isAnnotationPresent(OptionProvider.class) && m.getAnnotation(OptionProvider.class)
                        .paramKey().equals(paramKey)).findFirst();
        if (opt.isPresent())
        {
            Method optionProvider = opt.get();
            if (optionProvider.getReturnType() != List.class)
            {
                logger.error("OptionProvider must return a list");
                return null;
            }
            if (!isStatic(optionProvider.getModifiers()))
            {
                logger.error("OptionProvider must be static");
                return null;
            }
            return optionProvider;
        }
        else
        {
            return null;
        }
    }

    private static Reflections r;

    public static Reflections getReflections()
    {
        if (r == null)
        {
            Set<URL> packages = new HashSet<>(ClasspathHelper.forPackage("org.kanonizo"));
            packages.addAll(ClasspathHelper.forPackage("com.scythe"));
            r = new Reflections(new ConfigurationBuilder()
                                        .setUrls(packages)
                                        .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner(),
                                                     new FieldAnnotationsScanner()
                                        ));
        }
        return r;
    }

    public static boolean isTestClass(Class<?> cl)
    {
        if (Modifier.isAbstract(cl.getModifiers()))
        {
            return false;
        }
        if (Runner.class.isAssignableFrom(cl))
        {
            return false;
        }
        if (cl.getSuperclass() != null && TestingUtils.getTestMethods(cl.getSuperclass()).size() > 0)
        {
            return true;
        }
        // junit 3 test classes must inherit from TestCase
        if (TestCase.class.isAssignableFrom(cl) && isPublic(cl.getModifiers())
                && hasNoArgsOrSingleStringConstructor(cl))
        {
            return true;
        }
        List<Method> methods = asList(cl.getDeclaredMethods());
        return methods.stream()
                .anyMatch(method -> method.getAnnotation(Test.class) != null);
    }

    private static boolean hasNoArgsOrSingleStringConstructor(Class<?> cl)
    {
        return getConstructorWithParameterTypes(cl).isPresent()
                || getConstructorWithParameterTypes(cl, String.class).isPresent();
    }

    public static <T> Optional<Constructor<T>> getConstructorWithParameterTypes(Class<T> cl, Class<?>... constructorParamClasses)
    {
        try
        {
            // constructor that takes a string for test name
            return Optional.of(cl.getConstructor(constructorParamClasses));
        }
        catch (NoSuchMethodException e)
        {
            return Optional.empty();
        }
    }

    public static String getSignature(Method m)
    {
        String sig;
        try
        {
            Field gSig = Method.class.getDeclaredField("signature");
            gSig.setAccessible(true);
            sig = (String) gSig.get(m);
            if (sig != null)
            {
                return sig;
            }
        }
        catch (IllegalAccessException | NoSuchFieldException e)
        {
            e.printStackTrace();
        }

        StringBuilder sb = new StringBuilder("(");
        for (Class<?> c : m.getParameterTypes())
        {
            sb.append((sig = Array.newInstance(c, 0).toString())
                              .substring(1, sig.indexOf('@')));
        }
        return sb.append(')')
                .append(
                        m.getReturnType() == void.class ? "V" :
                                (sig = Array.newInstance(m.getReturnType(), 0).toString())
                                        .substring(1, sig.indexOf('@'))
                )
                .toString();
    }

    static final double EPSILON = 0.0000001d;

    public static boolean doubleEquals(final double a, final double b)
    {
        if (a == b)
        {
            return true;
        }
        return Math.abs(a - b) < EPSILON; //EPSILON = 0.0000001d
    }

    public static <T> List<T> combine(Collection<T> one, Collection<T> two)
    {
        ArrayList<T> ret = new ArrayList<>(one);
        ret.addAll(two);
        return ret;
    }

    public static <T> List<T> combine(Enumeration<T> one, Enumeration<T> two)
    {
        ArrayList<T> ret = new ArrayList<>();
        while (one.hasMoreElements())
        {
            ret.add(one.nextElement());
        }

        while (two.hasMoreElements())
        {
            ret.add(two.nextElement());
        }
        return ret;
    }

    public static <T> List<T> enumerationToList(Enumeration<T> enumeration)
    {
        ArrayList<T> ret = new ArrayList<>();
        while (enumeration.hasMoreElements())
        {
            ret.add(enumeration.nextElement());
        }
        return ret;
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map)
    {
        List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (Entry<K, V> entry : list)
        {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    public static Set<Field> getParameters()
    {
        Reflections r = getReflections();
        return r.getFieldsAnnotatedWith(Parameter.class);
    }

    public static String runSystemCommand(String... command) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        String[] commandSh = new String[command.length + 2];
        commandSh[0] = "/bin/sh";
        commandSh[1] = "-c";
        System.arraycopy(command, 0, commandSh, 2, command.length);
        ProcessBuilder pb = new ProcessBuilder(commandSh);
        Process p = pb.start();
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String s;
        while ((s = stdIn.readLine()) != null)
        {
            sb.append(s);
        }
        return sb.toString();
    }

    public static int runIntSystemCommand(String... command) throws IOException
    {
        String result = runSystemCommand(command);
        try
        {
            return Integer.parseInt(result);
        }
        catch (NumberFormatException e)
        {
            throw new IOException(
                    format("Command did not return a numeric value: %s%nActual Result: %s",
                           String.join(" ", command),
                           result
                    )
            );
        }
    }

    static class StringArgumentConverter<T>
    {
        private final List<Class<?>> matchingClassTypes;
        private final Function<String, T> converterFunction;
        private final FieldSetter<T> fieldSetter;

        StringArgumentConverter(
                Function<String, T> converterFunction,
                FieldSetter<T> fieldSetter,
                Class<?>... matchingClassTypes
        )
        {
            this.matchingClassTypes = asList(matchingClassTypes);
            this.converterFunction = converterFunction;
            this.fieldSetter  = fieldSetter;
        }

        public boolean matches(Class<?> fieldType)
        {
            return matchingClassTypes.stream().anyMatch(fieldType::isAssignableFrom);
        }

        public T convert(String value)
        {
            return converterFunction.apply(value);
        }

        public void setValueOnField(Field field, String value)
        {
            try
            {
                fieldSetter.set(field, null, convert(value));
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
        }
    }

    @FunctionalInterface
    interface FieldSetter<T>
    {
        void set(Field f, Object object, T value) throws IllegalAccessException;
    }
}
