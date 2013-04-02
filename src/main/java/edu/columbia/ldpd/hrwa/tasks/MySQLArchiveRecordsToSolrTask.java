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
import edu.columbia.ldpd.hrwa.processorrunnables.MySQLArchiveRecordToSolrProcessorRunnable;
import edu.columbia.ldpd.hrwa.solr.ASFSolrIndexer;

public class MySQLArchiveRecordsToSolrTask extends HrwaTask {
	
	//How many threads will we create?  HrwaManager.maxUsableProcessors
	private ArrayList<MySQLArchiveRecordToSolrProcessorRunnable> mySQLArchiveRecordToSolrProcessorRunnables;
	private ArrayList<Future<MySQLArchiveRecordToSolrProcessorRunnable>> mySQLArchiveRecordToSolrProcessorFutures;
	private ExecutorService fixedThreadPoolExecutorService;

	public MySQLArchiveRecordsToSolrTask() {
		mySQLArchiveRecordToSolrProcessorRunnables = new ArrayList<MySQLArchiveRecordToSolrProcessorRunnable>(HrwaManager.maxUsableProcessors);
		mySQLArchiveRecordToSolrProcessorFutures = new ArrayList<Future<MySQLArchiveRecordToSolrProcessorRunnable>>(HrwaManager.maxUsableProcessors);
		fixedThreadPoolExecutorService = Executors.newFixedThreadPool(HrwaManager.maxUsableProcessors);
	}
	
	public void runTask() {
		
		writeTaskHeaderMessageAndSetStartTime();
		
		ASFSolrIndexer.initSingleSolrServerObject();
		
		//Get the highest id in the web archive records table
		HrwaManager.writeToLog("Retrieving max(id) from MySQL web archive records table...", true, HrwaManager.LOG_TYPE_STANDARD);
		long maxWebArchiveRecordMySQLId = MySQLHelper.getMaxIdFromWebArchiveRecordsTable();
		HrwaManager.writeToLog("Max(id) from MySQL web archive records table: " + maxWebArchiveRecordMySQLId, true, HrwaManager.LOG_TYPE_STANDARD);
		
		//Then generate the list of MySQL offsets that will be used my child threads as they loop through all MySQL archive records
		ConcurrentLinkedQueue<Long> concurrentLinkedQueueOfMySQLRecordOffsets = new ConcurrentLinkedQueue<Long>();
		
		for(long nextRecordRetrievalOffset = 0; nextRecordRetrievalOffset < maxWebArchiveRecordMySQLId; nextRecordRetrievalOffset += HrwaManager.mySQLToSolrRowRetrievalSize) {
			concurrentLinkedQueueOfMySQLRecordOffsets.add(nextRecordRetrievalOffset);
		}
		
		
		initializeMySQLArchiveRecordToSolrProcessorThreads(concurrentLinkedQueueOfMySQLRecordOffsets);
		
		
		//Have this main thread wait around until all processors are done completing all tasks
		//Poll the processor every once in a while
		while(someMySQLArchiveRecordToSolrProcessorsAreStillRunning()) {
			
			try {
				Thread.sleep(5000);
			}
			catch (InterruptedException e) { e.printStackTrace(); }
			
			System.out.println("Total number of MySQL archive records processed: " + this.getTotalNumberOfRelevantArchiveRecordsIndexedIntoSolrAtThisExactMoment()); //This doesn't need to be logged.
			System.out.println(HrwaManager.getCurrentAppRunTime()); //This doesn't need to be logged.
			HrwaManager.writeToLog(HrwaManager.getCurrentAppMemoryUsageMessage(), true, HrwaManager.LOG_TYPE_MEMORY); //This doesn't need to be logged.
			
		}
		
		//All of the processor threads have completed!
		checkForAndLogAnyChildThreadArchiveRecordProcessorsExceptions();
		
		//Now we need to shut down the thread executor service
		shutDownThreadExecutorService();
		
		HrwaManager.writeToLog("Total number of MySQL archive records processed: " + this.getTotalNumberOfRelevantArchiveRecordsIndexedIntoSolrAtThisExactMoment(), true, HrwaManager.LOG_TYPE_STANDARD);
		System.out.println(HrwaManager.getCurrentAppRunTime()); //This doesn't need to be logged.
		
		writeTaskFooterMessageAndPrintTotalTime();
		
	}
	
	public long getTotalNumberOfRelevantArchiveRecordsIndexedIntoSolrAtThisExactMoment() {
		
		long total = 0;
		
		for(MySQLArchiveRecordToSolrProcessorRunnable singleProcessor : mySQLArchiveRecordToSolrProcessorRunnables) {
			total += singleProcessor.getNumArchiveRecordsIndexedIntoSolr();
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
		
		for(int i = 0; i < mySQLArchiveRecordToSolrProcessorFutures.size(); i++) {
			try {
				mySQLArchiveRecordToSolrProcessorFutures.get(i).get(); //.get() method will throw any uncaught exceptions from this thread
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
	public boolean someMySQLArchiveRecordToSolrProcessorsAreStillRunning() {
		
		for(Future<MySQLArchiveRecordToSolrProcessorRunnable> singleMySQLArchiveRecordToSolrProcessorFuture : mySQLArchiveRecordToSolrProcessorFutures) {
			if( ! singleMySQLArchiveRecordToSolrProcessorFuture.isDone() ) {
				return true;
			}
		}
		
		return false;
	}
	
	public void initializeMySQLArchiveRecordToSolrProcessorThreads(ConcurrentLinkedQueue<Long> concurrentLinkedQueueOfMySQLRecordOffsets) {
		
		HrwaManager.writeToLog("Starting processor threads...", true, HrwaManager.LOG_TYPE_STANDARD);
		
		for(int i = 0; i < HrwaManager.maxUsableProcessors; i++) {
			//Create thread
			mySQLArchiveRecordToSolrProcessorRunnables.add(i, new MySQLArchiveRecordToSolrProcessorRunnable(i, concurrentLinkedQueueOfMySQLRecordOffsets));
			
			//And submit it to the fixedThreadPoolExecutorService so that it will run.
			//The submit method will return a Future that we can use to check the runnable's
			//status (isDone()) or cancel the task (cancel()).
			mySQLArchiveRecordToSolrProcessorFutures.add(i, (Future<MySQLArchiveRecordToSolrProcessorRunnable>)fixedThreadPoolExecutorService.submit(mySQLArchiveRecordToSolrProcessorRunnables.get(i)));
		}
		
		HrwaManager.writeToLog("All " + HrwaManager.maxUsableProcessors + " processors started!", true, HrwaManager.LOG_TYPE_STANDARD);
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
