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
import edu.columbia.ldpd.hrwa.processorrunnables.MySQLArchiveRecordToSolrProcessorRunnable;

public class MySQLArchiveRecordsToSolrTask extends HrwaTask {
	
	private String[] validArchiveFileExtensions = {"arc.gz", "warc.gz"};
	
	//How many threads will we create?  HrwaManager.maxUsableProcessors
	private MySQLArchiveRecordToSolrProcessorRunnable[] mySQLArchiveRecordToSolrProcessorRunnables;
	Future[] mySQLArchiveRecordToSolrProcessorFutures;
	ExecutorService fixedThreadPoolExecutorService;

	public MySQLArchiveRecordsToSolrTask() {
		mySQLArchiveRecordToSolrProcessorRunnables = new MySQLArchiveRecordToSolrProcessorRunnable[HrwaManager.maxUsableProcessors];
		mySQLArchiveRecordToSolrProcessorFutures = new Future[HrwaManager.maxUsableProcessors];
		fixedThreadPoolExecutorService = Executors.newFixedThreadPool(HrwaManager.maxUsableProcessors);
	}
	
	public void runTask() {
		
		writeTaskHeaderMessageAndSetStartTime();
		
		initializeMySQLArchiveRecordToSolrProcessorThreads();
		
		//Get the highest id in the web archive records table
		//We'll loop until we hit that number
		
		HrwaManager.writeToLog("Retrieving max(id) from MySQL web archive records table...", true, HrwaManager.LOG_TYPE_STANDARD);
		long maxWebArchiveRecordMySQLId = MySQLHelper.getMaxIdFromWebArchiveRecordsTable();
		HrwaManager.writeToLog("Max(id) from MySQL web archive records table: " + maxWebArchiveRecordMySQLId, true, HrwaManager.LOG_TYPE_STANDARD);
		
		for(long currentRecordRetrievalOffset = 0; currentRecordRetrievalOffset < maxWebArchiveRecordMySQLId; currentRecordRetrievalOffset += HrwaManager.mySQLToSolrRowRetrievalSize) {
			indexMySQLArchiveRecordsIntoSolr(
				"SELECT " +
				"archived_url, record_date, digest, archive_file, length, url, " +
				HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + ".mimetype_detected, " +
				"mimetype_code, reader_identifier, record_identifier, status_code, " +
				"bib_key, creator_name, " + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + ".hoststring, organization_type, organization_based_in, " +
				"geographic_focus, language " +
				" FROM " + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + 
				" INNER JOIN " + HrwaManager.MYSQL_SITES_TABLE_NAME + " ON " + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + ".site_id = " + HrwaManager.MYSQL_SITES_TABLE_NAME + ".id " +
				" INNER JOIN " + HrwaManager.MYSQL_MIMETYPE_CODES_TABLE_NAME + " ON " + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + ".mimetype_detected =  " + HrwaManager.MYSQL_MIMETYPE_CODES_TABLE_NAME + ".mimetype_detected" +
				" WHERE" +
				" " + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + ".site_id IS NOT NULL" +
				" AND" +
				" " + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + ".id > " + currentRecordRetrievalOffset + " AND web_archive_records.id <= " + (currentRecordRetrievalOffset + HrwaManager.mySQLToSolrRowRetrievalSize)
			);
		}
		
		//CLEANUP TIME
		
		//Wait until all of the runnables are done processing.
		HrwaManager.writeToLog("Preparing to shut down processors...", true, HrwaManager.LOG_TYPE_STANDARD);
		HrwaManager.writeToLog("Allowing processors to complete their final tasks...", true, HrwaManager.LOG_TYPE_STANDARD);
		while( someMySQLArchiveRecordToSolrProcessorsAreStillRunning() ) {
			try {
				System.out.println("Waiting for final archive file processing to complete before shut down...");
				Thread.sleep(1000);
			}
			catch (InterruptedException e) { e.printStackTrace(); }
		}
		
		HrwaManager.writeToLog("All MySQLArchiveRecordToSolrProcessorRunnables have finished processing!  Shutting down all " + HrwaManager.maxUsableProcessors + " processor threads.", true, HrwaManager.LOG_TYPE_STANDARD);
		
		shutDownMySQLArchiveRecordToSolrProcessorThreads();
		
		HrwaManager.writeToLog("Total number of archive records processed: " + this.getTotalNumberOfRelevantArchiveRecordsProcessedAtThisExactMoment(), true, HrwaManager.LOG_TYPE_STANDARD);
		
		writeTaskFooterMessageAndPrintTotalTime();
		
	}
	
	/**
	 * Passes the given MySQL selectQuery to one of the available MySQLArchiveRecordToSolrProcessorRunnable worker threads.
	 * If no worker threads are currently available, this method waits until one is available.
	 * @param selectQuery
	 */
	public void indexMySQLArchiveRecordsIntoSolr(String selectQuery) {
		
		boolean lookingForAvailableProcessorThread = true;
		
		long currentMemoryUsage;
		while(lookingForAvailableProcessorThread) {
			
			currentMemoryUsage = HrwaManager.getCurrentAppMemoryUsageInBytes();
			
			if(currentMemoryUsage > HrwaManager.maxMemoryThresholdInBytesForStartingNewThreadProcesses) {
				
				//If current memory usage is too high, wait until it's lower before processing more MySQL records on another thread
				System.out.println("Memory usage is currently too high to concurrently start processing more MySQL records.  Waiting until usage is lower... (Currently: " + HrwaManager.bytesToMegabytes(currentMemoryUsage) + " MB)");
				try {
					Thread.sleep(100);
					//System.out.println("Sleeping for X ms because no threads are available for processing...");
				}
				catch (InterruptedException e) { e.printStackTrace(); }
				
			} else {
				
				//Otherwise process normally and wait until another thread is available do work
				
				for(MySQLArchiveRecordToSolrProcessorRunnable singleProcessorRunnable : mySQLArchiveRecordToSolrProcessorRunnables) {
					if( ! singleProcessorRunnable.isProcessingAMySQLArchiveRecordQuery() ) {
						lookingForAvailableProcessorThread = false;
						
						singleProcessorRunnable.queueMySQLArchiveRecordQueryForProcessing(selectQuery);
						// Uncomment the line below to perform single-threaded
						// debugging/processing (by directly calling the
						// processMySQLArchiveRecordQueryAndSendToSolr() method). If you do uncomment this
						// line, then you should comment out the line above (because
						// you no longer want to queue archive file processing).
						//singleProcessorRunnable.processMySQLArchiveRecordQueryAndSendToSolr(selectQuery);
						break;
					}
				}
				
				try {
					Thread.sleep(10);
					//System.out.println("Sleeping for X ms because no threads are available for processing...");
				}
				catch (InterruptedException e) { e.printStackTrace(); }
			}
			
		}
		
	}
	
	public long getTotalNumberOfRelevantArchiveRecordsProcessedAtThisExactMoment() {
		
		long total = 0;
		
		for(MySQLArchiveRecordToSolrProcessorRunnable singleProcessor : mySQLArchiveRecordToSolrProcessorRunnables) {
			total += singleProcessor.getNumArchiveRecordsIndexedIntoSolr();
		}
		
		return total;
	}
	
	/**
	 * Returns true if at least one of the processors is still running.
	 * @return
	 */
	public boolean someMySQLArchiveRecordToSolrProcessorsAreStillRunning() {
		for(MySQLArchiveRecordToSolrProcessorRunnable singleProcessor : mySQLArchiveRecordToSolrProcessorRunnables) {
			if(singleProcessor.isProcessingAMySQLArchiveRecordQuery()) {
				System.out.println("Thread " + singleProcessor.getUniqueRunnableId() + " is still running. --> " + singleProcessor.getNumArchiveRecordsIndexedIntoSolr());
				return true;
			}
		}
		return false;
	}
	
	public void initializeMySQLArchiveRecordToSolrProcessorThreads() {
		
		HrwaManager.writeToLog("Starting processor threads...", true, HrwaManager.LOG_TYPE_STANDARD);
		
		for(int i = 0; i < HrwaManager.maxUsableProcessors; i++) {
			//Create thread
			mySQLArchiveRecordToSolrProcessorRunnables[i] = new MySQLArchiveRecordToSolrProcessorRunnable(i);
			
			//And submit it to the fixedThreadPoolExecutorService so that it will run.
			//The submit method will return a Future that we can use to check the runnable's
			//status (isDone()) or cancel the task (cancel()).
			mySQLArchiveRecordToSolrProcessorFutures[i] = fixedThreadPoolExecutorService.submit(mySQLArchiveRecordToSolrProcessorRunnables[i]);
		}
		
		HrwaManager.writeToLog("All " + HrwaManager.maxUsableProcessors + " processors started!", true, HrwaManager.LOG_TYPE_STANDARD);
	}
	
	public void shutDownMySQLArchiveRecordToSolrProcessorThreads() {
		
		for(MySQLArchiveRecordToSolrProcessorRunnable singleRunnable : mySQLArchiveRecordToSolrProcessorRunnables) {
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
