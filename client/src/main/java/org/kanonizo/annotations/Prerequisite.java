package org.kanonizo.annotations;

import org.kanonizo.algorithms.SearchAlgorithm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Predicate;

public interface Prerequisite<T extends SearchAlgorithm> extends Predicate<T>
{
  String failureMessage() default "No failure message given";
}
