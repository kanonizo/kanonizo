package org.kanonizo.display.fx.converters;

import java.util.Optional;
import java.util.Set;
import javafx.util.StringConverter;
import org.kanonizo.framework.Readable;
import org.kanonizo.util.Util;
import org.reflections.Reflections;

public class ReadableConverter extends StringConverter {

  @Override
  public String toString(Object object) {
    return ((Readable) object).readableName();
  }

  @Override
  public Object fromString(String string) {
    Reflections r = Util.getReflections();
    Set<Class<? extends Readable>> candidates = r.getSubTypesOf(Readable.class);
    Optional<? extends Readable> candidate = candidates.stream().map(cl -> {
      try {
        return cl.newInstance();
      } catch (IllegalAccessException | InstantiationException e) {
        e.printStackTrace();
      }
      return null;
    }).filter(obj -> obj.readableName().equals(string)).findFirst();
    if (candidate.isPresent()) {
      return candidate.get();
    }

    return null;
  }
}
