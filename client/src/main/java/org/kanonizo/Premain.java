package org.kanonizo;

import com.scythe.instrumenter.InstrumentationProperties;
import com.scythe.instrumenter.instrumentation.ClassReplacementTransformer;
import com.scythe.instrumenter.instrumentation.ClassReplacementTransformer.ShouldInstrumentChecker;
import com.scythe.instrumenter.instrumentation.InstrumentingClassLoader;
import com.scythe.instrumenter.instrumentation.MockClassLoader;
import com.scythe.util.ClassNameUtils;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URLClassLoader;
import org.kanonizo.util.Util;

public class Premain {

  private static final String[] forbiddenPackages = new String[]{"org/kanonizo", "org/junit",
      "org/apache/commons/cli", "junit", "org/apache/bcel", "org/apache/logging/log4j",
      "org/objectweb/asm", "javax/", "java", "org/xml", "org/hamcrest", "com/intellij", "org/groovy"};

  /**
   * Premain that will be triggered when application runs with this
   * attached as a Java agent.
   *
   * @param arg   runtime properties to change
   * @param instr Instrumentation instance to attach a ClassFileTransformer
   */
  public static void premain(String arg, Instrumentation instr) {
    ClassReplacementTransformer.addShouldInstrumentChecker(new ShouldInstrumentChecker() {
      private ClassReplacementTransformer crt = new ClassReplacementTransformer();
      private MockClassLoader loader = new MockClassLoader(
          ((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs(),
          crt);

      private boolean isTestClass(Class<?> cl) {

        if (cl.isMemberClass() && isTestClass(cl.getEnclosingClass())) {
          return true;
        }
        if (cl.isAnonymousClass() && isTestClass(cl.getEnclosingClass())) {
          return true;
        }
        return Util.isTestClass(cl);
      }

      @Override
      public boolean shouldInstrument(String className) {
        try {
          Class<?> cl = loader.loadOriginalClass(ClassNameUtils.replaceSlashes(className));
          return !isTestClass(cl);
        } catch (ClassNotFoundException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
        return true;
      }
    });

    for (String s : forbiddenPackages) {
      ClassReplacementTransformer.addForbiddenPackage(s);
    }

    InstrumentingClassLoader loader = InstrumentingClassLoader.getInstance();
    InstrumentationProperties.INSTRUMENT_BRANCHES = false;
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
