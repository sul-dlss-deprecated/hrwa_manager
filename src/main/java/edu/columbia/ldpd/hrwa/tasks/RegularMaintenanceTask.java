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
import edu.columbia.ldpd.hrwa.solr.ASFSolrIndexer;

public class RegularMaintenanceTask extends HrwaTask {

	public RegularMaintenanceTask() {
		
	}
	
	public void runTask() {
		
		writeTaskHeaderMessageAndSetStartTime();
		
		long totalNumberOfWebArchiveRecordRowsUpdatedByThisTask = 0;
		
		//Update sites and related hosts tables to latest versions
		HrwaTask sitesToSolrAndMySQLTask = new SitesToSolrAndMySQLTask();
		sitesToSolrAndMySQLTask.runTask();
		
		//Update unlinked web archive records that should be linked to new sites (linked by sites table hoststring or related_hosts table entries)
		//Do this in batches of 1000 to avoid massive MySQL joins that can cause memory problems or major slowdowns
		
		long maxWebArchiveRecordMySQLId = MySQLHelper.getMaxIdFromWebArchiveRecordsTable();
		
		Connection conn = MySQLHelper.getNewDBConnection(true); //we want auto-commit to be on!
		PreparedStatement pstmt;
		ResultSet resultSet;
		long currentRecordRetrievalOffset;
		long rowsAffected;
		
		try {
			
			//First, check for conflicts between DELETED sites and related hosts that link to those sites
			//There shouldn't be any related hosts that point to deleted sites.  If there are, stop program and log error so that this issue can be resolved.
			pstmt = conn.prepareStatement(
					"SELECT related_host, sites.hoststring FROM related_hosts" +
					" INNER JOIN sites ON related_hosts.site_id = sites.id" +
					" WHERE" +
					" sites.hrwa_manager_todo = 'DELETED'"
			);
			
			resultSet = pstmt.executeQuery();
			String relatedHostDeletedSiteConflictMessage = "";
			while(resultSet.next()) {
				relatedHostDeletedSiteConflictMessage += "\n- Related host " + resultSet.getString("related_host") + " is pointing to a site that has been marked for deletion: " + resultSet.getString("hoststring");
			}
			
			resultSet.close();
			
			if( ! relatedHostDeletedSiteConflictMessage.equals("") ) {
				HrwaManager.writeToLog("One or more related host / deleted site conflict found:" + relatedHostDeletedSiteConflictMessage + "\nThis problem must be resolved before RegularMaintenanceTask can continue.\nExiting program.", true, HrwaManager.LOG_TYPE_ERROR);
				System.exit(HrwaManager.EXIT_CODE_ERROR);
			}
			
			//Get number of DELETED sites.  If > 0, then we want to run through MySQL and unlink these sites from their corresponding web archive records, marking each web archive record hrwa_manager_todo field as DELETED 
			if(MySQLHelper.getSitesMap("WHERE sites.hrwa_manager_todo = 'DELETED'").size() > 0) {
			
				pstmt = conn.prepareStatement(
					"UPDATE web_archive_records" +
					" INNER JOIN sites ON web_archive_records.site_id = sites.id" +
					" SET web_archive_records.site_id = NULL, web_archive_records.hrwa_manager_todo = 'DELETED', web_archive_records.linked_via_related_host = 0" +  
					" WHERE" + 
					" web_archive_records.id >= ?" +
					" AND web_archive_records.id <= ?" +
					" AND sites.hrwa_manager_todo = 'DELETED'" +
					" AND web_archive_records.site_id IS NOT NULL" +
					" AND (" +
						" web_archive_records.hrwa_manager_todo IS NULL" +
						" OR" +
						" web_archive_records.hrwa_manager_todo != 'NOINDEX'" +
					" )"
				);
				
				for(currentRecordRetrievalOffset = 0, rowsAffected = 0; currentRecordRetrievalOffset < maxWebArchiveRecordMySQLId; currentRecordRetrievalOffset += HrwaManager.regularMaintenanceMySQLRowRetrievalSize) {
					System.out.println("Unlinking and marking for deletion any web archive records linked to DELETED sites. MySQL batch " + (currentRecordRetrievalOffset/HrwaManager.regularMaintenanceMySQLRowRetrievalSize) + " of " + (maxWebArchiveRecordMySQLId/HrwaManager.regularMaintenanceMySQLRowRetrievalSize) + " (" + rowsAffected + " rows updated so far)...");
					pstmt.setLong(1, currentRecordRetrievalOffset);
					pstmt.setLong(2, currentRecordRetrievalOffset + HrwaManager.regularMaintenanceMySQLRowRetrievalSize);
					rowsAffected += pstmt.executeUpdate();
				}
				
				pstmt.close();
				
				HrwaManager.writeToLog("Done linking web archive records to NEW sites! " +
				"Affected rows: " + rowsAffected, true, HrwaManager.LOG_TYPE_STANDARD);
				
				totalNumberOfWebArchiveRecordRowsUpdatedByThisTask += rowsAffected;
		
				
				//And now do a Solr doc deletion for all MySQL web_archive_records marked as DELETED
				pstmt = conn.prepareStatement(
					"SELECT record_identifier FROM web_archive_records" +
					" WHERE" + 
					" web_archive_records.id >= ?" +
					" AND web_archive_records.id <= ?" +
					" AND hrwa_manager_todo = 'DELETED'"
				);
				
				ASFSolrIndexer.initSingleSolrServerObject();
				
				for(currentRecordRetrievalOffset = 0, rowsAffected = 0; currentRecordRetrievalOffset < maxWebArchiveRecordMySQLId; currentRecordRetrievalOffset += HrwaManager.regularMaintenanceMySQLRowRetrievalSize) {
					System.out.println("Deleting solr docs that are defined as DELETED in the web_archive_records table. MySQL batch " + (currentRecordRetrievalOffset/HrwaManager.regularMaintenanceMySQLRowRetrievalSize) + " of " + (maxWebArchiveRecordMySQLId/HrwaManager.regularMaintenanceMySQLRowRetrievalSize) + " (" + rowsAffected + " rows updated so far)...");
					
					pstmt.setLong(1, currentRecordRetrievalOffset);
					pstmt.setLong(2, currentRecordRetrievalOffset + HrwaManager.regularMaintenanceMySQLRowRetrievalSize);
					
					resultSet = pstmt.executeQuery();
					while(resultSet.next()) {
						ASFSolrIndexer.deleteAsfDocumentByUniqueIdRecordIdentifier(resultSet.getString(1), false);
						System.out.println("Deleted document from solr: " + resultSet.getString(1));
					}
					resultSet.close();
					ASFSolrIndexer.commit();
				}
				
				pstmt.close();
				
				ASFSolrIndexer.shutdownSingleSolrServerObject();
				
				//And now that we've successfully deleted all of these unwawnted mysql records from solr, let's mark these hrwa_manager_todo = DELETED web archive records as hrwa_manager_todo = NULL
				pstmt = conn.prepareStatement(
					"UPDATE web_archive_records" +
					" SET web_archive_records.hrwa_manager_todo = NULL" +  
					" WHERE" + 
					" web_archive_records.id >= ?" +
					" AND web_archive_records.id <= ?" +
					" AND web_archive_records.hrwa_manager_todo = 'DELETED'"
				);
				
				for(currentRecordRetrievalOffset = 0, rowsAffected = 0; currentRecordRetrievalOffset < maxWebArchiveRecordMySQLId; currentRecordRetrievalOffset += HrwaManager.regularMaintenanceMySQLRowRetrievalSize) {
					System.out.println("Unlinking and marking for deletion any web archive records linked to DELETED sites. MySQL batch " + (currentRecordRetrievalOffset/HrwaManager.regularMaintenanceMySQLRowRetrievalSize) + " of " + (maxWebArchiveRecordMySQLId/HrwaManager.regularMaintenanceMySQLRowRetrievalSize) + " (" + rowsAffected + " rows updated so far)...");
					pstmt.setLong(1, currentRecordRetrievalOffset);
					pstmt.setLong(2, currentRecordRetrievalOffset + HrwaManager.regularMaintenanceMySQLRowRetrievalSize);
					rowsAffected += pstmt.executeUpdate();
				}
				
				pstmt.close();
				
				HrwaManager.writeToLog("Done deleting unwanted MySQL records from Solr! " +
				"Affected rows: " + rowsAffected, true, HrwaManager.LOG_TYPE_STANDARD);
				
				totalNumberOfWebArchiveRecordRowsUpdatedByThisTask += rowsAffected;
				
				//And we'll also want to remove the deleted sites from the sites table now that we're done with it
				pstmt = conn.prepareStatement(
					"DELETE FROM sites WHERE sites.hrwa_manager_todo = 'DELETED'"
				);
				
				rowsAffected = pstmt.executeUpdate();
				pstmt.close();
				
				HrwaManager.writeToLog("Number of hrwa_manager_todo = DELETED sites deleted from the sites table: " + rowsAffected, true, HrwaManager.LOG_TYPE_STANDARD);
				
			} else {
				HrwaManager.writeToLog("No DELETED sites found, so no deleted-sites-related updates were necessary.", true, HrwaManager.LOG_TYPE_STANDARD);
			}
			
			//Get number of NEW sites.  If > 0, then we want to go through all unlinked web archive records and link them to a site
			if(MySQLHelper.getSitesMap("WHERE sites.hrwa_manager_todo = 'NEW'").size() > 0) {
			
				pstmt = conn.prepareStatement(
					"UPDATE web_archive_records" +
					" INNER JOIN sites ON web_archive_records.hoststring = sites.hoststring" +
					" SET web_archive_records.site_id = sites.id, web_archive_records.hrwa_manager_todo = 'UPDATED', web_archive_records.linked_via_related_host = 0" +  
					" WHERE" + 
					" web_archive_records.id >= ?" +
					" AND web_archive_records.id <= ?" +
					" AND web_archive_records.site_id IS NULL" + 
					" AND sites.hrwa_manager_todo = 'NEW'" +
					" AND (" +
						" web_archive_records.hrwa_manager_todo IS NULL" +
						" OR" +
						" web_archive_records.hrwa_manager_todo != 'NOINDEX'" +
					" )"
				);
				
				for(currentRecordRetrievalOffset = 0, rowsAffected = 0; currentRecordRetrievalOffset < maxWebArchiveRecordMySQLId; currentRecordRetrievalOffset += HrwaManager.regularMaintenanceMySQLRowRetrievalSize) {
					System.out.println("Linking web archive records to NEW sites. MySQL batch " + (currentRecordRetrievalOffset/HrwaManager.regularMaintenanceMySQLRowRetrievalSize) + " of " + (maxWebArchiveRecordMySQLId/HrwaManager.regularMaintenanceMySQLRowRetrievalSize) + " (" + rowsAffected + " rows updated so far)...");
					pstmt.setLong(1, currentRecordRetrievalOffset);
					pstmt.setLong(2, currentRecordRetrievalOffset + HrwaManager.regularMaintenanceMySQLRowRetrievalSize);
					rowsAffected += pstmt.executeUpdate();
				}
				HrwaManager.writeToLog("Linking web archive records to NEW sites -- Done! " +
				"Affected rows: " + rowsAffected, true, HrwaManager.LOG_TYPE_STANDARD);
				
				totalNumberOfWebArchiveRecordRowsUpdatedByThisTask += rowsAffected;
		
				pstmt.close();
				
				//Once all of this updating is done, set the hrwa_manager_todo status to NULL for all sites that were previously marked as NEW 
				
				pstmt = conn.prepareStatement("UPDATE sites SET hrwa_manager_todo = NULL WHERE hrwa_manager_todo = 'NEW'");
				HrwaManager.writeToLog("Total number of hrwa_manager_todo='NEW' sites reset to NULL: " + pstmt.executeUpdate(), true, HrwaManager.LOG_TYPE_STANDARD);
			
			} else {
				HrwaManager.writeToLog("No NEW sites found, so no sites-related updates were necessary.", true, HrwaManager.LOG_TYPE_STANDARD);
			}
			
			//For NEW related hosts, we'll also update unlinked web archive records that should be linked via related hosts to sites
			//Do this in groups of 1000 to avoid massive MySQL joins that can cause memory problems or major slowdowns
			
			//Get number of NEW related hosts.  If > 0, then we want to go through all unlinked web archive records and link them to a site THROUGH a related host
			if(MySQLHelper.getRelatedHostsMap("WHERE related_hosts.hrwa_manager_todo = 'NEW'").size() > 0) {
				pstmt = conn.prepareStatement(
					"UPDATE web_archive_records" +
					" INNER JOIN related_hosts ON web_archive_records.hoststring = related_hosts.related_host" +
					" INNER JOIN sites ON related_hosts.site_id = sites.id" +
					" SET web_archive_records.site_id = sites.id, web_archive_records.hrwa_manager_todo = 'UPDATED', linked_via_related_host = 1" +
					" WHERE" +
					" web_archive_records.id >= ?" +
					" AND web_archive_records.id <= ?" +
					" AND web_archive_records.site_id IS NULL" +
					" AND related_hosts.hrwa_manager_todo = 'NEW'" +
					" AND (" +
						" web_archive_records.hrwa_manager_todo IS NULL" +
						" OR" +
						" web_archive_records.hrwa_manager_todo != 'NOINDEX'" +
					" )"
				);
				
				for(currentRecordRetrievalOffset = 0, rowsAffected = 0; currentRecordRetrievalOffset < maxWebArchiveRecordMySQLId; currentRecordRetrievalOffset += HrwaManager.regularMaintenanceMySQLRowRetrievalSize) {
					System.out.println("Linking web archive records to NEW related hosts. MySQL batch " + (currentRecordRetrievalOffset/HrwaManager.regularMaintenanceMySQLRowRetrievalSize) + " of " + (maxWebArchiveRecordMySQLId/HrwaManager.regularMaintenanceMySQLRowRetrievalSize) + " (" + rowsAffected + " rows updated so far)...");
					pstmt.setLong(1, currentRecordRetrievalOffset);
					pstmt.setLong(2, currentRecordRetrievalOffset + HrwaManager.regularMaintenanceMySQLRowRetrievalSize);
					rowsAffected += pstmt.executeUpdate();
				}
				HrwaManager.writeToLog("Linking web archive records to NEW related hosts -- Done! " +
						"Affected rows: " + rowsAffected, true, HrwaManager.LOG_TYPE_STANDARD);
				
				totalNumberOfWebArchiveRecordRowsUpdatedByThisTask += rowsAffected;
				
				pstmt.close();
				
				//Once all of this updating is done, set the hrwa_manager_todo status to NULL for all related hosts that were previously marked as NEW 
				pstmt = conn.prepareStatement("UPDATE related_hosts SET hrwa_manager_todo = NULL WHERE hrwa_manager_todo = 'NEW'");
				HrwaManager.writeToLog("Total number of hrwa_manager_todo='NEW' related_hosts reset to NULL: " + pstmt.executeUpdate(), true, HrwaManager.LOG_TYPE_STANDARD);
				
			} else {
				HrwaManager.writeToLog("No NEW related hosts found, so no related-hosts-related updates were necessary.", true, HrwaManager.LOG_TYPE_STANDARD);
			}
			
			//For UPDATED sites, we'll also update the archive records that are related to these updated sites
			//Do this in groups of 1000 to avoid massive MySQL joins that could cause memory problems or major slowdowns
			
			//Get number of UPDATED sites.  If > 0, then we want to mark all linked web archive records as UPDATED
			if(MySQLHelper.getSitesMap("WHERE sites.hrwa_manager_todo = 'UPDATED'").size() > 0) {
			
				pstmt = conn.prepareStatement(
					"UPDATE web_archive_records" +
					" INNER JOIN sites ON web_archive_records.site_id = sites.id" +
					" INNER JOIN " + HrwaManager.MYSQL_MIMETYPE_CODES_TABLE_NAME + " ON " + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + ".mimetype_detected =  " + HrwaManager.MYSQL_MIMETYPE_CODES_TABLE_NAME + ".mimetype_detected" +
					" SET web_archive_records.hrwa_manager_todo = 'UPDATED'" +
					" WHERE" +
					" web_archive_records.id >= ?" + 
					" AND web_archive_records.id <= ?" +
					" AND web_archive_records.site_id IS NOT NULL" +
					" AND (" +
						" web_archive_records.hrwa_manager_todo IS NULL" +
						" OR" +
						" web_archive_records.hrwa_manager_todo != 'NOINDEX'" +
					" )" +
					" AND sites.hrwa_manager_todo = 'UPDATED'" +
					" AND " + HrwaManager.MYSQL_MIMETYPE_CODES_TABLE_NAME + ".mimetype_code IN " + HrwaManager.DESIRED_SOLR_INDEXED_MIMETYPE_CODES_STRING_FOR_MYSQL_WHERE_CLAUSE_LIST
				);
				
				for(currentRecordRetrievalOffset = 0, rowsAffected = 0; currentRecordRetrievalOffset < maxWebArchiveRecordMySQLId; currentRecordRetrievalOffset += HrwaManager.regularMaintenanceMySQLRowRetrievalSize) {
					System.out.println("Updating web archive records hrwa_manager_todo = 'UPDATED' where associated site has been marked as UPDATED. MySQL batch " + (currentRecordRetrievalOffset/HrwaManager.regularMaintenanceMySQLRowRetrievalSize) + " of " + (maxWebArchiveRecordMySQLId/HrwaManager.regularMaintenanceMySQLRowRetrievalSize) + " (" + rowsAffected + " rows updated so far)...");
					pstmt.setLong(1, currentRecordRetrievalOffset);
					pstmt.setLong(2, currentRecordRetrievalOffset + HrwaManager.regularMaintenanceMySQLRowRetrievalSize);
					rowsAffected += pstmt.executeUpdate();
				}
				HrwaManager.writeToLog("Done updating web archive records where associated site has been marked as UPDATED! " +
				"Affected rows: " + rowsAffected, true, HrwaManager.LOG_TYPE_STANDARD);
		
				totalNumberOfWebArchiveRecordRowsUpdatedByThisTask += rowsAffected;
				
				pstmt.close();
				
				//One all of this updating is done, set the hrwa_manager_todo status to NULL for all sites that were previously marked as UPDATED 
				
				pstmt = conn.prepareStatement("UPDATE sites SET hrwa_manager_todo = NULL WHERE hrwa_manager_todo = 'UPDATED'");
				HrwaManager.writeToLog("Total number of hrwa_manager_todo='UPDATED' sites reset to NULL: " + pstmt.executeUpdate(), true, HrwaManager.LOG_TYPE_STANDARD);
			
			} else {
				HrwaManager.writeToLog("No NEW sites found, so no sites-related updates were necessary.", true, HrwaManager.LOG_TYPE_STANDARD);
			}
			
			//Now Scan through all web_archive_records table rows and set hrwa_manager_todo to NULL
			//for all archive records that should not be indexed.
			pstmt = conn.prepareStatement(
				"UPDATE web_archive_records" +
				" LEFT OUTER JOIN " + HrwaManager.MYSQL_MIMETYPE_CODES_TABLE_NAME + " ON " + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + ".mimetype_detected =  " + HrwaManager.MYSQL_MIMETYPE_CODES_TABLE_NAME + ".mimetype_detected" +
				" SET hrwa_manager_todo = NULL" +
				" WHERE" +
				" web_archive_records.id >= ?" +
				" AND" +
				" web_archive_records.id <= ?" +
				" AND" +
				" web_archive_records.hrwa_manager_todo IS NOT NULL" +
				" AND (" +
				//	" web_archive_records.hrwa_manager_todo IS NULL" +
				//	" OR" +
					" web_archive_records.hrwa_manager_todo != 'NOINDEX'" +
				" )" +
				" AND (" +
					" site_id IS NULL" +
					" OR " + HrwaManager.MYSQL_MIMETYPE_CODES_TABLE_NAME + ".mimetype_code IS NULL" +
					" OR " + HrwaManager.MYSQL_MIMETYPE_CODES_TABLE_NAME + ".mimetype_code NOT IN " + HrwaManager.DESIRED_SOLR_INDEXED_MIMETYPE_CODES_STRING_FOR_MYSQL_WHERE_CLAUSE_LIST +
				" )"
			);
			
			for(currentRecordRetrievalOffset = 0, rowsAffected = 0; currentRecordRetrievalOffset < maxWebArchiveRecordMySQLId; currentRecordRetrievalOffset += HrwaManager.regularMaintenanceMySQLRowRetrievalSize) {
				System.out.println("Setting hrwa_manager_todo = NULL for items that we don't want to index. MySQL batch " + (currentRecordRetrievalOffset/HrwaManager.regularMaintenanceMySQLRowRetrievalSize) + " of " + (maxWebArchiveRecordMySQLId/HrwaManager.regularMaintenanceMySQLRowRetrievalSize) + " (" + rowsAffected + " rows updated so far)...");
				pstmt.setLong(1, currentRecordRetrievalOffset);
				pstmt.setLong(2, currentRecordRetrievalOffset + HrwaManager.regularMaintenanceMySQLRowRetrievalSize);
				rowsAffected += pstmt.executeUpdate();
			}
			HrwaManager.writeToLog("Setting hrwa_manager_todo = NULL for items that we don't want to index -- Done! " +
			"Affected rows: " + rowsAffected, true, HrwaManager.LOG_TYPE_STANDARD);
			
			pstmt.close();
		
		} catch (SQLException e) {
			e.printStackTrace();
			HrwaManager.writeToLog("Error: An SQLException occurred: " + e.getMessage(), true, HrwaManager.LOG_TYPE_ERROR);
			System.exit(HrwaManager.EXIT_CODE_ERROR);
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				HrwaManager.writeToLog("Error: Could not close MySQL connection: " + e.getMessage(), true, HrwaManager.LOG_TYPE_ERROR);
				System.exit(HrwaManager.EXIT_CODE_ERROR);
			}
		}
		
		//And finally, run SitesToSolrAndMySQLTask IF AND ONLY IF totalNumberOfWebArchiveRecordRowsUpdatedByThisTask > 0
		//If totalNumberOfWebArchiveRecordRowsUpdatedByThisTask == 0, then that means that there were no updates.
		if(totalNumberOfWebArchiveRecordRowsUpdatedByThisTask > 0) {
			HrwaTask mySQLArchiveRecordsToSolrTask = new MySQLArchiveRecordsToSolrTask();
			mySQLArchiveRecordsToSolrTask.runTask();
		} else {
			HrwaManager.writeToLog("No web archive record rows were updated by any of the sub-tasks within RegularMaintenanceTask, so there's no need to run the MySQLArchiveRecordsToSolrTask. Nice! That's a great time-saver!", true, HrwaManager.LOG_TYPE_STANDARD);
		}
		
		writeTaskFooterMessageAndPrintTotalTime();
		
	}
	
}
