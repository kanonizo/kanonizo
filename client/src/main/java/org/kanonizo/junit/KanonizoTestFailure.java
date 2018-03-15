package org.kanonizo.junit;

public class KanonizoTestFailure {
  private Throwable cause;

  public KanonizoTestFailure(Throwable cause){
    this.cause = cause;
  }

  public Throwable getCause(){
    return cause;
  }
}
