package org.kanonizo.commandline;

import java.io.PrintStream;

import org.apache.commons.lang.StringUtils;

public class ProgressBar {
	private PrintStream out;

	public ProgressBar(PrintStream out) {
		this.out = out;
	}

	public ProgressBar() {
		this(System.out);
	}

	public void setTitle(String title) {
		out.println(title);
	}

	public void reportProgress(double currentPoint, double totalPoints) {
		double percentageThrough = currentPoint / totalPoints * 100;
		// int repeats = title.length()
		out.print("\r|" + StringUtils.repeat("=", (int) percentageThrough)
				+ StringUtils.repeat(" ", 100 - (int) percentageThrough) + "| " + (int) percentageThrough + "%");
	}

	public void complete() {
		out.print("\n");
	}
}
