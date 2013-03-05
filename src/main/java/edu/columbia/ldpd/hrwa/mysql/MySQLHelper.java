package edu.columbia.ldpd.hrwa.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import edu.columbia.ldpd.hrwa.ArchiveFileProcessorRunnable;
import edu.columbia.ldpd.hrwa.HrwaManager;
import edu.columbia.ldpd.hrwa.HrwaSiteRecord;

public class MySQLHelper {
	
	public static final String HRWA_MANAGER_FSF_TODO_DELETE = "DELETE";
	public static final String HRWA_MANAGER_FSF_TODO_NEW_SITE = "NEW_SITE";
	public static final String HRWA_MANAGER_FSF_TODO_UPDATED_NEEDS_SOLR_REINDEX = "UPDATED_NEEDS_SOLR_REINDEX";
	
	public static Connection getNewDBConnection(boolean autoCommit) {

		Connection newConn = null;

		//Step 1: Load MySQL Driver
		try {
            // This is a test to check that the driver is available.
			// The newInstance() call is a work around for some broken Java implementations.
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception ex) { System.err.println("Could not load the mysql driver!"); }

		//Step 2: Establish connection
		
		String url = "jdbc:mysql://" + HrwaManager.mysqlURL + "/" + HrwaManager.mysqlDatabase;
		try {
			newConn = DriverManager.getConnection(url, HrwaManager.mysqlUsername, HrwaManager.mysqlPassword);
		} catch (SQLException ex) {
			HrwaManager.writeToLog("Error: Could not connect to MySQL at url:" + url, true, HrwaManager.LOG_TYPE_ERROR);
		    System.out.println("SQLException: " + ex.getMessage());
		    System.out.println("SQLState: " + ex.getSQLState());
		    System.out.println("VendorError: " + ex.getErrorCode());
		    System.exit(HrwaManager.EXIT_CODE_ERROR);
		}

		try {
			newConn.setAutoCommit(autoCommit);
		} catch (SQLException e) { e.printStackTrace(); System.exit(HrwaManager.EXIT_CODE_ERROR); }

		return newConn;
	}
	
	public static void createWebArchiveRecordsTableIfItDoesNotExist() throws SQLException {

		Connection conn = getNewDBConnection(true);
		
		PreparedStatement pstmt0 = conn.prepareStatement(
			"CREATE TABLE IF NOT EXISTS `" + HrwaManager.MYSQL_WEB_ARCHIVE_RECORDS_TABLE_NAME + "` (" +
			"  `id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Auto-incremented unique numeric identifier for MySQL convenience.'," +
			"  `ip` varchar(39) NOT NULL COMMENT 'IPv4 or IPv6 address of the host of the record crawled.'," +
			"  `url` varchar(2100) NOT NULL COMMENT 'Original url of this crawled record.'," +
			"  `digest` char(37) DEFAULT NULL COMMENT 'SHA1 digest of the crawled record.'," +
			"  `archive_file` varchar(255) NOT NULL COMMENT 'Name of the archive file (warc/arc) that this record came from.'," +
			"  `offset_in_archive_file` bigint(20) unsigned NOT NULL COMMENT 'This is the byte offset address of the record in the archive file.'," +
			"  `length` bigint(20) unsigned NOT NULL COMMENT 'Size of the content returned in the HTTP response in bytes. Largest will probably be video.'," +
			"  `record_date` char(14) NOT NULL COMMENT 'Crawl date for this record.'," +
			"  `blob_path` varchar(500) NOT NULL COMMENT 'Filesystem path to the blob data associated with this record.  Header info is the blob_path + \".header\"'," +
			"  `mimetype_from_header` varchar(255) DEFAULT NULL COMMENT 'Mimetype defined in the archive file header.'," +
			"  `mimetype_detected` varchar(100) DEFAULT NULL COMMENT 'Mimetype detected by our archive_to_mysql indexer, using Apache Tika.  NULL if mimetype could not be detected.'," +
			"  `mimetype_code` varchar(100) DEFAULT NULL COMMENT 'Custom, human-made mimetype code that might (1) place several mimetypes into a logical group (i.e. Microsoft Excel Document) or (2) translate long, unwieldy mimetypes into shorter-named, cleaner-looking ones.'," +
			"  `reader_identifier` varchar(255) NOT NULL COMMENT 'Full filesystem path to the warc/arc file associated with this record (at the time when this record was indexed).'," +
			"  `record_identifier` varchar(2115) NOT NULL COMMENT 'Unique identifier for this record.  Of the format: record_date/url'," +
			"  `archived_url` varchar(2200) DEFAULT NULL COMMENT 'Wayback url to this archive record.  Note that this url includes the record_identifier.'," +
			"  `status_code` int(3) NOT NULL COMMENT 'HTTP response status code at record crawl time.'," +
			"  `hoststring` varchar(255) DEFAULT NULL COMMENT 'Truncated url, only including hostname and removing www, www1, www2, etc. if present.'," +
			"  `site_id` smallint(5) unsigned NOT NULL COMMENT 'Foreign key to sites table, linking this record to a recognized website (or to a catch-all unknown site record in the sites table).'," +
			"  `load_timestamp` bigint(20) NOT NULL COMMENT 'Timestamp indicating when this record was indexed and inserted into mysql.'," +
			"  `rd_subject` varchar(255) DEFAULT NULL COMMENT 'Rich document data field (if available): Subject information.'," +
			"  `rd_description` varchar(255) DEFAULT NULL COMMENT 'Rich document data field (if available): Description'," +
			"  `rd_comments` varchar(255) DEFAULT NULL COMMENT 'Rich document data field (if available): Comments'," +
			"  `rd_author` varchar(255) DEFAULT NULL COMMENT 'Rich document data field (if available): Author'," +
			"  `rd_keywords` varchar(255) DEFAULT NULL COMMENT 'Rich document data field (if available): Keywords'," +
			"  `rd_category` varchar(255) DEFAULT NULL COMMENT 'Rich document data field (if available): Category'," +
			"  `rd_content_type` varchar(255) DEFAULT NULL COMMENT 'Rich document data field (if available): Content type'," +
			"  `rd_last_modified` varchar(255) DEFAULT NULL COMMENT 'Rich document data field (if available): Last modified'," +
			"  `rd_links` varchar(255) DEFAULT NULL COMMENT 'Rich document data field (if available): Links'," +
			"  `hrwa_manager_asf_todo` varchar(50) DEFAULT NULL," +
			"  PRIMARY KEY (`id`)," +
			"  KEY `mimetype_from_header` (`mimetype_from_header`)," +
			"  KEY `mimetype_detected` (`mimetype_detected`)," +
			"  KEY `status_code` (`status_code`)," +
			"  KEY `hoststring` (`hoststring`)," +
			"  KEY `site_id` (`site_id`)," +
			"  KEY `mimetype_code` (`mimetype_code`)," +
			"  KEY `record_date` (`record_date`)," +
			"  KEY `hrwa_manager_asf_todo` (`hrwa_manager_asf_todo`)" +
			") ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=0;"
		);
		
		pstmt0.execute();
		pstmt0.close();
		
	}
	
	public static void createSitesTableIfItDoesNotExist() throws SQLException {
				
		Connection conn = getNewDBConnection(true);
		
		PreparedStatement pstmt0 = conn.prepareStatement(
			"CREATE TABLE IF NOT EXISTS `" + HrwaManager.MYSQL_SITES_TABLE_NAME + "` (" +
			"  `id` int(11) unsigned NOT NULL AUTO_INCREMENT," +
			"  `bib_key` char(7) NOT NULL," +
			"  `creator_name` varchar(255) NOT NULL," +
			"  `hoststring` varchar(255) NOT NULL," +
			"  `organization_type` varchar(255) NOT NULL," +
			"  `organization_based_in` varchar(255) NOT NULL," +
			"  `geographic_focus` varchar(2000) NOT NULL," +
			"  `language` varchar(2000) NOT NULL," +
			"  `original_urls` varchar(2000) NOT NULL," +
			"  `marc_005_last_modified` char(16) NOT NULL," +
			"  `hrwa_manager_fsf_todo` varchar(50) DEFAULT NULL," +
			"  PRIMARY KEY (`id`)," +
			"  UNIQUE KEY `bib_key` (`bib_key`)," +
			"  KEY `creator_name` (`creator_name`)," +
			"  KEY `organization_type` (`organization_type`)," +
			"  KEY `organization_based_in` (`organization_based_in`)," +
			"  KEY `hoststring` (`hoststring`)," +
			"  KEY `geographic_focus` (`geographic_focus`(255))," +
			"  KEY `language` (`language`(255))," +
			"  KEY `original_urls` (`original_urls`(255))," +
			"  KEY `hrwa_manager_fsf_todo` (`hrwa_manager_fsf_todo`)" +
			") ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=2;" //2 is important here! We want to save room for id=1 for 
		);
		
		pstmt0.execute();
		pstmt0.close();
		
		//And now we need to verify that there aren't any records in this table (if it already existed)
		
		PreparedStatement pstmt1 = conn.prepareStatement("SELECT COUNT(*) FROM " + HrwaManager.MYSQL_SITES_TABLE_NAME);
		ResultSet resultSet = pstmt1.executeQuery();
		if (resultSet.next()) {
			if(resultSet.getInt(1) == 0) {
				
				PreparedStatement pstmt2 = conn.prepareStatement(
						"INSERT INTO `" + HrwaManager.MYSQL_SITES_TABLE_NAME + "` " +
						"(`id`, `bib_key`, `creator_name`, `hoststring`, `organization_type`, `organization_based_in`, `geographic_focus`, `language`, `original_urls`, `marc_005_last_modified`) " +
						"VALUES (1, '', '', '[UNCATALOGED SITE]', '', '', '', '', '', '0000000000000000');"
				);
				
				pstmt2.execute();
				pstmt2.close();
			}
		 }
        
		pstmt1.close();
		
        conn.close();
		
	}
	
	public static void createFullyIndexedArchiveFilesTableIfItDoesNotExist() throws SQLException {

		Connection conn = getNewDBConnection(true);
		
		PreparedStatement pstmt0 = conn.prepareStatement(
			"CREATE TABLE IF NOT EXISTS `" + HrwaManager.MYSQL_FULLY_INDEXED_ARCHIVE_FILES_TABLE_NAME + "` (" +
			"`archive_file_name` varchar(255) NOT NULL," +
			"`crawl_year_and_month` int(7) NOT NULL," +
			"PRIMARY KEY (`archive_file_name`)," +
			"KEY `crawl_year_and_month` (`crawl_year_and_month`)" +
			") ENGINE=InnoDB DEFAULT CHARSET=utf8;"
		);
		
		pstmt0.execute();
		pstmt0.close();
		
	}
	
	public static void createMimetypeCodesTableIfItDoesNotExist() throws SQLException{

		Connection conn = getNewDBConnection(true);
		
		PreparedStatement pstmt0 = conn.prepareStatement(
			"CREATE TABLE IF NOT EXISTS `" + HrwaManager.MYSQL_MIMETYPE_CODES_TABLE_NAME + "` (" +
			"`mimetype_detected` varchar(100) DEFAULT NULL," +
			"`mimetype_code` varchar(100) DEFAULT NULL," +
			"UNIQUE KEY `mimetype_detected` (`mimetype_detected`)," +
			"KEY `mimetype_code` (`mimetype_code`)" +
			") ENGINE=MyISAM DEFAULT CHARSET=utf8;"
		);
		
		pstmt0.execute();
		pstmt0.close();
		
		//And now we need to verify that there aren't any records in this table (if it already existed)
		
		PreparedStatement pstmt1 = conn.prepareStatement("SELECT COUNT(*) FROM " + HrwaManager.MYSQL_MIMETYPE_CODES_TABLE_NAME);
		ResultSet resultSet = pstmt1.executeQuery();
		if (resultSet.next()) {
			if(resultSet.getInt(1) == 0) {
				
				PreparedStatement pstmt2 = conn.prepareStatement(
					"INSERT INTO `" + HrwaManager.MYSQL_MIMETYPE_CODES_TABLE_NAME + "` (`mimetype_detected`, `mimetype_code`) VALUES" +
					"('application/rsd+xml', 'DISCOVERY')," +
					"('application/x-mspublisher', 'OFFICE')," +
					"('application/xslt+xml', 'XML')," +
					"('image/tiff', 'IMAGE')," +
					"('application/vnd.ms-cab-compressed', 'BINARY')," +
					"('application/pdf', 'PDF')," +
					"('application/x-mobipocket-ebook', 'BINARY')," +
					"('audio/x-wav', 'AUDIO')," +
					"('application/vnd.ms-fontobject', 'OFFICE')," +
					"('video/mpeg', 'VIDEO')," +
					"('application/x-elc', 'BINARY')," +
					"('application/vnd.oasis.opendocument.text', 'DOCUMENT')," +
					"('audio/ogg', 'AUDIO')," +
					"('application/vnd.ms-word.document.macroenabled.12', 'DOCUMENT')," +
					"('image/x-raw-panasonic', 'IMAGE')," +
					"('application/xml-dtd', 'XML')," +
					"('application/x-msdownload', 'EXECUTABLE')," +
					"('image/png', 'IMAGE')," +
					"('text/html', 'HTML')," +
					"('image/svg+xml', 'IMAGE')," +
					"('application/x-hwp', 'BINARY')," +
					"('image/vnd.adobe.photoshop', 'IMAGE')," +
					"('application/x-silverlight-app', 'VIDEO')," +
					"('application/rss+xml', 'WEB')," +
					"('application/xhtml+xml', 'HTML')," +
					"('application/postscript', 'BINARY')," +
					"('application/x-gzip', 'BINARY')," +
					"('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'SPREADSHEET')," +
					"('application/javascript', 'WEB')," +
					"('image/gif', 'IMAGE')," +
					"('application/vnd.wordperfect', 'DOCUMENT')," +
					"('video/x-ms-asf', 'VIDEO')," +
					"('text/uri-list', 'WEB')," +
					"('application/vnd.openxmlformats-officedocument.presentationml.template', 'SLIDESHOW')," +
					"('application/vnd.ms-powerpoint.presentation.macroenabled.12', 'SLIDESHOW')," +
					"('application/vnd.google-earth.kml+xml', 'BINARY')," +
					"('application/vnd.oasis.opendocument.presentation', 'SLIDESHOW')," +
					"('application/xml', 'XML')," +
					"('application/vnd.ms-powerpoint', 'SLIDESHOW')," +
					"('application/DOCUMENT', 'DOCUMENT')," +
					"('audio/midi', 'AUDIO')," +
					"('application/vnd.apple.keynote', 'SLIDESHOW')," +
					"('application/x-msmetafile', 'BINARY')," +
					"('application/octet-stream', 'BINARY')," +
					"('application/rdf+xml', 'XML')," +
					"('text/plain', 'DOCUMENT')," +
					"('application/rtf', 'DOCUMENT')," +
					"('application/x-123', 'BINARY')," +
					"('image/vnd.ms-modi', 'IMAGE')," +
					"('application/x-font-printer-metric', 'BINARY')," +
					"('application/x-rar-compressed', 'BINARY')," +
					"('application/pls+xml', 'BINARY')," +
					"('video/x-ms-wmv', 'VIDEO')," +
					"('application/vnd.google-earth.kmz', 'BINARY')," +
					"('text/vnd.graphviz', 'BINARY')," +
					"('application/java-archive', 'BINARY')," +
					"('audio/basic', 'AUDIO')," +
					"('application/x-bittorrent', 'BINARY')," +
					"('video/quicktime', 'VIDEO')," +
					"('application/java-vm', 'BINARY')," +
					"('application/ogg', 'AUDIO')," +
					"('text/x-pascal', 'CODE')," +
					"('application/vnd.lotus-organizer', 'CALENDAR')," +
					"('application/x-font-ttf', 'WEB')," +
					"('text/x-c', 'CODE')," +
					"('text/x-asm', 'CODE')," +
					"('application/vnd.quark.quarkxpress', 'IMAGE')," +
					"('application/x-font-type1', 'BINARY')," +
					"('NULL', 'NULL')," +
					"('application/vnd.framemaker', 'BINARY')," +
					"('text/x-fortran', 'CODE')," +
					"('application/vnd.arastra.swi', 'IMAGE')," +
					"('video/mp4', 'VIDEO')," +
					"('video/x-m4v', 'VIDEO')," +
					"('application/resource-lists+xml', 'BINARY')," +
					"('application/vnd.adobe.air-application-installer-package+zip', 'EXECUTABLE')," +
					"('image/jpeg', 'IMAGE')," +
					"('message/rfc822', 'EMAIL')," +
					"('application/x-wais-source', 'DISCOVERY')," +
					"('image/x-icon', 'IMAGE')," +
					"('application/vnd.ms-tnef', 'EMAIL')," +
					"('application/vnd.dynageo', 'BINARY')," +
					"('audio/x-pn-realaudio', 'AUDIO')," +
					"('video/ogg', 'VIDEO')," +
					"('text/csv', 'SPREADSHEET')," +
					"('audio/x-ms-wma', 'AUDIO')," +
					"('text/css', 'WEB')," +
					"('video/x-flv', 'VIDEO')," +
					"('application/vnd.openxmlformats-officedocument.presentationml.presentation', 'OFFICE')," +
					"('application/x-sh', 'BINARY')," +
					"('application/atom+xml', 'WEB')," +
					"('application/x-tika-OFFICE', 'OFFICE')," +
					"('application/x-ms-wmz', 'BINARY')," +
					"('text/troff', 'DOCUMENT')," +
					"('application/zip', 'BINARY')," +
					"('application/json', 'WEB')," +
					"('video/x-f4v', 'VIDEO')," +
					"('application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'DOCUMENT')," +
					"('application/vnd.rn-realmedia', 'VIDEO')," +
					"('application/vnd.ms-excel', 'SPREADSHEET')," +
					"('application/x-shockwave-flash', 'WEB')," +
					"('application/vnd.openxmlformats-officedocument.presentationml.slideshow', 'SLIDESHOW')," +
					"('image/x-raw-sony', 'IMAGE')," +
					"('image/x-ms-bmp', 'IMAGE')," +
					"('audio/mpeg', 'AUDIO')," +
					"('application/mac-binhex40', 'BINARY')," +
					"('application/vnd.ms-excel.sheet.binary.macroenabled.12', 'SPREADSHEET')," +
					"('application/vnd.lotus-1-2-3', 'SPREADSHEET')," +
					"('application/epub+zip', 'BINARY')," +
					"('text/x-uuencode', 'BINARY')," +
					"('application/vnd.ms-pki.seccat', 'OFFICE')," +
					"('application/x-tika-ooxml', 'OFFICE')," +
					"('application/rls-services+xml', 'BINARY')," +
					"('video/x-msvideo', 'VIDEO')," +
					"('text/calendar', 'CALENDAR');"
				);
				
				pstmt2.execute();
				pstmt2.close();
			}
		 }
        
		pstmt1.close();
		
        conn.close();
	}
	
	/**
	 * For latest version of related hosts table, see:
	 * https://wiki.cul.columbia.edu/display/webresourcescollection/Related+hosts+for+HRWA+portal
	 * @throws SQLException
	 */
	public static void createRelatedHostsTableIfItDoesNotExist() throws SQLException{

		Connection conn = getNewDBConnection(true);
		
		PreparedStatement pstmt0 = conn.prepareStatement(
			"CREATE TABLE IF NOT EXISTS `" + HrwaManager.MYSQL_RELATED_HOSTS_TABLE_NAME + "` (" +
			"  `site_id` int(10) unsigned NOT NULL," +
			"  `related_host` varchar(255) NOT NULL," +
			"  UNIQUE KEY `related_host` (`related_host`)," +
			"  KEY `site_id` (`site_id`)" +
			") ENGINE=InnoDB DEFAULT CHARSET=utf8;"
		);
		
		pstmt0.execute();
		pstmt0.close();
		
		//And now we need to verify that there aren't any records in this table (if it already existed)
		
		PreparedStatement pstmt1 = conn.prepareStatement("SELECT COUNT(*) FROM " + HrwaManager.MYSQL_RELATED_HOSTS_TABLE_NAME);
		ResultSet resultSet = pstmt1.executeQuery();
		if (resultSet.next()) {
			if(resultSet.getInt(1) == 0) {
				
				HashMap<String, String> seedsToRelatedHostsMap = new HashMap<String, String>();
				
				seedsToRelatedHostsMap.put("achrs.org", "www.achrs.com");
				seedsToRelatedHostsMap.put("amnistia.org.mx", "alzatuvoz.org");
				seedsToRelatedHostsMap.put("bahrainrights.org", "bahrainrights.hopto.org/ar");
				seedsToRelatedHostsMap.put("camp.org.pk", "www.understandingfata.org");
				seedsToRelatedHostsMap.put("child-soldiers.org", "childsoldiersglobalreport.org");
				seedsToRelatedHostsMap.put("chrrmw.org", "chrr.ultinets.net");
				seedsToRelatedHostsMap.put("crisisgroup.org", "www.crisisgroup.be");
				seedsToRelatedHostsMap.put("eaaf.typepad.com", "eaaf.typepad.org");
				seedsToRelatedHostsMap.put("eaaf.typepad.com", "eaaf.org/eaaf/styles");
				seedsToRelatedHostsMap.put("fohrid.org.np", "www.fohrid.org/dwn/");
				seedsToRelatedHostsMap.put("hrc.co.nz", "hrc.co.nz.woopra-ns.com");
				seedsToRelatedHostsMap.put("hrforumzim.org", "hrforumzim.huritech.org");
				seedsToRelatedHostsMap.put("humanrights.dk", "menneskeret.dk");
				seedsToRelatedHostsMap.put("ifhhro.org", "ifhhro-training-manual.org");
				seedsToRelatedHostsMap.put("minorityrights.org", "minorityrights.wordpress.com");
				seedsToRelatedHostsMap.put("observatorio.cl", "observatorio.cl.pampa.avnam.net");
				seedsToRelatedHostsMap.put("observatorio.cl", "www.monitoreandoderechos.cl");
				seedsToRelatedHostsMap.put("ombudsman.mk", "smultimedia.dynalias.net/Ombudsman/");
				seedsToRelatedHostsMap.put("ombudsman.mk", "ombudsman.mk/upload/documents/");
				seedsToRelatedHostsMap.put("physiciansforhumanrights.org", "phrtorturepapers.org");
				seedsToRelatedHostsMap.put("publicverdict.org", "eng.publicverdict.ru");
				seedsToRelatedHostsMap.put("rdc-viol.org", "www.rdcviolencesexuelle.org");
				seedsToRelatedHostsMap.put("savetibet.org", "www.savetibet.fr");
				seedsToRelatedHostsMap.put("savetibet.org", "savetibet.nl");
				seedsToRelatedHostsMap.put("savetibet.org", "www.liaowangxizang.net");
				seedsToRelatedHostsMap.put("savetibet.org", "savetibet.us");
				seedsToRelatedHostsMap.put("savetibet.org", "www.savetibet.de");
				seedsToRelatedHostsMap.put("shovrimshtika.org", "breakingthesilence.org.il");
				seedsToRelatedHostsMap.put("sova-center.ru", "sova-center.livejournal.com");
				seedsToRelatedHostsMap.put("sudanhumanrights.blogspot.com", "sudanmonitor.blogspot.com");
				seedsToRelatedHostsMap.put("woeser.middle-way.net", "woeser-weise.blogspot.com");
				
				//Link site table ids to related host values
				
				HashMap<String, Integer> sitesToSiteIdsMap = MySQLHelper.getSitesMap(); 
				
				String relatedHostRecordsInsertStatement = "INSERT INTO `" + HrwaManager.MYSQL_RELATED_HOSTS_TABLE_NAME + "` (`site_id`, `related_host`) VALUES";
				
				int siteId;
				String relatedHost;
				for(Map.Entry<String, String> entry : seedsToRelatedHostsMap.entrySet()) {
					if(sitesToSiteIdsMap.containsKey(entry.getKey())) {
						siteId = sitesToSiteIdsMap.get(entry.getKey());
						relatedHost = entry.getValue();
						
						//For safety, make sure that the relatedHost doesn't contain any single quotes
						if(relatedHost.contains("'")) {
							HrwaManager.writeToLog("Error: Single quotation mark found in related host (" + relatedHost + "). This will lead to an invalid MySQL insert statement.", true, HrwaManager.LOG_TYPE_ERROR);
							System.exit(HrwaManager.EXIT_CODE_ERROR);
						}
						
						relatedHostRecordsInsertStatement += "(" + siteId + ", '" + relatedHost + "'),";
					} else {
						//Write out error if this related host cannot be linked to an existing site string
						HrwaManager.writeToLog("Error: Could not find site in sites table (" + entry.getKey() + "), so there was no record to link to this related host (" + entry.getValue() + ").", true, HrwaManager.LOG_TYPE_ERROR);
						System.exit(HrwaManager.EXIT_CODE_ERROR);
					}
				}
				
				//And remove the last comma because it's not valid
				relatedHostRecordsInsertStatement = relatedHostRecordsInsertStatement.substring(0, relatedHostRecordsInsertStatement.length()-1);
				
				PreparedStatement pstmt2 = conn.prepareStatement(relatedHostRecordsInsertStatement);
				
				pstmt2.execute();
				pstmt2.close();
			}
		 }
        
		pstmt1.close();
		
        conn.close();
	}
	
	public static HashMap<String, Integer> getSitesMap() {
		
		HashMap<String, Integer> sitesMapToReturn = new HashMap<String, Integer>();
		
		try {
			Connection conn = MySQLHelper.getNewDBConnection(false);
			PreparedStatement pstmt = conn.prepareStatement("SELECT hoststring, id FROM " + HrwaManager.MYSQL_SITES_TABLE_NAME);
			ResultSet resultSet = pstmt.executeQuery();
			
			while (resultSet.next()) {
	
				//we know that hoststring == column 1 and id == column 2 because of the query ordering
				sitesMapToReturn.put(resultSet.getString(1), resultSet.getInt(2));
			}
			
			resultSet.close();
	        pstmt.close();
	        conn.close();
		} catch (SQLException e) {
			HrwaManager.writeToLog("Error: Could not retrieve sites table records from DB", true, HrwaManager.LOG_TYPE_ERROR);
			e.printStackTrace();
			System.exit(0);
		}
        
        return sitesMapToReturn;
	}
	
	public static HashSet<String> getAllBibKeysFromMySQLSitesTable() {
		HashSet<String> bibKeysToReturn = new HashSet<String>();
		
		try {
			Connection conn = MySQLHelper.getNewDBConnection(false);
			PreparedStatement pstmt = conn.prepareStatement("SELECT bib_key FROM " + HrwaManager.MYSQL_SITES_TABLE_NAME);
			ResultSet resultSet = pstmt.executeQuery();
			
			while (resultSet.next()) {
	
				//we know that hoststring == column 1 and id == column 2 because of the query ordering
				bibKeysToReturn.add(resultSet.getString(1));
			}
			
			resultSet.close();
	        pstmt.close();
	        conn.close();
		} catch (SQLException e) {
			HrwaManager.writeToLog("Error: Could not retrieve bib_keys from sites table.", true, HrwaManager.LOG_TYPE_ERROR);
			e.printStackTrace();
			System.exit(0);
		}
        
        return bibKeysToReturn;
	}
	
	public static HashMap<String, String> getAllBibKeysAndMarc005LastModifiedStringsFromMySQLSitesTable() {
		HashMap<String, String> bibKeysAndMarc005LastMofifiedStringsToReturn = new HashMap<String, String>();
		
		try {
			Connection conn = MySQLHelper.getNewDBConnection(false);
			PreparedStatement pstmt = conn.prepareStatement("SELECT bib_key, marc_005_last_modified FROM " + HrwaManager.MYSQL_SITES_TABLE_NAME);
			ResultSet resultSet = pstmt.executeQuery();
			
			while (resultSet.next()) {
	
				//we know that hoststring == column 1 and id == column 2 because of the query ordering
				bibKeysAndMarc005LastMofifiedStringsToReturn.put(resultSet.getString(1), resultSet.getString(2));
			}
			
			resultSet.close();
	        pstmt.close();
	        conn.close();
		} catch (SQLException e) {
			HrwaManager.writeToLog("Error: Could not retrieve bib_keys from sites table.", true, HrwaManager.LOG_TYPE_ERROR);
			e.printStackTrace();
			System.exit(0);
		}
        
        return bibKeysAndMarc005LastMofifiedStringsToReturn;
	}
	
	public static void addOrUpdateHrwaSiteRecordsInMySQLSitesTable(ArrayList<HrwaSiteRecord> hrwaSiteRecordToAddOrUpdate) {
		
		//Get all bib_key/marc_055_last_modified data from the sites table
		HashMap<String, String> bibKeysAndMarc005LastModifiedStrings = MySQLHelper.getAllBibKeysAndMarc005LastModifiedStringsFromMySQLSitesTable();
		
		ArrayList<HrwaSiteRecord> existingRrecordsToUpdate = new ArrayList<HrwaSiteRecord>();
		ArrayList<HrwaSiteRecord> newRecordsToAdd = new ArrayList<HrwaSiteRecord>();
		
		for(HrwaSiteRecord hsRecord : hrwaSiteRecordToAddOrUpdate) {
			if(bibKeysAndMarc005LastModifiedStrings.containsKey(hsRecord.getSingleValuedFieldValue("bib_key"))) {
				//This record exists in the site table.
				//Does it need to be updated?
				if( ! bibKeysAndMarc005LastModifiedStrings.get( hsRecord.getSingleValuedFieldValue("bib_key")).equals(hsRecord.getSingleValuedFieldValue("marc_005_last_modified")) ) {
					//marc_005_last_modified values do not match!  We need to update!
					existingRrecordsToUpdate.add(hsRecord);
				}
			} else {
				//This is a new record
				newRecordsToAdd.add(hsRecord);
			}
		}
		
		if(existingRrecordsToUpdate.size() > 0 || newRecordsToAdd.size() > 0) {
		
			try {
				
				Connection conn = MySQLHelper.getNewDBConnection(false);
				
				if(newRecordsToAdd.size() > 0) {
					//Add new records
					PreparedStatement pstmt1 = conn.prepareStatement("INSERT INTO " + HrwaManager.MYSQL_SITES_TABLE_NAME + " () VALUES ()");
					
					pstmt1.close();
				}
				
				if(existingRrecordsToUpdate.size() > 0) {
					//Update existing records
					
					PreparedStatement pstmt2 = conn.prepareStatement("UPDATE " + HrwaManager.MYSQL_SITES_TABLE_NAME + " SET a=?, b=? WHERE bib_key = ?");
					
					pstmt2.close();
				}
				
		        conn.close();
			} catch (SQLException e) {
				HrwaManager.writeToLog("Error: Could not retrieve records from HRWA MySQL related hosts table", true, HrwaManager.LOG_TYPE_ERROR);
				e.printStackTrace();
				System.exit(0);
			}
			
		}
	}

	public static HashMap<String, Integer> getRelatedHostsMap() {
		
		HashMap<String, Integer> relatedHostsMapToReturn = new HashMap<String, Integer>();

		try {
			Connection conn = MySQLHelper.getNewDBConnection(false);
			PreparedStatement pstmt = conn.prepareStatement("SELECT related_host, site_id FROM " + HrwaManager.MYSQL_RELATED_HOSTS_TABLE_NAME);
			ResultSet resultSet = pstmt.executeQuery();
			
			while (resultSet.next()) {
				//we know that hoststring == column 1 and id == column 2 because of the query ordering
				relatedHostsMapToReturn.put(resultSet.getString(1), resultSet.getInt(2));
		    }
			
			resultSet.close();
	        pstmt.close();
	        conn.close();
		} catch (SQLException e) {
			HrwaManager.writeToLog("Error: Could not retrieve records from HRWA MySQL related hosts table", true, HrwaManager.LOG_TYPE_ERROR);
			e.printStackTrace();
			System.exit(0);
		}
		
        return relatedHostsMapToReturn;
	}
	
	/**
	 * Marks sites in the Sites table as needing to be deleted.
	 * @param setOfBibKeysToMarkAsDeleted A HashSet containing String bib_keys associated with the sites that should be marked for deletion.
	 */
	public static void markSitesToBeDeleted(HashSet setOfBibKeysToMarkAsDeleted) {
		
		if(setOfBibKeysToMarkAsDeleted.size() > 0) {
			
			String commaDelimitedBibKeys = "'" + StringUtils.join(setOfBibKeysToMarkAsDeleted, "', '") + "'";
			
			try {
				Connection conn = MySQLHelper.getNewDBConnection(false);
				PreparedStatement pstmt = conn.prepareStatement("UPDATE " + HrwaManager.MYSQL_SITES_TABLE_NAME + " SET hrwa_manager_fsf_todo = '" + MySQLHelper.HRWA_MANAGER_FSF_TODO_DELETE + "' WHERE bib_key IN (" + commaDelimitedBibKeys + ");");
				pstmt.execute();
		        pstmt.close();
		        conn.close();
			} catch (SQLException e) {
				HrwaManager.writeToLog("Error: Could not mark sites to be deleted in HRWA MySQL sites tables", true, HrwaManager.LOG_TYPE_ERROR);
				e.printStackTrace();
				System.exit(0);
			}
		}
		
	}

}
