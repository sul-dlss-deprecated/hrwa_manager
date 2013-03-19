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
		
		//Update sites and related hosts tables to latest versions
		HrwaTask updateSitesTable = new SitesToSolrAndMySQLTask();
		updateSitesTable.runTask();
		
		////////// ARE THERE ANY NEW SITES / RELATED HOSTS THAT WE NEED TO LINK TO WEB ARCHIVE RECORDS? //////////
		
		//Update unlinked web archive records that should be linked to new sites (linked by hoststring)
		//Do this in groups of 1000 to avoid massive MySQL joins that could cause memory problems or major slowdowns
		
		long maxWebArchiveRecordMySQLId = MySQLHelper.getMaxIdFromWebArchiveRecordsTable();
		
		Connection conn = MySQLHelper.getNewDBConnection(true); //we want auto-commit to be on!
		PreparedStatement pstmt;
		long currentRecordRetrievalOffset;
		long rowsUpdated;
		
		try {
			
		//Get number of NEW sites.  If > 0, then run update below
		if(MySQLHelper.getSitesMap("WHERE sites.hrwa_manager_todo = 'NEW'").size() > 0) {
		
			pstmt = conn.prepareStatement(
				"UPDATE web_archive_records" +
				" INNER JOIN sites ON web_archive_records.hoststring = sites.hoststring" +
				" SET web_archive_records.site_id = sites.id, web_archive_records.hrwa_manager_todo = NULL, web_archive_records.linked_via_related_host = 0" +  
				" WHERE" + 
				" web_archive_records.site_id IS NULL" + 
				" AND" + 
				" sites.hrwa_manager_todo = 'NEW'" + 
				" AND " + 
				" web_archive_records.id >= ?" + 
				" AND" + 
				" web_archive_records.id <= ?"
			);
			
			for(currentRecordRetrievalOffset = 0, rowsUpdated = 0; currentRecordRetrievalOffset < maxWebArchiveRecordMySQLId; currentRecordRetrievalOffset += HrwaManager.regularMaintenanceMySQLRowRetrievalSize) {
				pstmt.setLong(1, currentRecordRetrievalOffset);
				pstmt.setLong(2, currentRecordRetrievalOffset + HrwaManager.regularMaintenanceMySQLRowRetrievalSize);
				rowsUpdated += pstmt.executeUpdate();
				
				System.out.println("Linking web archive records to NEW sites..." + (int)(100*currentRecordRetrievalOffset/maxWebArchiveRecordMySQLId) + "% complete (" + rowsUpdated + " rows updated so far)...");
			}
			
			HrwaManager.writeToLog("Linking web archive records to NEW sites...100% complete!\nDone!\n" +
			"Total number of rows updated: " + rowsUpdated, true, HrwaManager.LOG_TYPE_STANDARD);
	
			pstmt.close();
			
			//One all of this updating is done, set the hrwa_manager_todo status to NULL for all sites that were previously marked as NEW 
			
			pstmt = conn.prepareStatement("UPDATE sites SET hrwa_manager_todo = NULL WHERE hrwa_manager_todo = 'NEW'");
			HrwaManager.writeToLog("Total number of new sites updated: " + pstmt.executeUpdate(), true, HrwaManager.LOG_TYPE_STANDARD);
		
		} else {
			HrwaManager.writeToLog("No NEW sites found, so no sites-related updates were necessary.", true, HrwaManager.LOG_TYPE_STANDARD);
		}
		
		//For NEW related hosts, we'll also update unlinked web archive records that should be linked via related hosts to sites
		//Do this in groups of 1000 to avoid massive MySQL joins that could cause memory problems or major slowdowns
		
		//Get number of NEW related hosts.  If > 0, then run update below
		if(MySQLHelper.getSitesMap("WHERE sites.hrwa_manager_todo = 'NEW'").size() > 0) {
			pstmt = conn.prepareStatement(
				"UPDATE web_archive_records" +
				" INNER JOIN related_hosts ON web_archive_records.hoststring = related_hosts.related_host" +
				" INNER JOIN sites ON related_hosts.site_id = sites.id" +
				" SET web_archive_records.site_id = sites.id, web_archive_records.hrwa_manager_todo = NULL, linked_via_related_host = 1" +
				" WHERE" +
				" web_archive_records.site_id IS NULL" +
				" AND" +
				" related_hosts.hrwa_manager_todo = 'NEW'" +
				" AND " +
				" web_archive_records.id >= ?" +
				" AND" +
				" web_archive_records.id <= ?"
			);
			
			for(currentRecordRetrievalOffset = 0, rowsUpdated = 0; currentRecordRetrievalOffset < maxWebArchiveRecordMySQLId; currentRecordRetrievalOffset += HrwaManager.regularMaintenanceMySQLRowRetrievalSize) {
				pstmt.setLong(1, currentRecordRetrievalOffset);
				pstmt.setLong(2, currentRecordRetrievalOffset + HrwaManager.regularMaintenanceMySQLRowRetrievalSize);
				rowsUpdated += pstmt.executeUpdate();
				
				System.out.println("Linking web archive records to NEW related hosts..." + (int)(100*currentRecordRetrievalOffset/maxWebArchiveRecordMySQLId) + "% complete (" + rowsUpdated + " rows updated so far)...");
			}
			
			HrwaManager.writeToLog("Linking web archive records to NEW related hosts...100% complete!\nDone!\n" +
					"Total number of rows updated: " + rowsUpdated, true, HrwaManager.LOG_TYPE_STANDARD);
			
			pstmt.close();
			
			//One all of this updating is done, set the hrwa_manager_todo status to NULL for all related_hosts that were previously marked as NEW 
			
			pstmt = conn.prepareStatement("UPDATE related_hosts SET hrwa_manager_todo = NULL WHERE hrwa_manager_todo = 'NEW'");
			HrwaManager.writeToLog("Total number of related hosts updated: " + pstmt.executeUpdate(), true, HrwaManager.LOG_TYPE_STANDARD);
		} else {
			HrwaManager.writeToLog("No NEW related hosts found, so no related-hosts-related updates were necessary.", true, HrwaManager.LOG_TYPE_STANDARD);
		}
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				HrwaManager.writeToLog("Error: Could not close MySQL connection: " + e.getMessage(), true, HrwaManager.LOG_TYPE_ERROR);
			}
		}
		
		writeTaskFooterMessageAndPrintTotalTime();
		
	}
	
}
