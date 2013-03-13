package edu.columbia.ldpd.hrwa.solr;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.handler.extraction.ExtractingParams;

import edu.columbia.ldpd.hrwa.HrwaManager;

/**
 * It is very important that this class is entirely static. We do not want more
 * than one instance of an HttpSolrServer object connecting to the ASF solr URL.
 * 
 * From http://wiki.apache.org/solr/Solrj:
 * 
 * HttpSolrServer is thread-safe and if you are using the following constructor,
 * you *MUST* re-use the same instance for all requests. If instances are
 * created on the fly, it can cause a connection leak. The recommended practice
 * is to keep a static instance of HttpSolrServer per solr server url and share
 * it for all requests. See https://issues.apache.org/jira/browse/SOLR-861 for
 * more details.
 */
public class ASFSolrIndexer {
	
	private static HttpSolrServer asfSolrServer;
	
	/* HttpSolrServer preferences*/
	public static void initSingleSolrServerObject() {
		asfSolrServer = new HttpSolrServer( HrwaManager.asfSolrUrl );
		asfSolrServer.setMaxRetries(1);
		asfSolrServer.setConnectionTimeout(5000);
		asfSolrServer.setDefaultMaxConnectionsPerHost(HrwaManager.maxUsableProcessors);
		asfSolrServer.setMaxTotalConnections(HrwaManager.maxUsableProcessors);
		
		performTestSolrServerPing(); //Make sure that we can connect to Solr.
	}
	
	public static void performTestSolrServerPing() {
		try {
			SolrPingResponse pingResponse = asfSolrServer.ping();
			HrwaManager.writeToLog("Test ping response from " + HrwaManager.asfSolrUrl + ": " + pingResponse.toString(), true, HrwaManager.LOG_TYPE_STANDARD);
		} catch (SolrServerException e) {
			HrwaManager.writeToLog("Error: SolrServerException encountered while attempting to connect to ASF Solr server at: " + HrwaManager.asfSolrUrl, true, HrwaManager.LOG_TYPE_ERROR);
			e.printStackTrace();
		} catch (IOException e) {
			HrwaManager.writeToLog("Error: IOException encountered while attempting to connect to ASF Solr server at: " + HrwaManager.asfSolrUrl, true, HrwaManager.LOG_TYPE_ERROR);
			e.printStackTrace();
		}
	}
	
	/**
	 * Immediately commits the given batch of documents to Solr
	 * @param docBatch
	 * @throws IOException 
	 * @throws SolrServerException 
	 * @throws SQLException 
	 */
	public static void indexDocAndExtractMetadataToSolr(ResultSet resultSet) throws SolrServerException, IOException, SQLException {
				
		String recordDate = resultSet.getString( "record_date" );
		String mimetypeDetected   = resultSet.getString( "mimetype_detected" );
		File blobFile = new File(resultSet.getString( "blob_path" ));
		
		// Early exit if we can't find the blob file
		if ( ! blobFile.exists() ) {
			HrwaManager.writeToLog("Unable to index solr record because an expected blob file could not be found at: " + blobFile.getPath() , true, HrwaManager.LOG_TYPE_ERROR);
			return;
		}
				
		//We need to use a ContentStreamUpdateRequest so that we can use the extracting request handler
		ContentStreamUpdateRequest updateRequest = new ContentStreamUpdateRequest("/update/extract");
		updateRequest.addFile(blobFile, mimetypeDetected);
				
		ModifiableSolrParams modifiableSolrParams = new ModifiableSolrParams();
		
		//Single-valued Solr fields
		modifiableSolrParams.add(ExtractingParams.LITERALS_PREFIX + "archived_url", 			resultSet.getString( "archived_url" ));
		modifiableSolrParams.add(ExtractingParams.LITERALS_PREFIX + "date_of_capture_yyyy", 	recordDate.substring( 0, 4 ));
		modifiableSolrParams.add(ExtractingParams.LITERALS_PREFIX + "date_of_capture_yyyymm", 	recordDate.substring( 0, 6 ));
		modifiableSolrParams.add(ExtractingParams.LITERALS_PREFIX + "date_of_capture_yyyymmdd",	recordDate.substring( 0, 8 ));
		modifiableSolrParams.add(ExtractingParams.LITERALS_PREFIX + "digest", 					resultSet.getString( "digest" ));
		modifiableSolrParams.add(ExtractingParams.LITERALS_PREFIX + "filename", 				resultSet.getString( "archive_file" ));
		modifiableSolrParams.add(ExtractingParams.LITERALS_PREFIX + "length", 					resultSet.getString( "length" ));
		modifiableSolrParams.add(ExtractingParams.LITERALS_PREFIX + "original_url", 			resultSet.getString( "url" ));
		modifiableSolrParams.add(ExtractingParams.LITERALS_PREFIX + "mimetype", 				mimetypeDetected);
		modifiableSolrParams.add(ExtractingParams.LITERALS_PREFIX + "mimetype_code", 			resultSet.getString( "mimetype_code" ));
		modifiableSolrParams.add(ExtractingParams.LITERALS_PREFIX + "reader_identifier",		resultSet.getString( "reader_identifier" ));
		modifiableSolrParams.add(ExtractingParams.LITERALS_PREFIX + "record_date", 				resultSet.getString( "record_date" ));
		modifiableSolrParams.add(ExtractingParams.LITERALS_PREFIX + "record_identifier", 		resultSet.getString( "record_identifier" ));
		modifiableSolrParams.add(ExtractingParams.LITERALS_PREFIX + "status_code", 				resultSet.getString( "status_code" ));
		modifiableSolrParams.add(ExtractingParams.LITERALS_PREFIX + "bib_key", 					resultSet.getString( "bib_key" ));
		modifiableSolrParams.add(ExtractingParams.LITERALS_PREFIX + "domain", 					resultSet.getString( "hoststring" ));
		modifiableSolrParams.add(ExtractingParams.LITERALS_PREFIX + "organization_type", 		resultSet.getString( "organization_type" ));
		modifiableSolrParams.add(ExtractingParams.LITERALS_PREFIX + "organization_based_in", 	resultSet.getString( "organization_based_in" ));			
		
		//Multi-valued Solr fields
		addMultivaluedLiteralParams(modifiableSolrParams, "creator_name", resultSet.getString( "creator_name" ));
		addMultivaluedLiteralParams(modifiableSolrParams, "geographic_focus", resultSet.getString( "geographic_focus" ));
		addMultivaluedLiteralParams(modifiableSolrParams, "language", resultSet.getString( "language" ));
		addMultivaluedLiteralParams(modifiableSolrParams, "website_original_urls", resultSet.getString( "original_urls" ));
		
		//"contents" field comes from extracting request handler (Solr Cell)
		
		// Save SolrCell the trouble of having to do mimetype detection (saves
		// time during Solr indexing/reindexing).
		// We are matching Solr server's Tika version (Tika 1.2) so the mimetype
		// we pass should be equivalent to the mimetype Solr would detect.
		modifiableSolrParams.add( "stream.type", mimetypeDetected );
		
		// Q: Why not set literal.title?
		// A: We are using the value from metadata as opposed to <title> tag
		// from the content XHTML
		// because it appears to be more reliable. See Tika extract samples in
		// wiki:
		// https://wiki.cul.columbia.edu/display/mellonhumanrights/Sample+responses+for+Excel%2C+HTML%2C+PDF%2C+Powerpoint%2C+and+Word+files
		
		updateRequest.setParams(modifiableSolrParams);
		//updateRequest.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);
		
		if(asfSolrServer.request(updateRequest) == null) {
			HrwaManager.writeToLog("Error: Could not upload file to solr: " + blobFile.getPath(), true, HrwaManager.LOG_TYPE_ERROR);
		}
	}
	
	public static void addMultivaluedLiteralParams(ModifiableSolrParams modifiableSolrParams, String solrFieldName, String multivaluedString) {
		String[] values = StringUtils.split(multivaluedString.substring(1) , HrwaManager.multiValuedFieldMySQLSeparatorPrefixChar);
		if ( null == values ) {
			return;
		}
		for ( String value : values ) {
			modifiableSolrParams.add( ExtractingParams.LITERALS_PREFIX + solrFieldName, value );
		}
	}
	
	/**
	 * Returns true on success, false on failure.
	 * @return
	 */
	public static boolean commit() {
		try {
			asfSolrServer.commit();
			return true;
		} catch (SolrServerException e) {
			HrwaManager.writeToLog("Error: SolrServerException encountered while attempting to commit a batch of documents to the ASF Solr server at: " + HrwaManager.asfSolrUrl, true, HrwaManager.LOG_TYPE_ERROR);
			e.printStackTrace();
		} catch (IOException e) {
			HrwaManager.writeToLog("Error: IOException encountered while attempting to commit a batch of documents to the ASF Solr server at: " + HrwaManager.asfSolrUrl, true, HrwaManager.LOG_TYPE_ERROR);
			e.printStackTrace();
		}
		
		return false;
	}
	
	/**
	 * Must call this method once we're done with the HttpSolrServer object.
	 * Closes the single HttpSolrServer connection. 
	 */
	public static void shutdownSingleSolrServerObject() {
		commit();
		asfSolrServer.shutdown();
	}
	
}
