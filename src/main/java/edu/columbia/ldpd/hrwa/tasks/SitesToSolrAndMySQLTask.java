package edu.columbia.ldpd.hrwa.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import edu.columbia.ldpd.hrwa.HrwaManager;
import edu.columbia.ldpd.hrwa.HrwaSiteRecord;
import edu.columbia.ldpd.hrwa.clio.Main;
import edu.columbia.ldpd.hrwa.clio.SolrDoc;
import edu.columbia.ldpd.hrwa.marc.z3950.MARCFetcher;
import edu.columbia.ldpd.hrwa.mysql.MySQLHelper;
import edu.columbia.ldpd.hrwa.solr.SolrIndexer;

public class SitesToSolrAndMySQLTask extends HrwaTask {
	
	private static String PATH_TO_FSF_SOLR_PROPERTIES_FILE = "/fsf_solr.properties";
	
	private static String tempHrwaClioMarcXmlFileDirectory =  HrwaManager.tmpDirPath + File.separator + "hrwa_clio_marc_xml_files";
	private static String tempHrwaFSFSolrDocXmlFileDirectory =  HrwaManager.tmpDirPath + File.separator + "hrwa_fsf_solr_doc_xml_files";

	public SitesToSolrAndMySQLTask() {
		
	}
	
	public void runTask() {
		
		writeTaskHeaderMessageAndSetStartTime();
		
		//Create sites table if necessary.  Also related hosts table.
		try {
			MySQLHelper.createSitesTableIfItDoesNotExist();
		} catch (SQLException e2) {
			HrwaManager.writeToLog("Error: Could not create the MySQL sites tables.", true, HrwaManager.LOG_TYPE_ERROR);
		}
		
		//Fetch Marc Data From CLIO
		
		//Create temp download directory for marc files
		File marcFileDir = new File(tempHrwaClioMarcXmlFileDirectory);
		marcFileDir.mkdirs();
		//Download marc files
		HrwaManager.writeToLog("Fetching all relevant HRWA marc record files from CLIO. This may take a minute...", true, HrwaManager.LOG_TYPE_STANDARD);
		new MARCFetcher(new File(tempHrwaClioMarcXmlFileDirectory)).fetch();
		HrwaManager.writeToLog("Done fetching records!", true, HrwaManager.LOG_TYPE_STANDARD);
		
		//Create temp directory for generated solr files (which will be generated from the marc files) 
		File solrFileDir = new File(tempHrwaFSFSolrDocXmlFileDirectory);
		solrFileDir.mkdirs();
		
		System.out.println("Got here1 !");
		
		// Create a solr doc file from each marc file!
		for (File singleMarcFile : marcFileDir.listFiles()) {
			
			if( ! singleMarcFile.getName().endsWith(".xml") ) {
				//Ignore non-xml files (like OSX .DS_Store)
				continue;
			}
			
			InputStream marcInputStream;
			try {
				System.out.println("File name: " + singleMarcFile.getName());
				marcInputStream = new FileInputStream(singleMarcFile);
				System.out.println("marcInputStream: " + marcInputStream);
				new SolrDoc(marcInputStream).serialize(solrFileDir);
				
				marcInputStream.close();
			} catch (FileNotFoundException e) {
				HrwaManager.writeToLog("Error: Could not file an expected marc file while iterating through the temporary marc fild download directory." , true, HrwaManager.LOG_TYPE_ERROR);
				e.printStackTrace();
			} catch (IOException e) {
				HrwaManager.writeToLog("Error: An IO exception occurred while iterating through the temporary marc fild download directory." , true, HrwaManager.LOG_TYPE_ERROR);
				e.printStackTrace();
			}
		}
		
		System.out.println("Got here2 !");
		
		// Index all new Solr doc data into mysql:
		// This means:
		// -- Check to see if any records have been DELETED (by checking to see if they DO EXIST in MySQL, but do not exist the set of solr docs that we're working with)
		// -- Check to see if any existing FSF records have been modified (by checking for differing marc_005_last_modified values) 
		// -- Add all new FSF records that are not already in MySQL 
		
		//So we'll generate two sets:
		// -- (1) All of the bib keys in MySQL
		// -- (2) All of the bib keys within the set of new Solr docs that we want to index
		
		
		HashSet<String> bibKeySetAmongCLIORecordGeneratedSolrDocs = new HashSet<String>();
		
		for(String fileName : solrFileDir.list()) {
			if( ! fileName.endsWith(".xml") ) {
				//Ignore non-xml files (like OSX .DS_Store)
				continue;
			}
			bibKeySetAmongCLIORecordGeneratedSolrDocs.add(fileName.substring(0, fileName.length()-4)); //chop off ".xml"
		}
	
		HashSet<String> bibKeySetAmongExistingSiteTableMySQLRows = MySQLHelper.getAllBibKeysFromMySQLSitesTable();
		
		HashSet<String> bibKeysOfRecordsToDelete;
		HashSet<String> bibKeysOfNewRecords;
		
		bibKeysOfRecordsToDelete = new HashSet<String>(bibKeySetAmongExistingSiteTableMySQLRows);
		bibKeysOfRecordsToDelete.removeAll(bibKeySetAmongCLIORecordGeneratedSolrDocs);
		
		//So now we know which records need to be deleted.  Let's mark them to be deleted.
		MySQLHelper.markSitesToBeDeleted(bibKeysOfRecordsToDelete);
		
		HrwaManager.writeToLog("Number of existing sites table records to delete: " + bibKeysOfRecordsToDelete.size(), true, HrwaManager.LOG_TYPE_STANDARD);
		
		//Collect all solr-ready HrwaSiteRecords to add/update in MySQL
		ArrayList<HrwaSiteRecord> hrwaSiteRecordToAddOrUpdate = new ArrayList<HrwaSiteRecord>();
		
		for (File singleSolrFile : solrFileDir.listFiles()) {
			
			if( ! singleSolrFile.getName().endsWith(".xml") ) {
				//Ignore non-xml files (like OSX .DS_Store)
				continue;
			}
			
			hrwaSiteRecordToAddOrUpdate.add(new HrwaSiteRecord(singleSolrFile));
		}
		
		//And now we'll add or update those HrwaSiteRecords
		MySQLHelper.addOrUpdateHrwaSiteRecordsInMySQLSitesTable(hrwaSiteRecordToAddOrUpdate);
		
		
		//Now let's index all of those solr docs into solr
		Properties solrPropertiesConfig = new Properties();
        try {
			solrPropertiesConfig.load(getClass().getResourceAsStream(PATH_TO_FSF_SOLR_PROPERTIES_FILE));
	        SolrIndexer si = new SolrIndexer(solrPropertiesConfig);
	        si.index(solrFileDir);
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        try {
			MySQLHelper.refreshRelatedHostsTable();
		} catch (SQLException e) {
			HrwaManager.writeToLog("Error: Could not create the MySQL related hosts tables.", true, HrwaManager.LOG_TYPE_ERROR);
		}
		
		//If the temp directories are empty, delete them (and all files in them)
		try {
			FileUtils.deleteDirectory(marcFileDir);
			FileUtils.deleteDirectory(solrFileDir);
		} catch (IOException e) {
			HrwaManager.writeToLog("Error: Encountered a problem while attempting to delete the contents of tempHrwaClioMarcXmlFileDirectory (" + tempHrwaClioMarcXmlFileDirectory + ")", true, HrwaManager.LOG_TYPE_STANDARD);
		}
		
		writeTaskFooterMessageAndPrintTotalTime();
		
	}
	
}
