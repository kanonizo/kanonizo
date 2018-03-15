package org.kanonizo.framework.instrumentation;

/**
 * This interface is an indicator that a class is designated to be instrumented. In the case of Test Case Prioritisation, both Source Classes and Test Classes must be instrumented in order to be able to accurately collect code coverage information.
 *
 */
public interface Instrumented {
	/**
	 * Callback to happen after instrumentation has finished. This is used usually to indicate that some new information has been collected about one or many classes. This method is called after every test case has been executed so that <i>per test</i> coverage information can be gathered. It is also called after classes have been analysed for the first time so that the total number of lines,
	 * coverable lines, branches etc can be collected.
	 */
	void instrumentationFinished();
}
