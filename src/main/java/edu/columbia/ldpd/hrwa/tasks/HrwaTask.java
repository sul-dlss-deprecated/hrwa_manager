package edu.columbia.ldpd.hrwa.tasks;

import edu.columbia.ldpd.hrwa.HrwaManager;
import edu.columbia.ldpd.hrwa.TimeStringFormat;

public abstract class HrwaTask {
	
	private long startTime = 0;
	
	public abstract void runTask();
	
	public void writeTaskHeaderMessageAndSetStartTime() {
		this.startTime = System.currentTimeMillis();
		
		HrwaManager.writeToLog(
				"\n---------------------------------------------\n" +
				"Running task: " + this.getClass().getName() + "\n" +
				"---------------------------------------------",
				true,
				HrwaManager.LOG_TYPE_ALL);
	}
	
	public void writeTaskFooterMessageAndPrintTotalTime() {
		HrwaManager.writeToLog(
				"---------------------------------------------\n" +
				"Task complete: " + this.getClass().getName() + "\n" +
				"Total time: " + TimeStringFormat.getTimeString((System.currentTimeMillis() - this.startTime)/1000) + "\n" +
				"---------------------------------------------\n",
				true,
				HrwaManager.LOG_TYPE_STANDARD);
	}
	
}
