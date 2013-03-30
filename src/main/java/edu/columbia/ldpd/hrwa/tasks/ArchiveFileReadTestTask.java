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
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.nutchwax.tools.ArcReader;
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

public class ArchiveFileReadTestTask extends HrwaTask {
	
	public ArchiveFileReadTestTask() {
		
	}
	
	public void runTask() {
		
		writeTaskHeaderMessageAndSetStartTime();
		
		//Retrieve the first archive file found in the archive file directory
		String[] validArchiveFileExtensions = {"arc.gz", "warc.gz"};
		ArrayList<File> archiveFiles = ArchiveToMySQLTask.getAlphabeticallySortedRecursiveListOfFilesFromArchiveDirectory(HrwaManager.archiveFileDirPath, validArchiveFileExtensions);
		
		int numRecordsFound = 0;
		
		if(archiveFiles.size() == 0) {
			HrwaManager.writeToLog("Error: No archive files found in archive file directory. Ending Archive File Read test.", true, HrwaManager.LOG_TYPE_ERROR);
		} else {
			HrwaManager.writeToLog("Reading only the first archive file found (out of a total of " + archiveFiles.size() + " found archive files).", true, HrwaManager.LOG_TYPE_STANDARD);
					
			File archiveFileToRead = archiveFiles.get(0);
			
			//We need to get an ArchiveReader for this file
			ArchiveReader archiveReader = null;
			
			try {
				
				archiveReader = ArchiveReaderFactory.get(archiveFileToRead);
			
				archiveReader.setDigest(true);
	
				// Wrap archiveReader in NutchWAX ArcReader class, which converts WARC
				// records to ARC records on-the-fly, returning null for any records that
				// are not WARC-Type "response".
				ArcReader arcReader = new ArcReader(archiveReader);
				
				// Loop through all archive records in this file
				for (ARCRecord arcRecord : arcReader) {
					
					if (arcRecord == null) {
						System.out.println("Found a null record.");
					}
					else if(arcRecord.getMetaData().getUrl().startsWith("dns:")) {
						System.out.println("Found a DNS record.");
					}
					else
					{
						System.out.println("Found a relevant record.");
					}
					
					numRecordsFound++;
				}
			
			} catch (IOException e) {
				HrwaManager.writeToLog("An error occurred while trying to read in the archive file at " + archiveFileToRead.getPath(), true, HrwaManager.LOG_TYPE_ERROR);
				e.printStackTrace();
			} finally {
				try {
					archiveReader.close();
				} catch (IOException e) {
					HrwaManager.writeToLog("An error occurred while trying to close the ArchiveReader for file: " + archiveFileToRead.getPath(), true, HrwaManager.LOG_TYPE_ERROR);
					e.printStackTrace();
				}
			}
		}
		
		HrwaManager.writeToLog("Total number of records found within this archive file: " + numRecordsFound, true, HrwaManager.LOG_TYPE_STANDARD);
		
		writeTaskFooterMessageAndPrintTotalTime();
		
	}
	
}
