package edu.columbia.ldpd.hrwa.solr;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;


public class SolrIndexer {
    public static final String EMAIL_SENDER_KEY = "EMAIL_SENDER";
    public static final String EMAIL_RECIPIENT_KEY = "EMAIL_RECIPIENT";
    public static final String ERROR_PATH_KEY = "ERROR_PATH";
    public static final String LOG_PATH_KEY = "LOG_PATH";
    public static final String BATCH_SIZE_KEY = "BATCH_SIZE";
    public static final String SOLR_UPDATE_URL_KEY = "SOLR_UPDATE_URL";
    private static final String COMMIT_MSG = "<commit />";
    private static Pattern SERVER_NAME = Pattern.compile("^https?://([^.]+)\\..*");
    String emailSender;
    String emailRecipients;
    String errorReportsPath;
    String logPath;
    String serverName;
    String solrUpdateUrl;
    int updateBatchSize = 100;
    DefaultHttpClient client;
    public SolrIndexer(Properties config){
        emailSender = config.getProperty(EMAIL_SENDER_KEY);
        emailRecipients = config.getProperty(EMAIL_RECIPIENT_KEY);
        errorReportsPath = config.getProperty(ERROR_PATH_KEY);
        logPath = config.getProperty(LOG_PATH_KEY);
        updateBatchSize = Integer.parseInt(config.getProperty(BATCH_SIZE_KEY));
        solrUpdateUrl = config.getProperty(SOLR_UPDATE_URL_KEY);
        serverName = getServerName(solrUpdateUrl);
        client = new DefaultHttpClient();
    }

    public void index(File solrDir){
        File[] files = solrDir.listFiles();
        int ctr = 0;
        int status = 0;
        
        int numIgnoredFiles = 0;
        
        for (File file:files){
        	
        	if(file.getName().equals(".DS_Store")) {
				//Ignore OSX .DS_Store files
        		numIgnoredFiles++;
				continue;
			}
        	
            status = log(file.getAbsolutePath(), post(file));
            if (status > 199 && status < 300){
                if ((++ctr % updateBatchSize) == 0){
                    try{
                        empty(post(COMMIT_MSG));
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                }
            } else {
            }
        }
        if ((ctr % updateBatchSize) != 0){
            try{
                empty(post(COMMIT_MSG));
            } catch (IOException e){
                e.printStackTrace();
            }
        }
        logSuccess("" + ctr + "/" + (files.length-numIgnoredFiles) + " records indexed");
        if (ctr != files.length) {
        }
    }

    private static void empty(HttpResponse response) throws IOException {
        EntityUtils.consume(response.getEntity());
    }

    private HttpResponse post(File content){
        return post(getEntity(content));
    }

    private HttpResponse post(String content){
        return post(getEntity(content));
    }

    private HttpResponse post(HttpEntity entity){
        HttpPost post = new HttpPost(solrUpdateUrl);
        post.addHeader("Content-type", "text/xml; charset=utf-8");
        post.setEntity(entity);
        HttpResponse response;
        try {
            response = client.execute(post);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return response;
    }

    private static final String NULL_RESP = "Null response returned from SOLR";

    private int log(String filePath, HttpResponse response) {
        if (response == null) {
            logError(filePath, NULL_RESP);
            return 999;
        }
        int code = response.getStatusLine().getStatusCode();
        if (code > 199 && code < 300) {
            logSuccess(filePath);
        } else {
            logError(filePath, response.getStatusLine().getReasonPhrase());
        }
        try {
            EntityUtils.consume(response.getEntity());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return code;
    }

    private void logSuccess(String filePath){
        System.out.println("Indexed " + filePath);
    }

    private void logError(String filePath, String reasonPhrase) {
        System.err.println("Error indexing " + filePath + ": " + reasonPhrase);
    }

    private static Charset utf8(){
        return Charset.forName("UTF-8");
    }

    private static Charset UTF8 = utf8();

    private static ContentType UTF8_XML = ContentType.create("text/xml",UTF8);


    private static HttpEntity getEntity(String content){
        try {
            return new StringEntity(content,"utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static HttpEntity getEntity(File content){
        return new FileEntity(content,UTF8_XML);
    }

    private static String getServerName(String updateUrl){
        Matcher m = SERVER_NAME.matcher(updateUrl);
        m.find();
        return m.group(1);
    }
    public static void main(String[] args){
        String url = "http://vorpal.cul.columbia.edu:8080/solr-3.6/fsf/update";
        String name = getServerName(url);
        System.out.println("Server name: " + name + " from " + url);
        url = "https://harding.cul.columbia.edu:8080/solr-4/fsf/update";
        name = getServerName(url);
        System.out.println("Server name: " + name + " from " + url);
    }
}
