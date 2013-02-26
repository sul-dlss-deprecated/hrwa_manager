package edu.columbia.ldpd.hrwa;

public class ArchiveRecordProcessorRunnable implements Runnable {
	
	private boolean isProcessingARecord = false;
	private int uniqueRunnableId;
	private boolean running = true;
	
	public ArchiveRecordProcessorRunnable(int uniqueNumericId) {
		uniqueRunnableId = uniqueNumericId;
		
		//Each processor has its own unique connection to MySQL
		//TODO: Create MySQL connection
	}

	public void run() {
		while(running) {
			System.out.println("Thread " + uniqueRunnableId + ": running...");
			if(isProcessingARecord) {
				
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
		return isProcessingARecord;
	}
	
}
