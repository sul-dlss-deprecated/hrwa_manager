package edu.columbia.ldpd.hrwa;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;
import org.archive.nutchwax.tools.ArcReader;

public class ArchiveFileProcessorRunnable implements Runnable {
	
	private int uniqueRunnableId;
	private boolean running = true;
	private File currentArcFileBeingProcessed = null;
	private long numRelevantArchiveRecordsProcessed = 0;
	private final MimetypeDetector mimetypeDetector;
	private Boolean isProcessingAnArchiveFile = false;
	
	public ArchiveFileProcessorRunnable(int uniqueNumericId) {
		//Assign uniqueNumericId
		uniqueRunnableId = uniqueNumericId;
		
		//Create a MimetypeDetector
		mimetypeDetector = new MimetypeDetector();
		
		//Each processor has its own unique connection to MySQL
		//TODO: Create MySQL connection
	}
	
	public int getUniqueRunnableId() {
		return this.uniqueRunnableId;
	}

	public void run() {
		while(true) {
			
			if( ! running ) {
				break;
			}
			
			if(currentArcFileBeingProcessed != null) {
				
				if( ! HrwaManager.previewMode ) {
					processArchiveFile(currentArcFileBeingProcessed);
				}
				
				//done processing!
				synchronized (isProcessingAnArchiveFile) {
					currentArcFileBeingProcessed = null;
					isProcessingAnArchiveFile = false;
				}
				//System.out.println("Thread " + this.uniqueRunnableId + ": Finished processing.");
				
			} else {
				//Sleep when not actively processing anything
				try { Thread.sleep(5); }
				catch (InterruptedException e) { e.printStackTrace(); }
			}
			
		}
		
		System.out.println("THREAD " + getUniqueRunnableId() + " COMPLETE!");
	}
	
	public void processArchiveFile(File archiveFile) {
		//We need to get an ArchiveReader for this file
		ArchiveReader archiveReader = null;
		
		try {
			
			archiveReader = ArchiveReaderFactory.get(archiveFile);
		
			archiveReader.setDigest(true);

			// Wrap archiveReader in NutchWAX ArcReader class, which converts WARC
			// records to ARC records on-the-fly, returning null for any records that
			// are not WARC-Type "response".
			ArcReader arcReader = new ArcReader(archiveReader);
			
			String archiveFileName = archiveFile.getName();
			
			long memoryLogNotificationCounter = 0;
			
			// Loop through all archive records in this file
			for (ARCRecord arcRecord : arcReader) {
				
				if( numRelevantArchiveRecordsProcessed == 530 ) {
					System.out.println("We're about to crash! File info below for 530:");
					System.out.println(arcRecord.getMetaData().getOffset());
				}
				
				if (arcRecord == null) {
					// WARC records that are not of WARC-Type response will be set to null, and we don't want to analyze these.
					// Need to check for null.
					//HrwaManager.writeToLog("Notice: Can't process null record in " + archiveFile.getPath(), true, HrwaManager.LOG_TYPE_NOTICE);
				}
				else if(arcRecord.getMetaData().getUrl().startsWith("dns:")) {
					//Do not do mimetype/lang analysis on DNS records
					//HrwaManager.writeToLog("Notice: Skipping DNS record in " + archiveFile.getPath(), true, HrwaManager.LOG_TYPE_NOTICE);
					//HrwaManager.writeToLog("Current memory usage: " + HrwaManager.getCurrentAppMemoryUsage(), true, HrwaManager.LOG_TYPE_MEMORY);
				}
				else
				{
					//We'll be distributing the processing of these records between multiple threads
					this.processSingleArchiveRecord(arcRecord, archiveFileName);
					
					this.numRelevantArchiveRecordsProcessed++;
					System.out.println("Thread " + uniqueRunnableId + " records processed: " + this.numRelevantArchiveRecordsProcessed);
				}
				
				//Every x number of records, print a line in the memory log to keep track of memory consumption over time
				if(memoryLogNotificationCounter > 1500) {
					HrwaManager.writeToLog("Current memory usage: " + HrwaManager.getCurrentAppMemoryUsage(), true, HrwaManager.LOG_TYPE_MEMORY);
					memoryLogNotificationCounter = 0;
				} else {
					memoryLogNotificationCounter++;
				}
			}
		
		} catch (IOException e) {
			HrwaManager.writeToLog("An error occurred while trying to read in the archive file at " + archiveFile.getPath(), true, HrwaManager.LOG_TYPE_ERROR);
			HrwaManager.writeToLog(e.getMessage(), true, HrwaManager.LOG_TYPE_ERROR);
			HrwaManager.writeToLog("Skipping " + archiveFile.getPath() + " due to the read error. Moving on to the next file...", true, HrwaManager.LOG_TYPE_ERROR);
		} finally {
			try {
				archiveReader.close();
			} catch (IOException e) {
				HrwaManager.writeToLog("An error occurred while trying to close the ArchiveReader for file: " + archiveFile.getPath(), true, HrwaManager.LOG_TYPE_ERROR);
				e.printStackTrace();
			}
		}
	}
	
	public long getNumRelevantArchiveRecordsProcessed() {
		return this.numRelevantArchiveRecordsProcessed;
	}
	
	private void processSingleArchiveRecord(ARCRecord arcRecord, String parentArchiveFileName) {
		//Important! Get http header before running record.skipHttpHeader()
		String httpHeaderString = StringUtils.join(arcRecord.getHttpHeaders());
		
		try {
			arcRecord.skipHttpHeader(); //This advances the read pointer to the blob portion of the archive record, excluding the header
		} catch (IOException ex) {
			HrwaManager.writeToLog("Error: Cannot skip header in archive record", true, HrwaManager.LOG_TYPE_ERROR);
		}
	
		ARCRecordMetaData arcRecordMetaData = arcRecord.getMetaData();
		
		//Step 1: Create .blob file and .blob.header file
		String fullPathToNewlyCreatedBlobFile = createBlobAndHeaderFilesForRecord(arcRecord, arcRecordMetaData, httpHeaderString, parentArchiveFileName);
		
		//Step 2: Run mimetype detection on blob file - Mimetype detection with Tika 1.2 is thread-safe.
		String detectedMimetype = mimetypeDetector.getMimetype(new File(fullPathToNewlyCreatedBlobFile));
		//System.out.println("Detected mimetype: " + detectedMimetype);
		
		//Step 3: Insert all info into MySQL
	}
	
	public void stop() {
		System.out.println("STOP was called on Thread " + getUniqueRunnableId());
		running = false;
	}
	
	public boolean isProcessingAnArchiveFile() {
		
		boolean bool;
		
		synchronized (isProcessingAnArchiveFile) {
			bool = isProcessingAnArchiveFile;
		}
		
		return bool;
	}

	/**
	 * Sends the given arcRecord off to be processed during this runnable's run() loop.
	 * How does this work?  The passed arcRecord is assigned to this.currentArcRecordBeingProcessed.
	 * This function returns almost immediately.  Actual processing happens asynchronously.
	 * @param archiveFile
	 */
	public void queueArchiveFileForProcessing(File archiveFile) {
		if(currentArcFileBeingProcessed != null) {
			HrwaManager.writeToLog("Error: ArchiveRecordProcessorRunnable with id " + this.uniqueRunnableId + " cannot accept a new ARCRecord to process because isProcessingARecord == true. This error should never appear if things were coded properly.", true, HrwaManager.LOG_TYPE_ERROR);
		} else {
			synchronized (isProcessingAnArchiveFile) {
				currentArcFileBeingProcessed = archiveFile;
				isProcessingAnArchiveFile = true;
			}
			//System.out.println("Thread " + this.uniqueRunnableId + ": Just started processing.");
		}
	}
	
	/**
	 * Creates a blob file for this record on the file system.
	 * @param arcRecord
	 * @return The full path to the newly created blob file.
	 */
	private String createBlobAndHeaderFilesForRecord(ARCRecord arcRecord, ARCRecordMetaData arcRecordMetaData, String httpHeaderString, String arcRecordParentArchiveFileName) {
		
		String fullPathToNewBlobFile = getBlobFilePathForRecord(arcRecord, arcRecordMetaData, arcRecordParentArchiveFileName);
		
		File blobFile = new File(fullPathToNewBlobFile);

        //Step 1: Write out the .blob file

        //Make directories if necessary
        blobFile.getParentFile().mkdirs();

        
        // Write blob to file using method below in order to use less memory. (I need all the memory I can get!) 
        try {
            FileOutputStream blobOutputStream = new FileOutputStream(blobFile);
            byte [] buffer = checkout();
            int len = 0;
            while ((len = arcRecord.read(buffer)) != -1) {
                blobOutputStream.write(buffer, 0, len);
            }
            blobOutputStream.flush();
            blobOutputStream.close();
            checkin(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            HrwaManager.writeToLog("Error: Could not write file (" + blobFile.getPath() + ") to disk.", true, HrwaManager.LOG_TYPE_ERROR);
        }

        String blob_path_with_header_extension = blobFile.getPath() + ".header";

        //Write out the header string to an associated .blob.header file
        try {
            FileUtils.writeStringToFile(new File(blob_path_with_header_extension), httpHeaderString);
        } catch (IOException e) {
            e.printStackTrace();
            HrwaManager.writeToLog("Error: Could not write file (" + blob_path_with_header_extension + ") to disk.", true, HrwaManager.LOG_TYPE_ERROR);
        }
        
		return fullPathToNewBlobFile;			
	}
	
	private String getBlobFilePathForRecord(ARCRecord arcRecord, ARCRecordMetaData arcRecordMetaData, String arcRecordParentArchiveFileName) {
		
		//Get the captureYearAndMonthString from the name of this record's parent file
		String captureYearAndMonthString = HrwaManager.getCaptureYearAndMonthStringFromArchiveFileName(arcRecordParentArchiveFileName);
		
		return HrwaManager.blobDirPath + File.separator + arcRecordParentArchiveFileName + File.separator + arcRecordMetaData.getOffset() + ".blob";
	}
	
	
	/* Byte check-in/check-out stuff */
	
	private static Deque<byte[]> BUFFER_POOL = new ArrayDeque<byte[]>();
	private static byte[] checkout() {
	    synchronized(BUFFER_POOL) {
	        if (BUFFER_POOL.isEmpty()) {
	            return new byte[8196];
	        } else {
	            return BUFFER_POOL.pop();
	        }
	    }
	}
	private static void checkin(byte[] buffer) {
	    synchronized(BUFFER_POOL) {
	        BUFFER_POOL.add(buffer);
	    }
	}
	
}
