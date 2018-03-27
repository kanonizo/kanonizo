package org.kanonizo.display.fx;

import java.util.Optional;
import java.util.Set;
import javafx.util.StringConverter;
import org.kanonizo.annotations.Readable;
import org.kanonizo.util.Util;
import org.reflections.Reflections;

public class ReadableConverter extends StringConverter {

  @Override
  public String toString(Object object) {
    return object.getClass().getAnnotation(Readable.class).readableName();
  }

  @Override
  public Object fromString(String string) {
    Reflections r = Util.getReflections();
    Set<Class<?>> candidates = r.getTypesAnnotatedWith(Readable.class);
    Optional<Class<?>> candidate = candidates.stream().filter(cl -> cl.getAnnotation(Readable.class).readableName().equals(string)).findFirst();
    if(candidate.isPresent()){
      try{
        return candidate.get().newInstance();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (InstantiationException e) {
        e.printStackTrace();
      }
    }
    return null;
  }
}
