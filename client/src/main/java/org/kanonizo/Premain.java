package org.kanonizo;
import com.scythe.instrumenter.instrumentation.ClassReplacementTransformer;
import com.scythe.instrumenter.instrumentation.InstrumentingClassLoader;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class Premain {

  private static final String[] forbiddenPackages = new String[]{"org/kanonizo", "org/junit",
      "org/apache/commons/cli", "junit", "org/apache/bcel", "org/apache/logging/log4j",
      "org/objectweb/asm", "javax/", "java", "org/xml", "org/hamcrest"};

  /**
   * Premain that will be triggered when application runs with this
   * attached as a Java agent.
   *
   * @param arg   runtime properties to change
   * @param instr Instrumentation instance to attach a ClassFileTransformer
   */
  public static void premain(String arg, Instrumentation instr) {

    for (String s : forbiddenPackages) {
      ClassReplacementTransformer.addForbiddenPackage(s);
    }
    InstrumentingClassLoader loader = InstrumentingClassLoader.getInstance();

    instr.addTransformer((l, n, c, p, buf) -> {
      try {
        return loader.modifyBytes(n, buf);
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
      return buf;
    });

  }
}
