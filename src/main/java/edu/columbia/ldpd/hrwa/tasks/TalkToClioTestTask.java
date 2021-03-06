package edu.columbia.ldpd.hrwa.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jafer.exception.JaferException;
import org.jafer.util.xml.DOMFactory;
import org.w3c.dom.Document;

import edu.columbia.ldpd.hrwa.HrwaManager;
import edu.columbia.ldpd.hrwa.HrwaSiteRecord;
import edu.columbia.ldpd.hrwa.clio.Main;
import edu.columbia.ldpd.hrwa.clio.SolrDoc;
import edu.columbia.ldpd.hrwa.marc.z3950.MARCFetcher;
import edu.columbia.ldpd.hrwa.mysql.MySQLHelper;
import edu.columbia.ldpd.hrwa.solr.SolrIndexer;

public class TalkToClioTestTask extends HrwaTask {
	
	private static String PATH_TO_FSF_SOLR_PROPERTIES_FILE = "/fsf_solr.properties";
	
	private static String tempHrwaClioMarcXmlFileDirectory =  HrwaManager.tmpDirPath + File.separator + "hrwa_clio_marc_xml_files";
	private static String tempHrwaFSFSolrDocXmlFileDirectory =  HrwaManager.tmpDirPath + File.separator + "hrwa_fsf_solr_doc_xml_files";

	public TalkToClioTestTask() {
		
	}
	
	public void runTask() {
		
		writeTaskHeaderMessageAndSetStartTime();
		
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
