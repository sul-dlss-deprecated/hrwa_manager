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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;
import org.archive.nutchwax.tools.ArcReader;

import com.googlecode.mp4parser.h264.model.HRDParameters;

import edu.columbia.ldpd.hrwa.HrwaManager;
import edu.columbia.ldpd.hrwa.MimetypeDetector;
import edu.columbia.ldpd.hrwa.mysql.MySQLHelper;
import edu.columbia.ldpd.hrwa.solr.ASFSolrIndexer;
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
		
		HrwaManager.writeToLog("Thread " + this.getUniqueRunnableId() + ": Start process of results from MySQL query: " + archiveRecordSelectQuery, true, HrwaManager.LOG_TYPE_STANDARD);
		
		try {
			Connection conn = MySQLHelper.getNewDBConnection(true);
			
			PreparedStatement pstmt1 = conn.prepareStatement(archiveRecordSelectQuery);
			ResultSet resultSet = pstmt1.executeQuery();
			
			indexArchiveRecordMySQLResultSetToSolr(resultSet);
			
			resultSet.close();
			pstmt1.close();
	        conn.close();
        
		} catch (SQLException e) {
			HrwaManager.writeToLog("An error occurred while attempting to retrieve mysql archive record data from the web archive recods table. Query: " + archiveRecordSelectQuery, true, HrwaManager.LOG_TYPE_ERROR);
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
		
		ModifiableSolrParams modifiableSolrParams;
		
		while (resultSet.next()) {
			
			try {
				
				ASFSolrIndexer.indexDocAndExtractMetadataToSolr(resultSet);
				
				numArchiveRecordsIndexedIntoSolr++;
				
				if(HrwaManager.verbose) {
					System.out.println("Thread " + this.getUniqueRunnableId() + ": Num records indexed into Solr: " + numArchiveRecordsIndexedIntoSolr);
				}
				
			} catch (SolrServerException e1) {
				HrwaManager.writeToLog("Error: SolrServerException encountered while attempting to index a document to the ASF Solr server.  Archive record table row id: " + resultSet.getInt("id"), true, HrwaManager.LOG_TYPE_ERROR);
				e1.printStackTrace();
			} catch (IOException e2) {
				HrwaManager.writeToLog("Error: IOException encountered while attempting to index a document to the ASF Solr server.  Archive record table row id: " + resultSet.getInt("id"), true, HrwaManager.LOG_TYPE_ERROR);
				e2.printStackTrace();
			} catch (Exception e3) {
				HrwaManager.writeToLog("Error: Unknown Exception encountered while attempting to index a document to the ASF Solr server.  Archive record table row id: " + resultSet.getInt("id") + "\n" +
				e3.getMessage(), true, HrwaManager.LOG_TYPE_ERROR);
			}
			
		}
		
		HrwaManager.writeToLog("Current memory usage: " + HrwaManager.getCurrentAppMemoryUsageString(), true, HrwaManager.LOG_TYPE_MEMORY);
		
		if(ASFSolrIndexer.commit()) {
			//TODO: If everything committed successfully, mark this set of MySQL records as having been updated (i.e. set hrwa_manager_todo to NULL for these rows) 
		}
	}
	
}
