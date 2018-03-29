package org.kanonizo;

import com.scythe.instrumenter.InstrumentationProperties;
import com.scythe.instrumenter.instrumentation.ClassReplacementTransformer;
import com.scythe.instrumenter.instrumentation.ClassReplacementTransformer.ShouldInstrumentChecker;
import com.scythe.instrumenter.instrumentation.InstrumentingClassLoader;
import java.io.IOException;
import java.lang.instrument.Instrumentation;

public class Premain {

  private static final String[] forbiddenPackages = new String[]{"org/kanonizo", "org/junit",
      "org/apache/commons/cli", "junit", "org/apache/bcel", "org/apache/logging/log4j",
      "org/objectweb/asm", "javax/", "java", "org/xml", "org/hamcrest", "com/intellij", "org/groovy"};


  public static boolean instrument;
  /**
   * Premain that will be triggered when application runs with this
   * attached as a Java agent.
   *
   * @param arg   runtime properties to change
   * @param instr Instrumentation instance to attach a ClassFileTransformer
   */
  public static void premain(String arg, Instrumentation instr) {
    ClassReplacementTransformer.addShouldInstrumentChecker((name) -> instrument);

    for (String s : forbiddenPackages) {
      ClassReplacementTransformer.addForbiddenPackage(s);
    }

    InstrumentingClassLoader loader = InstrumentingClassLoader.getInstance();
    InstrumentationProperties.INSTRUMENT_BRANCHES = false;
    InstrumentationProperties.WRITE_CLASS_IF_MODIFIED = true;
    instr.addTransformer((l, n, c, p, buf) -> {
      try {
        if(n != null) {
          return loader.modifyBytes(n, buf);
        }
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      } catch(Throwable e){
        e.printStackTrace();
      }
      return buf;
    });

  }
}
