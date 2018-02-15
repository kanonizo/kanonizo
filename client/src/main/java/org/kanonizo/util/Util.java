package org.kanonizo.util;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.kanonizo.Main;

public class Util {

  public static String getName(Class<?> cl) {
    return (cl.isAnonymousClass() || cl.isMemberClass() || cl.isLocalClass()
        ? cl.getName().substring(cl.getName().lastIndexOf(".") + 1) : cl.getSimpleName()) + ".class";
  }

  /**
   * Method to add a folder or a jar file to the classpath. Invokes {@link URLClassLoader#addURL} via reflection using the URL from the file object
   *
   * @param file
   *          - either a jar file or a directory to be added to the classpath
   * @throws SecurityException
   *           - if protected java classes are trying to be added back into the classpath
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
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
        method.setAccessible(true);
        method.invoke(urlClassLoader, new Object[] { absoluteFile.toURI().toURL() });
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
   * @param property
   *          - usually one of the command line arguments defined that represent files.
   * @throws IllegalArgumentException
   *           - if the file does not exist in the current directory or globally on the file system
   * @return
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
}
