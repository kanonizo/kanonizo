package org.kanonizo.util;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
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
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.Runner;
import org.kanonizo.annotations.OptionProvider;
import org.kanonizo.framework.Readable;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

public class Util {

  private static final Logger logger = LogManager.getLogger(Util.class);

  private static PrintStream defaultSysOut, defaultSysErr;

  static {
    defaultSysOut = System.out;
    defaultSysErr = System.err;
  }

  public static PrintStream getSysOut() {
    return defaultSysOut;
  }

  public static PrintStream getSysErr() {
    return defaultSysErr;
  }

  public static void suppressOutput() {
    System.setOut(NullPrintStream.instance);
    System.setErr(NullPrintStream.instance);
  }

  public static void resumeOutput() {
    System.setOut(defaultSysOut);
    System.setErr(defaultSysErr);
  }

  public static String getName(Class<?> cl) {
    return (cl.isAnonymousClass() || cl.isMemberClass() || cl.isLocalClass()
        ? cl.getName().substring(cl.getName().lastIndexOf(".") + 1) : cl.getSimpleName())
        + ".class";
  }

  public static String humanise(String paramName) {
    String[] parts = paramName.split("_");
    String human = Arrays.asList(parts).stream()
        .map(str -> str.substring(0, 1) + str.substring(1).toLowerCase())
        .reduce((a, b) -> a + " " + b).get();
    return human;
  }

  private static List<File> userEntries = new ArrayList<>();

  public static List<File> getUserEntries() {
    return userEntries;
  }

  /**
   * Method to add a folder or a jar file to the classpath. Invokes {@link URLClassLoader#addURL}
   * via reflection using the URL from the file object
   *
   * @param file - either a jar file or a directory to be added to the classpath
   * @throws SecurityException - if protected java classes are trying to be added back into the
   * classpath
   */
  public static void addToClassPath(File file) throws SecurityException {
    userEntries.add(file);
    if (file.isDirectory() || file.getName().endsWith(".jar")) {
      logger.info("Adding " + file.getName() + " to class path");
      try {
        File absoluteFile = file.getAbsoluteFile();
        if (file.getPath().startsWith("./")) {
          file = new File(file.getPath().substring(2));
          absoluteFile = file.getAbsoluteFile();
        }
        ClassLoader urlClassLoader = ClassLoader.getSystemClassLoader();
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
        method.setAccessible(true);
        method.invoke(urlClassLoader, new Object[]{absoluteFile.toURI().toURL()});
      } catch (NoSuchMethodException e) {
        e.printStackTrace();
      } catch (MalformedURLException e) {
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        e.printStackTrace();
      }
    }
  }

  public static void removeFromClassPath(File file) throws SecurityException {
    if (file != null) {
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
   * globally on the file system
   */
  public static File getFile(String property) {
    if (property == null || property.isEmpty()) {
      throw new IllegalArgumentException("Property must not be null or empty");
    }
    if (property.startsWith("./")) {
      property = property.substring(2);
    }
    File f = new File("./" + property);
    if (!f.exists()) {
      f = new File(property);
      if (!f.exists()) {
        throw new IllegalArgumentException(
            "File " + property
                + " could not be found in the current directory or on the global file system");
      }
    }
    return f;
  }

  private static PropertyChangeSupport changeSupport = new PropertyChangeSupport(Util.class);

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static void setParameter(Field f, String value)
      throws IllegalArgumentException, IllegalAccessException {
    Object old = f.get(null);
    Class<?> cl = f.getType();
    if (cl.isAssignableFrom(Number.class) || cl.isPrimitive()) {
      if (cl.equals(Long.class) || cl.equals(long.class)) {
        try {
          Long l = Long.parseLong(value);
          f.setLong(null, l);
        } catch (NumberFormatException e) {
          Double fl = Double.parseDouble(value);
          f.setLong(null, (long) fl.doubleValue());
        }
      } else if (cl.equals(Double.class) || cl.equals(double.class)) {
        Double d = Double.parseDouble(value);
        f.setDouble(null, d);
      } else if (cl.equals(Float.class) || cl.equals(float.class)) {
        Float fl = Float.parseFloat(value);
        f.setFloat(null, fl);
      } else if (cl.equals(Integer.class) || cl.equals(int.class)) {
        Double fl = Double.parseDouble(value);
        f.setInt(null, (int) fl.doubleValue());
      } else if (cl.equals(Boolean.class) || cl.equals(boolean.class)) {
        Boolean bl = Boolean.parseBoolean(value);
        f.setBoolean(null, bl);
      }
    } else if (cl.isAssignableFrom(String.class)) {
      f.set(null, value);
    } else if (cl.isAssignableFrom(File.class)) {
      f.set(null, value == null ? null : new File(value));
    }
    if (f.getType().isEnum()) {
      f.set(null, Enum.valueOf((Class<Enum>) f.getType(), value.toUpperCase()));
    }
    if (f.getAnnotation(Parameter.class).hasOptions()) {
      Method m = findOptionProvider(f);
      if(m != null){
        try {
          List<?> options = (List<?>) m.invoke(null, null);
          for(Object opt : options){
            if(!(opt instanceof Readable)){
              logger.error("Can't compare option "+opt+" as it is not readable");
            }
            if(((Readable)opt).readableName().equals(value)){
              f.set(null, opt);
              break;
            }
          }

        } catch (InvocationTargetException e) {
          e.printStackTrace();
        }
      }
    }
    changeSupport.firePropertyChange(f.getName(), old, value);
  }

  public static Method findOptionProvider(Field f) {
    List<Method> methods = Arrays.asList(f.getDeclaringClass().getMethods());
    String paramKey = f.getAnnotation(Parameter.class).key();
    Optional<Method> opt = methods.stream().filter(
        m -> m.isAnnotationPresent(OptionProvider.class) && m.getAnnotation(OptionProvider.class)
            .paramKey().equals(paramKey)).findFirst();
    if(opt.isPresent()){
      Method optionProvider = opt.get();
      if (optionProvider.getReturnType() != List.class) {
        logger.error("OptionProvider must return a list");
        return null;
      }
      if (!Modifier.isStatic(optionProvider.getModifiers())) {
        logger.error("OptionProvider must be static");
        return null;
      }
      return optionProvider;
    } else {
      return null;
    }
  }

  public static void addPropertyChangeListener(String propertyName, PropertyChangeListener pcl) {
    changeSupport.addPropertyChangeListener(propertyName, pcl);
  }

  public static void addPropertyChangeListener(PropertyChangeListener pcl) {
    changeSupport.addPropertyChangeListener(pcl);
  }

  public static void removePropertyChangeListener(String propertyName, PropertyChangeListener pcl) {
    changeSupport.removePropertyChangeListener(propertyName, pcl);
  }

  public static void removePropertyChangeListener(PropertyChangeListener pcl) {
    changeSupport.removePropertyChangeListener(pcl);
  }

  private static Reflections r;

  public static Reflections getReflections() {
    if (r == null) {
      Set<URL> packages = new HashSet<>(ClasspathHelper.forPackage("org.kanonizo"));
      packages.addAll(ClasspathHelper.forPackage("com.scythe"));
      r = new Reflections(new ConfigurationBuilder()
          .setUrls(packages)
          .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner(),
              new FieldAnnotationsScanner()));
    }
    return r;
  }

  public static boolean isTestClass(Class<?> cl) {
    if (Modifier.isAbstract(cl.getModifiers())) {
      return false;
    }
    if (Runner.class.isAssignableFrom(cl)) {
      return false;
    }
    if (cl.getSuperclass() != null && Modifier.isAbstract(cl.getSuperclass().getModifiers())
        && isTestClass(cl.getSuperclass())) {
      return true;
    }
    // junit 3 test classes must inherit from TestCase
    if (TestCase.class.isAssignableFrom(cl) && Modifier.isPublic(cl.getModifiers())
        && hasConstructor(cl)) {
      return true;
    }
    List<Method> methods = Arrays.asList(cl.getDeclaredMethods());
    if (methods.stream()
        .anyMatch(method -> method.getAnnotation(Test.class) != null)) {
      return true;
    }

    return false;
  }

  private static boolean hasConstructor(Class<?> cl) {
    return getConstructor(cl, new Class[0]) != null
        || getConstructor(cl, new Class[]{String.class}) != null;
  }

  public static <T> Constructor<T> getConstructor(Class<T> cl, Class... classes) {
    try {
      // constructor that takes a string for test name
      Constructor<T> con = cl.getConstructor(classes);
      return con;
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  public static String getSignature(Method m) {
    String sig;
    try {
      Field gSig = Method.class.getDeclaredField("signature");
      gSig.setAccessible(true);
      sig = (String) gSig.get(m);
      if (sig != null) {
        return sig;
      }
    } catch (IllegalAccessException | NoSuchFieldException e) {
      e.printStackTrace();
    }

    StringBuilder sb = new StringBuilder("(");
    for (Class<?> c : m.getParameterTypes()) {
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

  public static boolean doubleEquals(final double a, final double b) {
    if (a == b) {
      return true;
    }
    return Math.abs(a - b) < EPSILON; //EPSILON = 0.0000001d
  }

  public static <T> List<T> combine(Collection<T> one, Collection<T> two) {
    ArrayList<T> ret = new ArrayList<>();
    Iterator<T> it1 = one.iterator();
    while (it1.hasNext()) {
      ret.add(it1.next());
    }
    Iterator<T> it2 = two.iterator();
    while (it2.hasNext()) {
      ret.add(it2.next());
    }
    return ret;
  }

  public static <T> List<T> combine(Enumeration<T> one, Enumeration<T> two) {
    ArrayList<T> ret = new ArrayList<>();
    while (one.hasMoreElements()) {
      ret.add(one.nextElement());
    }

    while (two.hasMoreElements()) {
      ret.add(two.nextElement());
    }
    return ret;
  }

  public static <T> List<T> enumerationToList(Enumeration<T> enumeration) {
    ArrayList<T> ret = new ArrayList<>();
    while (enumeration.hasMoreElements()) {
      ret.add(enumeration.nextElement());
    }
    return ret;
  }
}
