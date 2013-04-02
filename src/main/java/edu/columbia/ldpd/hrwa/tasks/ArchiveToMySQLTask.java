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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
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
	private ArrayList<ArchiveFileProcessorRunnable> archiveRecordProcessorRunnables;
	private ArrayList<Future<ArchiveFileProcessorRunnable>> archiveRecordProcessorFutures;
	private ExecutorService fixedThreadPoolExecutorService;

	public ArchiveToMySQLTask() {
		archiveRecordProcessorRunnables = new ArrayList<ArchiveFileProcessorRunnable>(HrwaManager.maxUsableProcessors);
		archiveRecordProcessorFutures = new ArrayList<Future<ArchiveFileProcessorRunnable>>(HrwaManager.maxUsableProcessors);
		fixedThreadPoolExecutorService = Executors.newFixedThreadPool(HrwaManager.maxUsableProcessors);
	}
	
	public void runTask() {
		
		writeTaskHeaderMessageAndSetStartTime();
		
		//Scan through warc file directory and generate an alphabetically-sorted queue of all warc files to index
		ArrayList<File> listOfArchiveFiles = getAlphabeticallySortedRecursiveListOfFilesFromArchiveDirectory(HrwaManager.archiveFileDirPath, validArchiveFileExtensions);
		
		int numberOfArchiveFilesToProcess = listOfArchiveFiles.size();
		
		if(numberOfArchiveFilesToProcess < 1) {
			HrwaManager.writeToLog("No files found for indexing in directory: " + HrwaManager.archiveFileDirPath, true, HrwaManager.LOG_TYPE_ERROR);
			System.exit(HrwaManager.EXIT_CODE_ERROR);
		}
		
		HrwaManager.writeToLog("Number of archive files to process: " + numberOfArchiveFilesToProcess, true, HrwaManager.LOG_TYPE_STANDARD);
		
		//Create necessary MySQL tables
		try {
			MySQLHelper.createMimetypeCodesTableIfItDoesNotExist();
			MySQLHelper.createWebArchiveRecordsTableIfItDoesNotExist();
			MySQLHelper.createFullyIndexedArchiveFilesTableIfItDoesNotExist();
			
			//And make sure that the sites and related_hosts tables exist!
			if(MySQLHelper.getSitesMap(null).size() < 1) {
				HrwaManager.writeToLog("Error: Could not find sites table in database.", true, HrwaManager.LOG_TYPE_ERROR);
				System.exit(HrwaManager.EXIT_CODE_ERROR);
			}
			
			//And make sure that the sites and related_hosts tables exist!
			if(MySQLHelper.getRelatedHostsMap(null).size() < 1) {
				HrwaManager.writeToLog("Error: Could not find related hosts table in database.", true, HrwaManager.LOG_TYPE_ERROR);
				System.exit(HrwaManager.EXIT_CODE_ERROR);
			}
			
		} catch (SQLException e1) {
			HrwaManager.writeToLog("Error: Could not create one of the required MySQL tables.", true, HrwaManager.LOG_TYPE_ERROR);
		}
		
		//Now we'll turn this list into a ConcurrentLinkedQueue<File>.  It's thread-safe!  Nice!  
		ConcurrentLinkedQueue<File> concurrentLinkedQueueOfArchiveFiles = new ConcurrentLinkedQueue<File>(listOfArchiveFiles);
		
		
		initializeArchiveRecordProcessorThreads(concurrentLinkedQueueOfArchiveFiles);
		
		
		//Have this main thread wait around until all processors are done completing all tasks
		//Poll the processor every once in a while
		while(someArchiveRecordProcessorsAreStillRunning()) {
			
			try {
				Thread.sleep(5000);
			}
			catch (InterruptedException e) { e.printStackTrace(); }
			
			System.out.println("Total number of archive records processed: " + this.getTotalNumberOfRelevantArchiveRecordsProcessedAtThisExactMoment()); //This doesn't need to be logged.
			System.out.println(HrwaManager.getCurrentAppRunTime()); //This doesn't need to be logged.
			HrwaManager.writeToLog(HrwaManager.getCurrentAppMemoryUsageMessage(), true, HrwaManager.LOG_TYPE_MEMORY); //This doesn't need to be logged.
			
		}
		
		//All of the processor threads have completed!
		checkForAndLogAnyChildThreadArchiveRecordProcessorsExceptions();
		
		//Now we need to shut down the thread executor service
		shutDownThreadExecutorService();
		
		HrwaManager.writeToLog("Total number of archive records processed: " + this.getTotalNumberOfRelevantArchiveRecordsProcessedAtThisExactMoment(), true, HrwaManager.LOG_TYPE_STANDARD);
		System.out.println(HrwaManager.getCurrentAppRunTime()); //This doesn't need to be logged.
		
		writeTaskFooterMessageAndPrintTotalTime();
		
	}
	
	public long getTotalNumberOfRelevantArchiveRecordsProcessedAtThisExactMoment() {
		
		long total = 0;
		
		for(ArchiveFileProcessorRunnable singleProcessor : archiveRecordProcessorRunnables) {
			total += singleProcessor.getNumRelevantArchiveRecordsProcessed();
		}
		
		return total;
	}
	
	/**
	 * This method is good to run at the end of the program, once all threads have completed execution.
	 * It seems that uncaught exceptions in child threads aren't necessarily propagated to the parent thread,
	 * so this will at least let us know if we ran into any exceptions. 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void checkForAndLogAnyChildThreadArchiveRecordProcessorsExceptions() {
		
		for(int i = 0; i < archiveRecordProcessorFutures.size(); i++) {
			try {
				archiveRecordProcessorFutures.get(i).get(); //.get() method will throw any uncaught exceptions from this thread
			} catch (Exception e) {
				HrwaManager.writeToLog("During the final child thread exception check, an uncaught exception was found on thread " + i + ": " + e.getMessage(),  true, HrwaManager.LOG_TYPE_ERROR);
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Returns true if at least one of the processors is still running.
	 * @return
	 */
	public boolean someArchiveRecordProcessorsAreStillRunning() {
		
		for(Future<ArchiveFileProcessorRunnable> singleArchiveRecordProcessorFuture : archiveRecordProcessorFutures) {
			if( ! singleArchiveRecordProcessorFuture.isDone() ) {
				return true;
			}
		}
		
		return false;
	}
	
	public void initializeArchiveRecordProcessorThreads(ConcurrentLinkedQueue<File> concurrentLinkedQueueOfArchiveFiles) {
		
		HrwaManager.writeToLog("Starting processor threads...", true, HrwaManager.LOG_TYPE_STANDARD);
		
		for(int i = 0; i < HrwaManager.maxUsableProcessors; i++) {
			//Create thread
			archiveRecordProcessorRunnables.add(i, new ArchiveFileProcessorRunnable(i, concurrentLinkedQueueOfArchiveFiles));
			
			//And submit it to the fixedThreadPoolExecutorService so that it will run.
			//The submit method will return a Future that we can use to check the runnable's
			//status (isDone()) or cancel the task (cancel()).
			 
			archiveRecordProcessorFutures.add(i, (Future<ArchiveFileProcessorRunnable>)fixedThreadPoolExecutorService.submit(archiveRecordProcessorRunnables.get(i)));
		}
		
		HrwaManager.writeToLog("All " + HrwaManager.maxUsableProcessors + " processors started!", true, HrwaManager.LOG_TYPE_STANDARD);
	}
	
	////////////////////////////////
	/* Archive File List Creation */
	////////////////////////////////
	
	/**
	 * Recursively scans a directory, collecting Files that are matched by the passed fileExtensionFilter. Returns the resulting ArrayList<File>,
	 * with files in the queue alphabetically sorted by their full paths.
	 * @param String dir The directory to recursively search through.
	 * @param String[] fileExtensionFilter File extensions to include in the search. All unspecified file extensions will be excluded.
	 * @return
	 */
	public static ArrayList<File> getAlphabeticallySortedRecursiveListOfFilesFromArchiveDirectory(String pathToArchiveDirectory, String[] fileExtensionFilter) {

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
		
		//And finally, turn the list into a queue
		ConcurrentLinkedQueue<File> queueToReturn = new ConcurrentLinkedQueue<File>(fileList);
		
		return fileList;
	}
	
	public static void sortFileList(ArrayList<File> fileListToSort) {

		Collections.sort(fileListToSort, new Comparator<File>(){
			
			public int compare(File f1, File f2)
		    {
		        return (f1.getPath()).compareTo(f2.getPath());
		    }

		});

	}
	
	public void shutDownThreadExecutorService() {

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
	
}
