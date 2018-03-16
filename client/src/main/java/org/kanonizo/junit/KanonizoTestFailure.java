package org.kanonizo.junit;

public class KanonizoTestFailure {
  private Throwable cause;
  private String trace;

  public KanonizoTestFailure(Throwable cause, String trace) {
    this.cause = cause;
    this.trace = trace;
  }

  public Throwable getCause() {
    return cause;
  }


  public String getTrace() {
    return trace;
  }
}
