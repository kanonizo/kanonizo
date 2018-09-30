package org.kanonizo.commandline;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import java.io.PrintStream;

import org.apache.commons.lang.StringUtils;

public class ProgressBar {
	@Parameter(key="progressbar_enable", description = "Progress Bar can be used to see the progress of long running tasks such as reading coverage data, historical test information etc. However, in log files, this can use a lot of lines as the \\r character is not translated into the logs", category="tcp")
  public static boolean enable = true;

	private PrintStream out;

	public ProgressBar(PrintStream out) {
		this.out = out;
	}

	public ProgressBar() {
		this(System.out);
	}

	public void setTitle(String title) {
		if(enable){
		  out.println(title);
    }
	}

	public void reportProgress(double currentPoint, double totalPoints) {
	  if(enable) {
      double percentageThrough = currentPoint / totalPoints * 100;
      // int repeats = title.length()
      out.print("\r|" + StringUtils.repeat("=", (int) percentageThrough)
          + StringUtils.repeat(" ", 100 - (int) percentageThrough) + "| " + (int) percentageThrough
          + "%");
    }
	}

	public void complete() {
	  if(enable) {
      out.print("\n");
    }
	}
}
