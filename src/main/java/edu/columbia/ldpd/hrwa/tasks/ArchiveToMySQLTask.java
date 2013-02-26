package edu.columbia.ldpd.hrwa.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

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

import edu.columbia.ldpd.hrwa.ArchiveRecordProcessorRunnable;
import edu.columbia.ldpd.hrwa.HrwaManager;

public class ArchiveToMySQLTask extends HrwaTask {
	
	//How many threads will we create?  HrwaManager.maxUsableProcessors
	private ArchiveRecordProcessorRunnable[] archiveRecordProcessorRunnables;
	Future[] archiveRecordProcessorFutures;
	
	ExecutorService fixedThreadPoolExecutorService;

	public ArchiveToMySQLTask() {
		archiveRecordProcessorRunnables = new ArchiveRecordProcessorRunnable[HrwaManager.maxUsableProcessors];
		archiveRecordProcessorFutures = new Future[HrwaManager.maxUsableProcessors];
		fixedThreadPoolExecutorService = Executors.newFixedThreadPool(HrwaManager.maxUsableProcessors);
	}
	
	public void runTask() {
		
		writeTaskHeaderMessageAndSetStartTime();
		
		initializeArchiveRecordProcessorThreads();
		
		
		
		//Do stuff
		try { Thread.sleep(2000); }
		catch (InterruptedException e) { e.printStackTrace(); }
		
		
		
		//Wait until all of the runnables are done processing.
		HrwaManager.writeToLog("Preparing to shut down processors...", true, HrwaManager.LOG_TYPE_STANDARD);
		HrwaManager.writeToLog("Allowing processors to complete their final tasks...", true, HrwaManager.LOG_TYPE_STANDARD);
		while( someArchiveRecordProcessorsAreStillRunning() ) {
			try {
				System.out.println("Waiting for task completion before shut down...");
				Thread.sleep(1000);
			}
			catch (InterruptedException e) { e.printStackTrace(); }
		}
		
		HrwaManager.writeToLog("All ArchiveRecordProcessorRunnables have finished processing!  Shutting down all " + HrwaManager.maxUsableProcessors + " processor threads.", true, HrwaManager.LOG_TYPE_STANDARD);
		
		shutDownArchiveRecordProcessorThreads();
		
		writeTaskFooterMessageAndPrintTotalTime();
		
	}
	
	
	/**
	 * Returns true if at least one of the processors is still running.
	 * @return
	 */
	public boolean someArchiveRecordProcessorsAreStillRunning() {
		for(ArchiveRecordProcessorRunnable singleProcessor : archiveRecordProcessorRunnables) {
			if(singleProcessor.isProcessingARecord()) {
				return true;
			}
		}
		return false;
	}
	
	public void initializeArchiveRecordProcessorThreads() {
		
		for(int i = 0; i < HrwaManager.maxUsableProcessors; i++) {
			//Create thread
			archiveRecordProcessorRunnables[i] = new ArchiveRecordProcessorRunnable(i);
			
			//And submit it to the fixedThreadPoolExecutorService so that it will run.
			//The submit method will return a Future that we can use to check the runnable's
			//status (isDone()) or cancel the task (cancel()).
			archiveRecordProcessorFutures[i] = fixedThreadPoolExecutorService.submit(archiveRecordProcessorRunnables[i]);
		}
		
	}
	
	public void shutDownArchiveRecordProcessorThreads() {
		
		for(int i = 0; i < HrwaManager.maxUsableProcessors; i++) {
			//Shut down each of the runnables
			archiveRecordProcessorRunnables[i].stop();
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
	
}
