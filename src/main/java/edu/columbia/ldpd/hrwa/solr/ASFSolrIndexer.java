package edu.columbia.ldpd.hrwa.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;

import org.apache.solr.common.SolrInputDocument;

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
	 */
	public static void commitBatchToSolr(Collection<SolrInputDocument> docBatch) throws SolrServerException, IOException {
		
		// Uing an UpdateRequest + setAction will allow is to immediately commit
		// this batch of documents to solr. No explicit call to asfSolrServer is
		// required.
		UpdateRequest req = new UpdateRequest();
		req.setAction( UpdateRequest.ACTION.COMMIT, false, false );
		req.add( docBatch );
		UpdateResponse rsp = req.process( asfSolrServer );
	}
	
	/**
	 * Must call this method once we're done with the HttpSolrServer object.
	 * Closes the single HttpSolrServer connection. 
	 */
	public static void shutdownSingleSolrServerObject() {
		asfSolrServer.shutdown();
	}
	
}
