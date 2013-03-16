package edu.columbia.ldpd.hrwa.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.nutchwax.tools.ArcReader;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import edu.columbia.ldpd.hrwa.HrwaManager;
import edu.columbia.ldpd.hrwa.mysql.MySQLHelper;
import edu.columbia.ldpd.hrwa.processorrunnables.ArchiveFileProcessorRunnable;

public class ArchiveToMySQLTask extends HrwaTask {
	
	private String[] validArchiveFileExtensions = {"arc.gz", "warc.gz"};
	
	//How many threads will we create?  HrwaManager.maxUsableProcessors
	private ArchiveFileProcessorRunnable[] archiveRecordProcessorRunnables;
	Future[] archiveRecordProcessorFutures;
	ExecutorService fixedThreadPoolExecutorService;

	public ArchiveToMySQLTask() {
		archiveRecordProcessorRunnables = new ArchiveFileProcessorRunnable[HrwaManager.maxUsableProcessors];
		archiveRecordProcessorFutures = new Future[HrwaManager.maxUsableProcessors];
		fixedThreadPoolExecutorService = Executors.newFixedThreadPool(HrwaManager.maxUsableProcessors);
	}
	
	public void runTask() {
		
		writeTaskHeaderMessageAndSetStartTime();
		
		//Scan through warc file directory and generate an alphabetically-sorted list of all warc files to index
		File[] archiveFilesToProcess = getAlphabeticallySortedRecursiveListOfFilesFromArchiveDirectory(HrwaManager.archiveFileDirPath, validArchiveFileExtensions);
		
		int numberOfArchiveFilesToProcess = archiveFilesToProcess.length;
		
		if(numberOfArchiveFilesToProcess < 1) {
			HrwaManager.writeToLog("No files found for indexing in directory: " + HrwaManager.archiveFileDirPath, true, HrwaManager.LOG_TYPE_ERROR);
			System.exit(HrwaManager.EXIT_CODE_ERROR);
		}
		
		HrwaManager.writeToLog("Number of archive files to process: " + archiveFilesToProcess.length, true, HrwaManager.LOG_TYPE_STANDARD);
		
		//Create necessary MySQL tables
		try {
			MySQLHelper.createMimetypeCodesTableIfItDoesNotExist();
			MySQLHelper.createWebArchiveRecordsTableIfItDoesNotExist();
			MySQLHelper.createFullyIndexedArchiveFilesTableIfItDoesNotExist();
		} catch (SQLException e1) {
			HrwaManager.writeToLog("Error: Could not create one of the required MySQL tables.", true, HrwaManager.LOG_TYPE_ERROR);
		}
		
		initializeArchiveRecordProcessorThreads();
		
		//Iterate through and process all archive files
		for(int i = 0; i < numberOfArchiveFilesToProcess; i++) {
			HrwaManager.writeToLog(	"Processing archive file " + (i+1) + " of " + numberOfArchiveFilesToProcess + "\n" +
									"-- Name of archive file: " + archiveFilesToProcess[i].getName() + "\n" +
									"-- Total number of relevant archive records processed so far (at this exact moment): " + this.getTotalNumberOfRelevantArchiveRecordsProcessedAtThisExactMoment(),
									true, HrwaManager.LOG_TYPE_STANDARD);
			
			processSingleArchiveFile(archiveFilesToProcess[i]);
			
			System.out.println(HrwaManager.getCurrentAppRunTime()); //This doesn't need to be logged.
		}
		
		//CLEANUP TIME
		
		//Wait until all of the runnables are done processing.
		HrwaManager.writeToLog("Preparing to shut down processors...", true, HrwaManager.LOG_TYPE_STANDARD);
		HrwaManager.writeToLog("Allowing processors to complete their final tasks...", true, HrwaManager.LOG_TYPE_STANDARD);
		while( someArchiveRecordProcessorsAreStillRunning() ) {
			try {
				System.out.println("Waiting for final archive file processing to complete before shut down...");
				Thread.sleep(1000);
			}
			catch (InterruptedException e) { e.printStackTrace(); }
		}
		
		HrwaManager.writeToLog("All ArchiveRecordProcessorRunnables have finished processing!  Shutting down all " + HrwaManager.maxUsableProcessors + " processor threads.", true, HrwaManager.LOG_TYPE_STANDARD);
		
		shutDownArchiveRecordProcessorThreads();
		
		HrwaManager.writeToLog("Total number of archive records processed: " + this.getTotalNumberOfRelevantArchiveRecordsProcessedAtThisExactMoment(), true, HrwaManager.LOG_TYPE_STANDARD);
		
		writeTaskFooterMessageAndPrintTotalTime();
		
	}
	
	/**
	 * Passes the archiveFile to one of the available ArchiveFileProcessorRunnable worker threads.
	 * If no worker threads are currently available, this method waits until one is available.
	 * @param arcRecord
	 */
	public void processSingleArchiveFile(File archiveFile) {
		
		boolean lookingForAvailableProcessorThread = true;
		
		long currentMemoryUsage;
		while(lookingForAvailableProcessorThread) {
			
			currentMemoryUsage = HrwaManager.getCurrentAppMemoryUsageInBytes();
			
			if(currentMemoryUsage > HrwaManager.maxMemoryThresholdInBytesForStartingNewThreadProcesses) {
				
				//If current memory usage is too high, wait until it's lower before processing another file on another thread
				try {
					Thread.sleep(100);
					//System.out.println("Sleeping for X ms because no threads are available for processing...");
					if(HrwaManager.verbose) {
						System.out.println("HrwaManager ArchiveToMySQL Task is sleeping for 100 ms because memory usage is currently too high to concurrently start processing an additional file.  Waiting until usage is lower... (Current memory usage: " + HrwaManager.bytesToMegabytes(currentMemoryUsage) + " MB)");
					}
				}
				catch (InterruptedException e) { e.printStackTrace(); }
				
			} else {
				
				//Otherwise process normally and wait until another thread is available do work
				
				for(ArchiveFileProcessorRunnable singleProcessorRunnable : archiveRecordProcessorRunnables) {
					if( ! singleProcessorRunnable.isProcessingAnArchiveFile() ) {
						lookingForAvailableProcessorThread = false;
						
						singleProcessorRunnable.queueArchiveFileForProcessing(archiveFile);
						// Uncomment the line below to perform single-threaded
						// debugging/processing (by directly calling the
						// processArchiveFile() method). If you do uncomment this
						// line, then you should comment out the line above (because
						// you no longer want to queue archive file processing).
						//singleProcessorRunnable.processArchiveFile(archiveFile);
						break;
					}
				}
				
				try {
					Thread.sleep(100);
					if(HrwaManager.verbose) {
						System.out.println("HrwaManager ArchiveToMySQL Task is sleeping for 100 ms because no threads are currently available for processing...");
					}
				}
				catch (InterruptedException e) { e.printStackTrace(); }
			}
			
		}
		
	}
	
	public long getTotalNumberOfRelevantArchiveRecordsProcessedAtThisExactMoment() {
		
		long total = 0;
		
		for(ArchiveFileProcessorRunnable singleProcessor : archiveRecordProcessorRunnables) {
			total += singleProcessor.getNumRelevantArchiveRecordsProcessed();
		}
		
		return total;
	}
	
	/**
	 * Returns true if at least one of the processors is still running.
	 * @return
	 */
	public boolean someArchiveRecordProcessorsAreStillRunning() {
		for(ArchiveFileProcessorRunnable singleProcessor : archiveRecordProcessorRunnables) {
			if(singleProcessor.isProcessingAnArchiveFile()) {
				System.out.println("Thread " + singleProcessor.getUniqueRunnableId() + " is still running. --> " + singleProcessor.getNumRelevantArchiveRecordsProcessed());
				return true;
			}
		}
		return false;
	}
	
	public void initializeArchiveRecordProcessorThreads() {
		
		HrwaManager.writeToLog("Starting processor threads...", true, HrwaManager.LOG_TYPE_STANDARD);
		
		for(int i = 0; i < HrwaManager.maxUsableProcessors; i++) {
			//Create thread
			archiveRecordProcessorRunnables[i] = new ArchiveFileProcessorRunnable(i);
			
			//And submit it to the fixedThreadPoolExecutorService so that it will run.
			//The submit method will return a Future that we can use to check the runnable's
			//status (isDone()) or cancel the task (cancel()).
			archiveRecordProcessorFutures[i] = fixedThreadPoolExecutorService.submit(archiveRecordProcessorRunnables[i]);
		}
		
		HrwaManager.writeToLog("All " + HrwaManager.maxUsableProcessors + " processors started!", true, HrwaManager.LOG_TYPE_STANDARD);
	}
	
	public void shutDownArchiveRecordProcessorThreads() {
		
		for(ArchiveFileProcessorRunnable singleRunnable : archiveRecordProcessorRunnables) {
			//Shut down each of the runnables
			singleRunnable.stop();
			System.out.println("STOPPING RUNNABLE!");
		}
		
		//Shut down the executor service
		
		fixedThreadPoolExecutorService.shutdown();
	    try {
			fixedThreadPoolExecutorService.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		fixedThreadPoolExecutorService.shutdownNow();
		
		HrwaManager.writeToLog("All " + HrwaManager.maxUsableProcessors + " threads have been shut down.", true, HrwaManager.LOG_TYPE_STANDARD);
	}
	
	////////////////////////////////
	/* Archive File List Creation */
	////////////////////////////////
	
	/**
	 * Recursively scans a directory, collecting Files that are matched by the passed fileExtensionFilter. Returns the resulting File[],
	 * with files alphabetically sorted by their full paths. 
	 * @param String dir The directory to recursively search through.
	 * @param String[] fileExtensionFilter File extensions to include in the search. All unspecified file extensions will be excluded.
	 * @return
	 */
	public static File[] getAlphabeticallySortedRecursiveListOfFilesFromArchiveDirectory(String pathToArchiveDirectory, String[] fileExtensionFilter) {

		File directory = new File(pathToArchiveDirectory);

		if (! directory.exists()) {
			HrwaManager.writeToLog("Error: achiveFileDir path " + directory.toString() + " does not exist.", true, HrwaManager.LOG_TYPE_ERROR);
			System.exit(HrwaManager.EXIT_CODE_ERROR);
		}
		if (! directory.isDirectory()) {
			HrwaManager.writeToLog("Error: achiveFileDir path " + directory.toString() + " is not a directory.", true, HrwaManager.LOG_TYPE_ERROR);
			System.exit(HrwaManager.EXIT_CODE_ERROR);
		}
		if (! directory.canRead()) {
			HrwaManager.writeToLog("Error: achiveFileDir path " + directory.toString() + " is not readable.", true, HrwaManager.LOG_TYPE_ERROR);
			System.exit(HrwaManager.EXIT_CODE_ERROR);
		}

		// Get file iterator that will recurse subdirectories in
		// archiveDirectory, only grabbing files with valid archive file
		// extensions
		// Note: FileUtils.iterateFiles returns a basic Iterator, and NOT a
		// generic Iterator. That's unfortunate (because I like Generics), but
		// I'll have to work with it anyway.
		Iterator<File> fileIterator = FileUtils.iterateFiles(
			directory,
			fileExtensionFilter,
			true
		);

		//Generate an alphabetically-ordered list of all the files that we'll be using
		ArrayList<File> fileList = new ArrayList<File>();
		while(fileIterator.hasNext())
		{
			fileList.add(fileIterator.next());
		}

		sortFileList(fileList);

		return fileList.toArray(new File[fileList.size()]);
	}
	
	public static void sortFileList(ArrayList<File> fileListToSort) {

		Collections.sort(fileListToSort, new Comparator<File>(){
			
			public int compare(File f1, File f2)
		    {
		        return (f1.getPath()).compareTo(f2.getPath());
		    }

		});

	}
	
	
}
