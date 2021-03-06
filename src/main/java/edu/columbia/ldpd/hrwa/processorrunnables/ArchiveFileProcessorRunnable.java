package edu.columbia.ldpd.hrwa.processorrunnables;

import java.io.File;
import java.io.FileNotFoundException;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

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

public class ArchiveFileProcessorRunnable implements Runnable {
	
	private static final String ARCHIVED_URL_PREFIX = "http://wayback.archive-it.org/1068/";
	
	private int uniqueRunnableId;
	private long numRelevantArchiveRecordsProcessed = 0;
	private final MimetypeDetector mimetypeDetector;
	private Boolean isProcessingAnArchiveFile = false;
	
	private ConcurrentLinkedQueue<File> sharedReferenceToConcurrentLinkedQueueOfArchiveFiles;
	
	// Even though every instance of an ArchiveFileProcessorRunnable will have
	// the same data stored in sitesMap and relatedHostsMap, I don't want to
	// worry about thread safety and sharing data, so each thread will generate
	// its own copy of these HashMaps. They're small objects anyway.
	private final HashMap<String, Integer> sitesMap;
	private final HashMap<String, Integer> relatedHostsMap;
	
	private Connection mySQLConn = null;
	private PreparedStatement mainRecordInsertPstmt;
	
	public ArchiveFileProcessorRunnable(int uniqueNumericId, ConcurrentLinkedQueue<File> concurrentLinkedQueueOfArchiveFiles) {
		//Assign uniqueNumericId
		uniqueRunnableId = uniqueNumericId;
		
		//Pass reference to shared concurrentLinkedQueueOfArchiveFiles
		sharedReferenceToConcurrentLinkedQueueOfArchiveFiles = concurrentLinkedQueueOfArchiveFiles;
		
		//Create a MimetypeDetector
		mimetypeDetector = new MimetypeDetector();
		
		//Each ArchiveFileProcessorRunnable has its own unique connection to MySQL.
		//Initialize the one and only database connection for this instance.
		this.mySQLConn = MySQLHelper.getNewDBConnection(false);
		
		this.sitesMap = MySQLHelper.getSitesMap(null);
		this.relatedHostsMap = MySQLHelper.getRelatedHostsMap(null);
		
		try {
			setupMainInsertPreparedStatement();
		} catch (SQLException e) {
			HrwaManager.writeToLog("An SQLException occurred while calling preparing mainInsertPstmt.", true, HrwaManager.LOG_TYPE_ERROR);
			e.printStackTrace();
			System.exit(0);
		}

	}
	
	public int getUniqueRunnableId() {
		return this.uniqueRunnableId;
	}

	public void run() {
		
		HrwaManager.writeToLog("Thread " + getUniqueRunnableId() + " has started!", true, HrwaManager.LOG_TYPE_STANDARD);
		
		File latestFileToProcess;
		
		while((latestFileToProcess = sharedReferenceToConcurrentLinkedQueueOfArchiveFiles.poll()) != null) {
			
			try {
			
				while(HrwaManager.getCurrentAppMemoryUsageInBytes() > HrwaManager.maxMemoryThresholdInBytesForStartingNewThreadProcesses) {
					
					//If current memory usage is too high, wait until it's lower before processing another file on this thread
					try {
						Thread.sleep(5000);
						System.out.println("Thread " + this.uniqueRunnableId + ": sleeping for 5 seconds because current memory usage is too high to safely start an additional simutaneous file process.  Current memory usage: " + HrwaManager.bytesToMegabytes(HrwaManager.getCurrentAppMemoryUsageInBytes()) + " MB");
					}
					catch (InterruptedException e) { e.printStackTrace(); }
				}
				
				if( ! HrwaManager.previewMode ) {
					
					if(MySQLHelper.archiveFileHasAlreadyBeenFullyIndexedIntoMySQL(latestFileToProcess.getName())) {
						HrwaManager.writeToLog("Skipping the MySQL indexing of file (" + latestFileToProcess.getName() + ") because it has already been fully indexed", true, HrwaManager.LOG_TYPE_NOTICE);
					} else {
						//This archive file has NOT been fully indexed into MySQL.
						//To ensure that we don't have any partially-indexed records in MySQL, we'll delete any partially indexed records from this file.
						//This will allow us to safely stop and start the ArchiveToMySQLTask any time.
						MySQLHelper.deleteWebArchiveRecordsByFile(latestFileToProcess.getName());
						processArchiveFile(latestFileToProcess);
					}
				} else {
					HrwaManager.writeToLog("PREVIEWING the MySQL indexing of file (" + latestFileToProcess.getName() + "). No actual database changes will be made.", true, HrwaManager.LOG_TYPE_NOTICE);
				}
				
			} catch (Exception ex) {
				//Catching any random uncaught exception related to this file so that one bad file doesn't crash this thread
				HrwaManager.writeToLog("An unexpected error occurred while processing the file [" + latestFileToProcess.getName() + "].  Moving onto the next file.", true, HrwaManager.LOG_TYPE_ERROR);
			}
			
		}
		
		try {
			closeMainInsertPreparedStatement();
		} catch (SQLException e) {
			HrwaManager.writeToLog("An SQLException occurred while trying to close mainInsertPstmt.", true, HrwaManager.LOG_TYPE_ERROR);
			e.printStackTrace();
			System.exit(0);
		}
		
		HrwaManager.writeToLog("Thread " + getUniqueRunnableId() + " has stopped!", true, HrwaManager.LOG_TYPE_STANDARD);
	}
	
	private void addNewArchiveFileToFullyIndexedArchiveFilesTable(String archiveFileName) {
		
		try {
			PreparedStatement pstmt = this.mySQLConn.prepareStatement("INSERT INTO " + HrwaManager.MYSQL_FULLY_INDEXED_ARCHIVE_FILES_TABLE_NAME + " (archive_file_name, crawl_year_and_month) VALUES (?,?);");
			pstmt.setString(1, archiveFileName);
			pstmt.setString(2, HrwaManager.getCaptureYearAndMonthStringFromArchiveFileName(archiveFileName));

			pstmt.execute();
		
			this.mySQLConn.commit(); //need to commit explicitly because auto-commit is turned off for this.mySQLConn
		} catch (SQLException e) {
			HrwaManager.writeToLog("An SQL error occurred while attempting to add the following archive file to the fully indexed archive files table" + archiveFileName + "\n" + e.getMessage(), true, HrwaManager.LOG_TYPE_ERROR);
			e.printStackTrace();
			System.exit(HrwaManager.EXIT_CODE_ERROR);
		}
	}
	
	public void processArchiveFile(File archiveFile) {
		
		HrwaManager.writeToLog("Thread " + this.getUniqueRunnableId() + ": Start process of archive file " + archiveFile.getName(), true, HrwaManager.LOG_TYPE_STANDARD);
		
		//We need to get an ArchiveReader for this file
		ArchiveReader archiveReader = null;
		
		try {
			
			archiveReader = ArchiveReaderFactory.get(archiveFile);
		
			archiveReader.setDigest(true);

			// Wrap archiveReader in NutchWAX ArcReader class, which converts WARC
			// records to ARC records on-the-fly, returning null for any records that
			// are not WARC-Type "response".
			ArcReader arcReader = new ArcReader(archiveReader);
			
			String archiveFileName = archiveFile.getName();
			
			// Loop through all archive records in this file
			for (ARCRecord arcRecord : arcReader) {
				
				if (arcRecord == null) {
					// WARC records that are not of WARC-Type response will be set to null, and we don't want to analyze these.
					// Need to check for null.
					//HrwaManager.writeToLog("Notice: Can't process null record in " + archiveFile.getPath(), true, HrwaManager.LOG_TYPE_NOTICE);
				}
				else if(arcRecord.getMetaData().getUrl().startsWith("dns:")) {
					//Do not do mimetype/lang analysis on DNS records
					//HrwaManager.writeToLog("Notice: Skipping DNS record in " + archiveFile.getPath(), true, HrwaManager.LOG_TYPE_NOTICE);
					//HrwaManager.writeToLog(HrwaManager.getCurrentAppMemoryUsage(), true, HrwaManager.LOG_TYPE_MEMORY);
				}
				else
				{
					this.processSingleArchiveRecord(arcRecord, archiveFileName);
					
					this.numRelevantArchiveRecordsProcessed++;
					if(HrwaManager.verbose) {
						System.out.println("Thread " + uniqueRunnableId + " records processed: " + this.numRelevantArchiveRecordsProcessed);
					}
				}
			}
			
			try {
				executeAndCommitLatestRecordBatch(); //make sure to commit any remaining records that didn't get committed as part of a regular batch!
			} catch (SQLException e) {
				HrwaManager.writeToLog("An error occurred while attempting to commit the last batch of records for archive file: " + archiveFileName + "\n" + e.getMessage(), true, HrwaManager.LOG_TYPE_ERROR);
				e.printStackTrace();
				System.exit(HrwaManager.EXIT_CODE_ERROR);
			}
			
			addNewArchiveFileToFullyIndexedArchiveFilesTable(archiveFileName);
		
		} catch (IOException e) {
			HrwaManager.writeToLog("An error occurred while trying to read in the archive file at " + archiveFile.getPath() + "\n" + e.getMessage(), true, HrwaManager.LOG_TYPE_ERROR);
			HrwaManager.writeToLog("Skipping " + archiveFile.getPath() + " due to the read error. Moving on to the next file...", true, HrwaManager.LOG_TYPE_ERROR);
		} finally {
			try {
				archiveReader.close();
			} catch (IOException e) {
				HrwaManager.writeToLog("An error occurred while trying to close the ArchiveReader for file: " + archiveFile.getPath() + "\n" + e.getMessage(), true, HrwaManager.LOG_TYPE_ERROR);
				e.printStackTrace();
			}
		}
		
		HrwaManager.writeToLog("Thread " + this.getUniqueRunnableId() + ": Processing of archive file " + archiveFile.getName() + " COMPLETE!", true, HrwaManager.LOG_TYPE_STANDARD);
		
	}
	
	public long getNumRelevantArchiveRecordsProcessed() {
		return this.numRelevantArchiveRecordsProcessed;
	}
	
	private void processSingleArchiveRecord(ARCRecord arcRecord, String parentArchiveFileName) {
		//Important! Get http header before running record.skipHttpHeader()
		String httpHeaderString = StringUtils.join(arcRecord.getHttpHeaders());
		
		try {
			arcRecord.skipHttpHeader(); //This advances the read pointer to the blob portion of the archive record, excluding the header
		} catch (IOException ex) {
			HrwaManager.writeToLog("Error: Cannot skip header in archive record", true, HrwaManager.LOG_TYPE_ERROR);
		}
	
		ARCRecordMetaData arcRecordMetaData = arcRecord.getMetaData();
		
		String pathToBlobFile;
		String detectedMimetype;
		
		//Only create blob file and do mimetype detection for records with a 200 (success) status
		if(arcRecord.getStatusCode() == 200) {
			//Step 1: Create .blob file and .blob.header file
			File newlyCreatedBlobFile = createBlobAndHeaderFilesForRecord(arcRecord, arcRecordMetaData, httpHeaderString, parentArchiveFileName);
			
			//Step 2: Run mimetype detection on blob file - Mimetype detection with Tika >= 1.2 is thread-safe.
			detectedMimetype = mimetypeDetector.getMimetype(newlyCreatedBlobFile);
			//System.out.println("Detected mimetype: " + detectedMimetype);
			
			pathToBlobFile = newlyCreatedBlobFile.getPath();
		} else {
			pathToBlobFile = null;
			detectedMimetype = null;
		}
		
		//Step 3: If this archive record has no digest, create a digest.
		//IMPORTANT NOTE: DO NOT close the record until after you've already extracted blob/header info from it.
		
		// If there is no digest, then we assume we're reading an
        // ARCRecord and not a WARCRecord.  In that case, we close the
        // record, which updates the digest string.  Then we tweak the
        // digest string so we have the same for for both ARC and WARC
        // records.
        if ( arcRecordMetaData.getDigest() == null ) {
        	try {
        		arcRecord.close();
			} catch (IOException e) {
				HrwaManager.writeToLog("Error: Could not close current record while attempting to assign digest. Record from archive file: " + parentArchiveFileName, true, HrwaManager.LOG_TYPE_ERROR);
			}

            // ARC and WARC records produce two slightly different
        	// digest formats.  WARC record digests have the algorithm
        	// name as a prefix, such as
            // "sha1:PD3SS4WWZVFWTDC63RU2MWX7BVC2Y2VA" but
            // ArcRecord.getDigestStr() does not.  Since we want the
            // formats to match, we prepend the "sha1:" prefix to ARC
            // record digest.
        	arcRecordMetaData.setDigest( "sha1:" + arcRecord.getDigestStr() );
        }
		
		//Step 3: Insert all info into MySQL
		try {
			insertRecordIntoMySQLArchiveRecordTable(arcRecord, arcRecordMetaData, detectedMimetype, parentArchiveFileName, pathToBlobFile);
		} catch (SQLException ex) {
			HrwaManager.writeToLog("An error occurred while attempting to insert a new archive record row into MySQL.", true, HrwaManager.LOG_TYPE_ERROR);
		}
	}
	
	public void insertRecordIntoMySQLArchiveRecordTable(ARCRecord arcRecord, ARCRecordMetaData arcRecordMetaData, String detectedMimetype, String parentArchiveFileName, String pathToBlobFile) throws SQLException {
		
		String recordIdentifier = arcRecordMetaData.getRecordIdentifier();
		String hoststring = HrwaManager.getHoststringFromUrl(arcRecordMetaData.getUrl());
		
		boolean linkedViaRelatedHost = false;
		
		int siteId = -1; //unless changed, this default -1 will translate to NULL when we do the database insert
		
		if (sitesMap.containsKey(hoststring)) {
			siteId = sitesMap.get(hoststring); //but we're hoping to match to a site_id
		}
		else if (relatedHostsMap.containsKey(hoststring)) {
			siteId = relatedHostsMap.get(hoststring); //and if we can't match to a site_id, we'll try matching to a related host
			linkedViaRelatedHost = true;
		}
		
		long loadTimestamp = System.currentTimeMillis()/1000L; //1000L because we want to use *long* divsion rather than *int* division.
		
		this.mainRecordInsertPstmt.setString(	1,  arcRecordMetaData.getIp()				);
		this.mainRecordInsertPstmt.setString(	2,  arcRecordMetaData.getUrl()				);
		this.mainRecordInsertPstmt.setString(	3,  arcRecordMetaData.getDigest()			);
		this.mainRecordInsertPstmt.setString(	4,  parentArchiveFileName           		);
		this.mainRecordInsertPstmt.setLong  (	5,  arcRecordMetaData.getOffset()			);
		this.mainRecordInsertPstmt.setLong  (   6,  arcRecordMetaData.getLength()           );
		this.mainRecordInsertPstmt.setString(	7,  arcRecordMetaData.getDate()				);
		if(pathToBlobFile == null) {
			this.mainRecordInsertPstmt.setNull( 8,  java.sql.Types.VARCHAR             		);
		} else {
			this.mainRecordInsertPstmt.setString( 8,  pathToBlobFile             			);
		}
		this.mainRecordInsertPstmt.setString(	9,  arcRecordMetaData.getMimetype()    		);
		if(detectedMimetype == null) {
			this.mainRecordInsertPstmt.setNull( 10,  java.sql.Types.VARCHAR             	);
		} else {
			this.mainRecordInsertPstmt.setString( 10, detectedMimetype      				);
		}
		this.mainRecordInsertPstmt.setString(	11, arcRecordMetaData.getReaderIdentifier()	);
		this.mainRecordInsertPstmt.setString(	12, recordIdentifier						);
		this.mainRecordInsertPstmt.setString(	13, ArchiveFileProcessorRunnable.ARCHIVED_URL_PREFIX + recordIdentifier);
		this.mainRecordInsertPstmt.setInt   (   14, arcRecord.getStatusCode()				);
		this.mainRecordInsertPstmt.setString(	15, hoststring								);
		if(siteId > -1) {
			this.mainRecordInsertPstmt.setInt(	16, siteId                					);
		} else {
			//Set site_id to NULL for records that aren't linked to a site
			this.mainRecordInsertPstmt.setNull(	16, java.sql.Types.INTEGER                	);
		}
		this.mainRecordInsertPstmt.setLong  (	17, loadTimestamp							);
		this.mainRecordInsertPstmt.setBoolean(	18, linkedViaRelatedHost					);
		
		if(siteId > -1) {
			this.mainRecordInsertPstmt.setString  (	19, MySQLHelper.HRWA_MANAGER_TODO_UPDATED );
		} else {
			//Set hrwa_manager_todo to NULL for records that aren't linked to a site
			this.mainRecordInsertPstmt.setNull    (	19, java.sql.Types.VARCHAR	);
		}
		
		this.mainRecordInsertPstmt.addBatch();
		if ((numRelevantArchiveRecordsProcessed + 1) % HrwaManager.mysqlCommitBatchSize == 0) {
			// Batch execute group size: HrwaManager.mysqlCommitBatchSize
			executeAndCommitLatestRecordBatch();
        }
	}
	
	public void executeAndCommitLatestRecordBatch() throws SQLException {
		this.mainRecordInsertPstmt.executeBatch();
		this.mySQLConn.commit();
	}
	
	public boolean isProcessingAnArchiveFile() {
		
		boolean bool;
		
		synchronized (isProcessingAnArchiveFile) {
			bool = isProcessingAnArchiveFile;
		}
		
		return bool;
	}
	
	/**
	 * Creates a blob file for this record on the file system.
	 * @param arcRecord
	 * @return The newly created blob File.
	 */
	private File createBlobAndHeaderFilesForRecord(ARCRecord arcRecord, ARCRecordMetaData arcRecordMetaData, String httpHeaderString, String arcRecordParentArchiveFileName) {
		
		String fullPathToNewBlobFile = getBlobFilePathForRecord(arcRecord, arcRecordMetaData, arcRecordParentArchiveFileName);
		
		File blobFile = new File(fullPathToNewBlobFile);

        //Step 1: Write out the .blob file

        //Make directories if necessary
        blobFile.getParentFile().mkdirs();

        
        // Write blob to file using method below in order to use less memory 
        try {
            FileOutputStream blobOutputStream = new FileOutputStream(blobFile);
            byte [] buffer = checkout();
            int len = 0;
            while ((len = arcRecord.read(buffer)) != -1) {
                blobOutputStream.write(buffer, 0, len);
            }
            blobOutputStream.flush();
            blobOutputStream.close();
            checkin(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            HrwaManager.writeToLog("Error: Could not write BLOB file (" + blobFile.getPath() + ") to disk.\n" + e.getMessage(), true, HrwaManager.LOG_TYPE_ERROR);
        }

        String blob_path_with_header_extension = blobFile.getPath() + ".header";

        //Write out the header string to an associated .blob.header file
        try {
            FileUtils.writeStringToFile(new File(blob_path_with_header_extension), httpHeaderString);
        } catch (IOException e) {
            e.printStackTrace();
            HrwaManager.writeToLog("Error: Could not write HEADER file (" + blob_path_with_header_extension + ") to disk.\n" + e.getMessage(), true, HrwaManager.LOG_TYPE_ERROR);
        }
        
		return blobFile;
	}
	
	private String getBlobFilePathForRecord(ARCRecord arcRecord, ARCRecordMetaData arcRecordMetaData, String arcRecordParentArchiveFileName) {
		
		//Get the captureYearAndMonthString from the name of this record's parent file
		String captureYearAndMonthString = HrwaManager.getCaptureYearAndMonthStringFromArchiveFileName(arcRecordParentArchiveFileName);
		
		return HrwaManager.blobDirPath + File.separator + arcRecordParentArchiveFileName + File.separator + arcRecordMetaData.getOffset() + ".blob";
	}
	
	//Only prepare the statement once.  No need to do it multiple times since we're inserting the same-formatted data over and over
	public void setupMainInsertPreparedStatement() throws SQLException {
	    this.mainRecordInsertPstmt = mySQLConn.prepareStatement (
                "INSERT INTO " + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME +
                " ( ip, url, digest, archive_file, offset_in_archive_file, " +
                "length, record_date, blob_path, mimetype_from_header, mimetype_detected, " +
                "reader_identifier, record_identifier, archived_url, status_code, hoststring, " +
                "site_id, load_timestamp, linked_via_related_host, " + MySQLHelper.HRWA_MANAGER_TODO_FIELD_NAME + ") " +
                "VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        );
	}
	
	public void closeMainInsertPreparedStatement() throws SQLException {
	    this.mainRecordInsertPstmt.close();
	}
	
	/* Byte check-in/check-out stuff */
	
	private static Deque<byte[]> BUFFER_POOL = new ArrayDeque<byte[]>();
	private static byte[] checkout() {
	    synchronized(BUFFER_POOL) {
	        if (BUFFER_POOL.isEmpty()) {
	            return new byte[8196];
	        } else {
	            return BUFFER_POOL.pop();
	        }
	    }
	}
	private static void checkin(byte[] buffer) {
	    synchronized(BUFFER_POOL) {
	        BUFFER_POOL.add(buffer);
	    }
	}
	
}
