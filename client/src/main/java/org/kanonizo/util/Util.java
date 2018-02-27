package org.kanonizo.util;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.kanonizo.Main;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

public class Util {

  public static String getName(Class<?> cl) {
    return (cl.isAnonymousClass() || cl.isMemberClass() || cl.isLocalClass()
        ? cl.getName().substring(cl.getName().lastIndexOf(".") + 1) : cl.getSimpleName()) + ".class";
  }

  /**
   * Method to add a folder or a jar file to the classpath. Invokes {@link URLClassLoader#addURL} via reflection using the URL from the file object
   *
   * @param file - either a jar file or a directory to be added to the classpath
   * @throws SecurityException - if protected java classes are trying to be added back into the classpath
   */
  public static void addToClassPath(File file) throws SecurityException {
    if (file.isDirectory() || file.getName().endsWith(".jar")) {
      Main.logger.info("Adding " + file.getName() + " to class path");
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

  /**
   * This method serves as a utility for finding files defined by the command line. It first checks in the current directory for a relative path, then checks globally on the file system. If the file
   * doesn't exist in either location, an IllegalArgumentException is thrown.
   *
   * @param property - usually one of the command line arguments defined that represent files.
   * @return
   * @throws IllegalArgumentException - if the file does not exist in the current directory or globally on the file system
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
            "File " + property + " could not be found in the current directory or on the global file system");
      }
    }
    return f;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static void setParameter(Field f, String value) throws IllegalArgumentException, IllegalAccessException {
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
    }
    if (f.getType().isEnum()) {
      f.set(null, Enum.valueOf((Class<Enum>) f.getType(), value.toUpperCase()));
    }
  }

  private static Reflections r;

  public static Reflections getReflections() {
    if (r == null) {
      r = new Reflections(new ConfigurationBuilder()
          .setUrls(ClasspathHelper.forClassLoader())
          .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner(), new FieldAnnotationsScanner()));
    }
    return r;
  }

  public static boolean isTestClass(Class<?> cl) {
    if(Modifier.isAbstract(cl.getModifiers())){
      return false;
    }
    List<Method> methods = Arrays.asList(cl.getMethods());
    if (methods.stream().anyMatch(method -> method.getName().startsWith("test"))) {
      return true;
    }
    if (methods.stream()
        .anyMatch(method -> Arrays.asList(method.getAnnotations()).contains(Test.class))) {
      return true;
    }
    return false;
  }

  public static String getSignature(Method m){
    String sig;
    try {
      Field gSig = Method.class.getDeclaredField("signature");
      gSig.setAccessible(true);
      sig = (String) gSig.get(m);
      if(sig!=null) return sig;
    } catch (IllegalAccessException | NoSuchFieldException e) {
      e.printStackTrace();
    }

    StringBuilder sb = new StringBuilder("(");
    for(Class<?> c : m.getParameterTypes())
      sb.append((sig= Array.newInstance(c, 0).toString())
          .substring(1, sig.indexOf('@')));
    return sb.append(')')
        .append(
            m.getReturnType()==void.class?"V":
                (sig=Array.newInstance(m.getReturnType(), 0).toString()).substring(1, sig.indexOf('@'))
        )
        .toString();
  }
}
