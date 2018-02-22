package org.kanonizo.reporting;

import org.kanonizo.framework.objects.TestSuite;

public class OriginalOrderingWriter extends TestCaseOrderingWriter{
	
	private TestSuite tsc;
	public OriginalOrderingWriter(TestSuite tsc){
		this.tsc = tsc;
	}
	

	protected String getLogFileName(){
		return "ORIGINAL.csv";
	}
	
	@Override
	protected void prepareCsv() {
		setHeaders(new String[]{"TestCase"});
		tsc.getOriginalOrdering().forEach(testCase -> {
			addRow(new String[]{testCase.getTestClass().getName() + "." + testCase.getMethod().getName()});
		});
	}

}
