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
import java.util.concurrent.ConcurrentLinkedQueue;

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
	
	private int uniqueRunnableId;
	private long numArchiveRecordsIndexedIntoSolr = 0;
	private Boolean isProcessingAMySQLQuery = false;
	
	private ConcurrentLinkedQueue<Long> sharedReferenceToConcurrentLinkedQueueOfMySQLRecordOffsets;
	
	public MySQLArchiveRecordToSolrProcessorRunnable(int uniqueNumericId, ConcurrentLinkedQueue<Long> concurrentLinkedQueueOfMySQLRecordOffsets) {
		//Assign uniqueNumericId
		uniqueRunnableId = uniqueNumericId;
		
		//Pass reference to shared concurrentLinkedQueueOfArchiveFiles
		sharedReferenceToConcurrentLinkedQueueOfMySQLRecordOffsets = concurrentLinkedQueueOfMySQLRecordOffsets;
	}
	
	public int getUniqueRunnableId() {
		return this.uniqueRunnableId;
	}

	public void run() {
		HrwaManager.writeToLog("Thread " + getUniqueRunnableId() + " has started!", true, HrwaManager.LOG_TYPE_STANDARD);
		
		Long latestMySQLRecordOffsetToProcess;
		
		while((latestMySQLRecordOffsetToProcess = sharedReferenceToConcurrentLinkedQueueOfMySQLRecordOffsets.poll()) != null) {
			
			try {
				
				while(HrwaManager.getCurrentAppMemoryUsageInBytes() > HrwaManager.maxMemoryThresholdInBytesForStartingNewThreadProcesses) {
					
					//If current memory usage is too high, wait until it's lower before processing another MySQL batch on this thread
					try {
						Thread.sleep(5000);
						System.out.println("Thread " + this.uniqueRunnableId + ": sleeping for 5 seconds because current memory usage is too high to safely start an additional simutaneous MySQL batch process.  Current memory usage: " + HrwaManager.bytesToMegabytes(HrwaManager.getCurrentAppMemoryUsageInBytes()) + " MB");
					}
					catch (InterruptedException e) { e.printStackTrace(); }
				}
				
				if( ! HrwaManager.previewMode ) {
					processMySQLArchiveRecordQueryAndSendToSolr(latestMySQLRecordOffsetToProcess);
				} else {
					HrwaManager.writeToLog("PREVIEWING the Solr indexing of the results from the MySQL batch (" + latestMySQLRecordOffsetToProcess + " - " + (latestMySQLRecordOffsetToProcess+HrwaManager.mySQLToSolrRowRetrievalSize-1) + "). No actual Solr changes will be made.", true, HrwaManager.LOG_TYPE_NOTICE);
				}
				
			} catch (Exception ex) {
				//Catching any random uncaught exception related to this file so that one bad file doesn't crash this thread
				HrwaManager.writeToLog("An unexpected error occurred while processing the MySQL batch [" + latestMySQLRecordOffsetToProcess + " - " + (latestMySQLRecordOffsetToProcess+HrwaManager.mySQLToSolrRowRetrievalSize-1)  + "].  Moving onto the next batch.", true, HrwaManager.LOG_TYPE_ERROR);
			}
			
		}
		
		HrwaManager.writeToLog("Thread " + getUniqueRunnableId() + " has stopped!", true, HrwaManager.LOG_TYPE_STANDARD);
	}
	
	public void processMySQLArchiveRecordQueryAndSendToSolr(long startingRecordIdForMySQLArchiveRecordRowsBeingProcessed) {
		
		HrwaManager.writeToLog("Thread " + this.getUniqueRunnableId() + ": Start process of results from MySQL batch: (" + startingRecordIdForMySQLArchiveRecordRowsBeingProcessed + " - " + (startingRecordIdForMySQLArchiveRecordRowsBeingProcessed+HrwaManager.mySQLToSolrRowRetrievalSize-1) + ")", true, HrwaManager.LOG_TYPE_STANDARD);
		
		try {
			Connection conn = MySQLHelper.getNewDBConnection(true);
			
			PreparedStatement pstmt1 = conn.prepareStatement(
				"SELECT " +
				HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + ".id as id, archived_url, record_date, digest, archive_file, length, url, " +
				HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + ".mimetype_detected as mimetype_detected, blob_path, " +
				"mimetype_code, reader_identifier, record_identifier, status_code, " +
				"original_urls, bib_key, creator_name, " + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + ".hoststring as hoststring, organization_type, organization_based_in, " +
				"geographic_focus, language " +
				" FROM " + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + 
				" INNER JOIN " + HrwaManager.MYSQL_SITES_TABLE_NAME + " ON " + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + ".site_id = " + HrwaManager.MYSQL_SITES_TABLE_NAME + ".id " +
				" INNER JOIN " + HrwaManager.MYSQL_MIMETYPE_CODES_TABLE_NAME + " ON " + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + ".mimetype_detected =  " + HrwaManager.MYSQL_MIMETYPE_CODES_TABLE_NAME + ".mimetype_detected" +
				" WHERE" +
				" " + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + ".id >= " + startingRecordIdForMySQLArchiveRecordRowsBeingProcessed +
				" AND " + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + ".id < " + (startingRecordIdForMySQLArchiveRecordRowsBeingProcessed + HrwaManager.mySQLToSolrRowRetrievalSize) +
				" AND " + HrwaManager.MYSQL_MIMETYPE_CODES_TABLE_NAME + ".mimetype_code IN " + HrwaManager.DESIRED_SOLR_INDEXED_MIMETYPE_CODES_STRING_FOR_MYSQL_WHERE_CLAUSE_LIST +
				" AND " + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + "." + MySQLHelper.HRWA_MANAGER_TODO_FIELD_NAME + " = '" + MySQLHelper.HRWA_MANAGER_TODO_UPDATED + "'"
			);
			ResultSet resultSet = pstmt1.executeQuery();
			
			indexArchiveRecordMySQLResultSetToSolr(resultSet, startingRecordIdForMySQLArchiveRecordRowsBeingProcessed);
			
			resultSet.close();
			pstmt1.close();
	        conn.close();
        
		} catch (SQLException e) {
			HrwaManager.writeToLog("An error occurred while attempting to retrieve mysql archive record data from the web archive recods table. Query batch: (" + startingRecordIdForMySQLArchiveRecordRowsBeingProcessed + " - " + (startingRecordIdForMySQLArchiveRecordRowsBeingProcessed+HrwaManager.mySQLToSolrRowRetrievalSize-1) + ")", true, HrwaManager.LOG_TYPE_ERROR);
			e.printStackTrace();
			System.exit(0);
		}
		
		HrwaManager.writeToLog("Thread " + this.getUniqueRunnableId() + ": Processing of results from MySQL query batch (" + startingRecordIdForMySQLArchiveRecordRowsBeingProcessed + " - " + (startingRecordIdForMySQLArchiveRecordRowsBeingProcessed+HrwaManager.mySQLToSolrRowRetrievalSize-1) + ") COMPLETE!", true, HrwaManager.LOG_TYPE_STANDARD);
		
	}
	
	public long getNumArchiveRecordsIndexedIntoSolr() {
		return this.numArchiveRecordsIndexedIntoSolr;
	}
	
	public void indexArchiveRecordMySQLResultSetToSolr(ResultSet resultSet, long startingRecordIdForMySQLArchiveRecordRowsBeingProcessed) throws SQLException {
		
		ModifiableSolrParams modifiableSolrParams;
		
		while (resultSet.next()) {
				
				ASFSolrIndexer.indexDocAndExtractMetadataToSolr(resultSet);
				
				numArchiveRecordsIndexedIntoSolr++;
				
				if(HrwaManager.verbose) {
					System.out.println("Thread " + this.getUniqueRunnableId() + ": Num records indexed into Solr: " + numArchiveRecordsIndexedIntoSolr);
				}
			
		}
		
		System.gc(); //Must garbage collect to make sure that closing file handles close themselves quickly enough.
		
		HrwaManager.writeToLog(HrwaManager.getCurrentAppMemoryUsageMessage(), true, HrwaManager.LOG_TYPE_MEMORY);
		
		if(ASFSolrIndexer.commit()) {
			// // If everything committed successfully, mark this set of MySQL
			// records as having been updated (i.e. set hrwa_manager_todo to
			// NULL for these rows)
			
			try {
				Connection conn = MySQLHelper.getNewDBConnection(true);
				
				String query = "UPDATE " + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + 
						" INNER JOIN " + HrwaManager.MYSQL_SITES_TABLE_NAME + " ON " + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + ".site_id = " + HrwaManager.MYSQL_SITES_TABLE_NAME + ".id " +
						" INNER JOIN " + HrwaManager.MYSQL_MIMETYPE_CODES_TABLE_NAME + " ON " + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + ".mimetype_detected =  " + HrwaManager.MYSQL_MIMETYPE_CODES_TABLE_NAME + ".mimetype_detected" +
						" SET " + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + "." + MySQLHelper.HRWA_MANAGER_TODO_FIELD_NAME + " = NULL" + 
						" WHERE" +
						" " + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + ".id >= " + startingRecordIdForMySQLArchiveRecordRowsBeingProcessed +
						" AND " + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + ".id < " + (startingRecordIdForMySQLArchiveRecordRowsBeingProcessed + HrwaManager.mySQLToSolrRowRetrievalSize) +
						" AND " + HrwaManager.MYSQL_MIMETYPE_CODES_TABLE_NAME + ".mimetype_code IN " + HrwaManager.DESIRED_SOLR_INDEXED_MIMETYPE_CODES_STRING_FOR_MYSQL_WHERE_CLAUSE_LIST +
						" AND " + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + "." + MySQLHelper.HRWA_MANAGER_TODO_FIELD_NAME + " = '" + MySQLHelper.HRWA_MANAGER_TODO_UPDATED + "'"; 
				
				System.out.println(query);
				
				PreparedStatement pstmt1 = conn.prepareStatement(query);
				
				pstmt1.execute();
				
				pstmt1.close();
				//conn.commit(); No need to call commit because auto-commit == true
		        conn.close();
	        
			} catch (SQLException e) {
				HrwaManager.writeToLog("An error occurred while attempting to reset the values of recently UPDATED mysql rows in the web archive recods table. Query batch: (" + startingRecordIdForMySQLArchiveRecordRowsBeingProcessed + " - " + (startingRecordIdForMySQLArchiveRecordRowsBeingProcessed+HrwaManager.mySQLToSolrRowRetrievalSize-1) + ")", true, HrwaManager.LOG_TYPE_ERROR);
				e.printStackTrace();
				System.exit(0);
			}
		}
	}
	
}
