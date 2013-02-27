package edu.columbia.ldpd.hrwa;

import org.archive.io.arc.ARCRecord;

public class ArchiveRecordProcessorRunnable implements Runnable {
	
	private int uniqueRunnableId;
	private boolean running = true;
	private ARCRecord currentArcRecordBeingProcessed = null;
	
	public ArchiveRecordProcessorRunnable(int uniqueNumericId) {
		uniqueRunnableId = uniqueNumericId;
		
		//Each processor has its own unique connection to MySQL
		//TODO: Create MySQL connection
	}

	public void run() {
		while(running) {
			if(currentArcRecordBeingProcessed != null) {
				
				//TODO: Remove placeholder code below with real processing logic
				try { Thread.sleep(2000); }
				catch (InterruptedException e) { e.printStackTrace(); }
				
				
				currentArcRecordBeingProcessed = null; //done processing!
				System.out.println("Thread " + this.uniqueRunnableId + ": Finished processing.");
				
			} else {
				//Sleep when not actively processing anything
				try { Thread.sleep(100); }
				catch (InterruptedException e) { e.printStackTrace(); }
			}
		}
	}
	
	public void stop() {
		running = false;
	}
	
	public boolean isProcessingARecord() {
		return (currentArcRecordBeingProcessed != null);
	}

	/**
	 * Processes the given arcRecord.
	 * When processing begins, this instance's isProcessingARecord gets set to true.
	 * @param arcRecord
	 */
	public void processRecord(ARCRecord arcRecord) {
		if(currentArcRecordBeingProcessed != null) {
			HrwaManager.writeToLog("Error: ArchiveRecordProcessorRunnable with id " + this.uniqueRunnableId + " cannot accept a new ARCRecord to process because isProcessingARecord == true. This error should never appear if things were coded properly.", true, HrwaManager.LOG_TYPE_ERROR);
		} else {
			currentArcRecordBeingProcessed = arcRecord;
			System.out.println("Thread " + this.uniqueRunnableId + ": Just started processing.");
		}
		
	}
	
}
