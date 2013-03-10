package edu.columbia.ldpd.hrwa.processorrunnables;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;
import org.archive.nutchwax.tools.ArcReader;

import com.googlecode.mp4parser.h264.model.HRDParameters;

import edu.columbia.ldpd.hrwa.HrwaManager;
import edu.columbia.ldpd.hrwa.MimetypeDetector;
import edu.columbia.ldpd.hrwa.mysql.MySQLHelper;
import edu.columbia.ldpd.hrwa.tasks.ArchiveToMySQLTask;
import edu.columbia.ldpd.hrwa.util.common.MetadataUtils;

public class MySQLArchiveRecordToSolrProcessorRunnable implements Runnable {
	
	private static final String ARCHIVED_URL_PREFIX = "http://wayback.archive-it.org/1068/";
	
	private int uniqueRunnableId;
	private boolean running = true;
	private String currentMySQLArchiveRecordQueryBeingProcessed = null;
	private long numArchiveRecordsIndexedIntoSolr = 0;
	private Boolean isProcessingAMySQLQuery = false;
	
	public MySQLArchiveRecordToSolrProcessorRunnable(int uniqueNumericId) {
		//Assign uniqueNumericId
		uniqueRunnableId = uniqueNumericId;
	}
	
	public int getUniqueRunnableId() {
		return this.uniqueRunnableId;
	}

	public void run() {
		while(true) {
			
			if( ! running ) {
				break;
			}
			
			if(currentMySQLArchiveRecordQueryBeingProcessed != null) {
				
				if( ! HrwaManager.previewMode ) {
					processMySQLArchiveRecordQueryAndSendToSolr(currentMySQLArchiveRecordQueryBeingProcessed);
				} else {
					HrwaManager.writeToLog("PREVIEWING the Solr indexing of the results from the MySQL query (" + currentMySQLArchiveRecordQueryBeingProcessed + "). No actual Solr changes will be made.", true, HrwaManager.LOG_TYPE_NOTICE);
				}
				
				//done processing!
				synchronized (isProcessingAMySQLQuery) {
					currentMySQLArchiveRecordQueryBeingProcessed = null;
					isProcessingAMySQLQuery = false;
				}
				
			} else {
				//Sleep when not actively processing anything
				try { Thread.sleep(5); }
				catch (InterruptedException e) { e.printStackTrace(); }
			}
			
		}
		
		System.out.println("Thread " + getUniqueRunnableId() + " complete!");
	}
	
	public void processMySQLArchiveRecordQueryAndSendToSolr(String archiveRecordSelectQuery) {
		
		HrwaManager.writeToLog("Thread " + this.getUniqueRunnableId() + ": Start process of results from MySQL query" + archiveRecordSelectQuery, true, HrwaManager.LOG_TYPE_STANDARD);
		
		try {
			Connection conn = MySQLHelper.getNewDBConnection(true);
			
			PreparedStatement pstmt1 = conn.prepareStatement(archiveRecordSelectQuery);
			ResultSet resultSet = pstmt1.executeQuery();
			
			indexArchiveRecordMySQLResultSetToSolr(resultSet);
			
			resultSet.close();
			pstmt1.close();
	        conn.close();
        
		} catch (SQLException e) {
			HrwaManager.writeToLog("An error occurred while attempting to retrieve the max id from the web archive records table.", true, HrwaManager.LOG_TYPE_ERROR);
			e.printStackTrace();
			System.exit(0);
		}
		
		HrwaManager.writeToLog("Thread " + this.getUniqueRunnableId() + ": Processing of results from MySQL query " + archiveRecordSelectQuery + " COMPLETE!", true, HrwaManager.LOG_TYPE_STANDARD);
		
	}
	
	public long getNumArchiveRecordsIndexedIntoSolr() {
		return this.numArchiveRecordsIndexedIntoSolr;
	}
	
	public void stop() {
		System.out.println("STOP was called on Thread " + getUniqueRunnableId());
		running = false;
	}
	
	public boolean isProcessingAMySQLArchiveRecordQuery() {
		
		boolean bool;
		
		synchronized (isProcessingAMySQLQuery) {
			bool = isProcessingAMySQLQuery;
		}
		
		return bool;
	}

	/**
	 * Sends the archiveRecordSelectQuery off to be processed during this runnable's run() loop.
	 * How does this work?  The passed query is assigned to this.currentArcRecordBeingProcessed.
	 * This function returns almost immediately.  Actual processing happens asynchronously.
	 * @param archiveFile
	 */
	public void queueMySQLArchiveRecordQueryForProcessing(String archiveRecordSelectQuery) {
		
		HrwaManager.writeToLog("Notice: MySQL query claimed by MySQLArchiveRecordToSolrProcessorRunnable " + this.getUniqueRunnableId() + " (" + archiveRecordSelectQuery + ")", true, HrwaManager.LOG_TYPE_NOTICE);
		
		if(isProcessingAMySQLQuery) {
			HrwaManager.writeToLog("Error: MySQLArchiveRecordToSolrProcessorRunnable with id " + this.uniqueRunnableId + " cannot accept a new MySQL query to process because isProcessingAMySQLQuery == true. This error should never appear if things were coded properly.", true, HrwaManager.LOG_TYPE_ERROR);
		} else {
			synchronized (isProcessingAMySQLQuery) {
				currentMySQLArchiveRecordQueryBeingProcessed = archiveRecordSelectQuery;
				isProcessingAMySQLQuery = true;
			}
			//System.out.println("Thread " + this.uniqueRunnableId + ": Just started processing.");
		}
	}
	
	public void indexArchiveRecordMySQLResultSetToSolr(ResultSet resultSet) throws SQLException {
		
		//TODO: This method should actually index stuff into Solr!
		
		while (resultSet.next()) {
			numArchiveRecordsIndexedIntoSolr++;
		}
	}
	
}
