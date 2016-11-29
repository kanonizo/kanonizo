package com.dpaterson.testing.reporting;

import com.dpaterson.testing.framework.TestSuiteChromosome;

public class OriginalOrderingWriter extends TestCaseOrderingWriter{
	
	private TestSuiteChromosome tsc;
	public OriginalOrderingWriter(TestSuiteChromosome tsc){
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
