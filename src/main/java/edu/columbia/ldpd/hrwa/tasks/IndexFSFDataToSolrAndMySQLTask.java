package edu.columbia.ldpd.hrwa.tasks;

public class IndexFSFDataToSolrAndMySQLTask extends HrwaTask {

	public IndexFSFDataToSolrAndMySQLTask() {
		
	}
	
	public void runTask() {
		
		writeTaskHeaderMessageAndSetStartTime();
		
		//Do stuff
		
		writeTaskFooterMessageAndPrintTotalTime();
		
	}
	
}
