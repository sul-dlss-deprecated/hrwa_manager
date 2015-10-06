package edu.columbia.ldpd.hrwa.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

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

public class DownloadArchiveFilesFromArchivitTask extends HrwaTask {
	
	private String archiveFileDownloadDomain = "https://partner.archive-it.org";
	private String archiveFileDownloadPageUrl = archiveFileDownloadDomain + "/cgi-bin/getarcs.pl?coll=" + HrwaManager.archiveItCollectionId;
	//HRWA Collection: 1068
	
	private static String pathToTempDownloadDir = HrwaManager.tmpDirPath + File.separatorChar + "downloads";

	public DownloadArchiveFilesFromArchivitTask() {
		
	}
	
	public void runTask() {
		
		writeTaskHeaderMessageAndSetStartTime();
		
		//Create temp download directory
		createTempDownloadDirectory();
		
		//Do actual task stuff
		performDownload();
		
		//Destroy temp download directory
		destroyTempDownloadDirectory();
		
		writeTaskFooterMessageAndPrintTotalTime();
		
	}
	
	private void performDownload() {
		
		ArrayList<HashMap<String, String>> filesToDownload = new ArrayList<HashMap<String, String>>();
		
		try {
			
		    WebClient webClient = webClientAuthenticator();
			
			//Get list of filesToDownload (which ignores already-downloaded files)
			filesToDownload = getListOfFilesToDownload(webClient);
			
			webClientCloseWindow(webClient);
			
			downloadArchiveFiles(filesToDownload);
		} catch (FailingHttpStatusCodeException e) {
			HrwaManager.writeToLog("Error: Unable to connect to ArchiveIt's site. Did you supply a valid username and password as command line arguments? If so, is the Archive-It site working properly? (archiveFileDownloadPageUrl: " + archiveFileDownloadPageUrl + ")", true, HrwaManager.LOG_TYPE_ERROR);
			e.printStackTrace();
		} catch (MalformedURLException e) {
			HrwaManager.writeToLog("Error: Unable to connect to ArchiveIt's site, MaleformedURL.\n"+convertStackTraceToString(e),true,HrwaManager.LOG_TYPE_ERROR);
		} catch (IOException e) {
			HrwaManager.writeToLog("Error: Unable to connect to ArchiveIt's site\n"+convertStackTraceToString(e),true,HrwaManager.LOG_TYPE_ERROR);
		}
	}

	private void webClientCloseWindow(WebClient webClient) {
		webClient.closeAllWindows();
	}

	private WebClient webClientAuthenticator() {
		WebClient webClient = new WebClient(BrowserVersion.FIREFOX_24);
		DefaultCredentialsProvider credentialsProvider = new DefaultCredentialsProvider();
		credentialsProvider.addCredentials(HrwaManager.archiveItUsername, HrwaManager.archiveItPassword);
		webClient.setCredentialsProvider(credentialsProvider);
		webClient.getOptions().setUseInsecureSSL(true);
		return webClient;
	}
	
	private ArrayList<HashMap<String, String>> getListOfFilesToDownload(WebClient webClient) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		
		HrwaManager.writeToLog("Retreiving list of possible archive files to download...", true, HrwaManager.LOG_TYPE_STANDARD);
		
		ArrayList<HashMap<String, String>> filesToDownload = new ArrayList<HashMap<String, String>>();
		
		HtmlPage page = webClient.getPage(archiveFileDownloadPageUrl);
		
		HrwaManager.writeToLog("Done retreiving list of possible archive files to download!\nAnalyzing list to determine which files have already been downloaded...", true, HrwaManager.LOG_TYPE_STANDARD);
		
		//Working with all relevant <tr> elements on this page (which are contained within the second table)
		DomElement targetTBodyElement = page.getElementsByTagName("tbody").get(1);
		targetTBodyElement.normalize();
		
		int numListedArchiveFilesFoundInHtml = 0;
		int trProgressCounter = 0;
		
		DomNodeList<HtmlElement> trElements = targetTBodyElement.getElementsByTagName("tr");
		int totalNumTrElementsToScanThrough = trElements.getLength();
		
		for(HtmlElement singleTrElement: trElements) {
			
			if( ! singleTrElement.asXml().contains(".gz") ) {
				//Ignore this tr because it doesn't contain info about an archive .gz file
				continue;
			}
			
			String fileName = null;
			String downloadUrl = null;
			String expectedMD5Hash = null;
			String captureYearAndMonthString = null;
			
			HtmlElement downloadInfoAnchorElement = null;
			HtmlElement md5InfoTrElement = null;
			
			DomNodeList<HtmlElement> tdElements = singleTrElement.getElementsByTagName("td");
			
			if(tdElements.size() != 2) {
				continue;
			}

			downloadInfoAnchorElement = tdElements.get(0).getElementsByTagName("a").get(0);
			md5InfoTrElement = tdElements.get(1);
			
			//System.out.println("downloadInfoAnchorElement: " + downloadInfoAnchorElement.asXml());
			//System.out.println("md5InfoTrElement: " + md5InfoTrElement.asXml());
			
			//Get file name and download url
			fileName = downloadInfoAnchorElement.getTextContent().trim();
			downloadUrl = downloadInfoAnchorElement.getAttribute("href").trim();
			expectedMD5Hash = md5InfoTrElement.getTextContent().trim();
			
			if( ! downloadUrl.startsWith("http") ) {
				downloadUrl = archiveFileDownloadDomain + downloadUrl;
			}
			
			//Extract crawl date from file name
			captureYearAndMonthString = HrwaManager.getCaptureYearAndMonthStringFromArchiveFileName(fileName);
			boolean isFileDownloaded = false;
			if(captureYearAndMonthString != null ) {
				
				//Finally, let's check to see if we've already downloaded this file.  If we have, then we don't want to add it to filesToDownload
				if( new File(getDestinationDirForArchiveFile(captureYearAndMonthString) + File.separator + fileName).exists() ) {
					isFileDownloaded = true;
				}  
					
				if(HrwaManager.requiredmonth.length()>0){
					if(captureYearAndMonthString.equalsIgnoreCase(HrwaManager.requiredmonth)){
						numListedArchiveFilesFoundInHtml++;
						if(isFileDownloaded){
							HrwaManager.writeToLog("No need to download file [" + downloadUrl + "] because it has already been downloaded.", true, HrwaManager.LOG_TYPE_NOTICE);
						} else {
							HashMap<String, String> archiveFileInfoMap = getArchiveDownloadFileInfo(
									fileName, downloadUrl, expectedMD5Hash,
									captureYearAndMonthString);
							filesToDownload.add(archiveFileInfoMap);
						}
					} // else the file is out of scope of the required month
				} else {
					// Check all the files for this collection
					if(isFileDownloaded){
						HrwaManager.writeToLog("No need to download file [" + downloadUrl + "] because it has already been downloaded.", true, HrwaManager.LOG_TYPE_NOTICE);
					} else {
						HashMap<String, String> archiveFileInfoMap = getArchiveDownloadFileInfo(
								fileName, downloadUrl, expectedMD5Hash,
								captureYearAndMonthString);
						filesToDownload.add(archiveFileInfoMap);
					}
					numListedArchiveFilesFoundInHtml++;
				}
			} else {
				HrwaManager.writeToLog("Error: Skipped file at [" + fileName + "] because a capture year/month combo could not be parsed from its name." , true, HrwaManager.LOG_TYPE_ERROR);
			}
			
			trProgressCounter++;
			
			System.out.println("Number of downloadable archive files found so far: " + numListedArchiveFilesFoundInHtml); //This doesn't need to be logged.
			System.out.println("Analyzing..." + (100*trProgressCounter/totalNumTrElementsToScanThrough) + "%"); //This doesn't need to be logged.
			System.out.println(HrwaManager.getCurrentAppRunTime()); //This doesn't need to be logged.
			
		}
		
		HrwaManager.writeToLog("Total number of downloadable archive files found: " + numListedArchiveFilesFoundInHtml, true, HrwaManager.LOG_TYPE_STANDARD);
		HrwaManager.writeToLog("Number of NEW archive files to download: " + filesToDownload.size(), true, HrwaManager.LOG_TYPE_STANDARD);
		
		return filesToDownload;
	}

	private HashMap<String, String> getArchiveDownloadFileInfo(String fileName,
			String downloadUrl, String expectedMD5Hash,
			String captureYearAndMonthString) {
		HashMap<String, String> archiveFileInfoMap = new HashMap<String, String>();
		
		archiveFileInfoMap.put("fileName", fileName);
		archiveFileInfoMap.put("downloadUrl", downloadUrl);
		archiveFileInfoMap.put("expectedMD5Hash", expectedMD5Hash);
		archiveFileInfoMap.put("captureYearAndMonthString", captureYearAndMonthString);
		return archiveFileInfoMap;
	}
	
	private void downloadArchiveFiles(ArrayList<HashMap<String, String>> listOfFilesToDownload)  {
		
		int numFilesToDownload = listOfFilesToDownload.size();
		int counter = 1;
		
		int successCount = 0;
		int failCount = 0;
		
		for(HashMap<String, String> singleArchiveFileInfo : listOfFilesToDownload) {
			// Download file to temp directory in case the download process
			// is interrupted, so that we're never left with incomplete files
			// in the final download directory.
			String tempFileDownloadLocation = DownloadArchiveFilesFromArchivitTask.pathToTempDownloadDir + File.separator + singleArchiveFileInfo.get("fileName");
			
			//There shouldn't be any file current at tempFileDownloadLocation, but let's check just in case someone moved a file there unintentionally
			if(new File(tempFileDownloadLocation).exists()) {
				//A file already exists at tempFileDownloadLocation.  This is not good, and should never happen.
				HrwaManager.writeToLog("Error: Could not create temp file at " + tempFileDownloadLocation + " because a file with the same name already exists there. Skipping download of this file (" + singleArchiveFileInfo.get("fileName") + ").", true, HrwaManager.LOG_TYPE_ERROR);
				failCount++;
				continue; //skip download of this file
			}
			
			HrwaManager.writeToLog("Download " + counter + " of " + numFilesToDownload + ": ", true, HrwaManager.LOG_TYPE_STANDARD);
			
			if( HrwaManager.previewMode ) {
				HrwaManager.writeToLog("PREVIEW NOTE: Pretending to download the file at " + singleArchiveFileInfo.get("downloadUrl") + "...", true, HrwaManager.LOG_TYPE_STANDARD);
				HrwaManager.writeToLog("PREVIEW NOTE: Pretend success!", true, HrwaManager.LOG_TYPE_STANDARD);
			}
			else {
				
				try {
					boolean encounterError = downloadFile( singleArchiveFileInfo.get("downloadUrl"), tempFileDownloadLocation, singleArchiveFileInfo.get("expectedMD5Hash"));
					//Create final destination directory
					String destinationDirectory = getDestinationDirForArchiveFile(singleArchiveFileInfo.get("captureYearAndMonthString")); 
					(new File(destinationDirectory)).mkdirs();
					
					//Move the fully-downloaded file from the temp directory to its permanent download location
					if( ! new File(tempFileDownloadLocation).renameTo(new File(destinationDirectory + File.separator + singleArchiveFileInfo.get("fileName"))) ) {
						//File could not be moved for some reason.  This is not good, and should never happen.
						HrwaManager.writeToLog("Error: For some reason, the fully-downloaded archive file at " + tempFileDownloadLocation + " could not be moved to its final destination. It will be left in the temp directory.", true, HrwaManager.LOG_TYPE_ERROR);
						failCount++;
					} else if(encounterError){
						failCount++;
					} else{
						successCount++;
					}
				} catch (Exception e) {
					HrwaManager.writeToLog("Error: Downloading "+singleArchiveFileInfo.get("downloadUrl")+"\n"+convertStackTraceToString(e),true,HrwaManager.LOG_TYPE_ERROR);
					failCount++;
				}
			}
			counter++;
			System.out.println(HrwaManager.getCurrentAppRunTime()); //This doesn't need to be logged.
		}
		HrwaManager.writeToLog("Completing downloading the files. ", true, HrwaManager.LOG_TYPE_STANDARD);
		HrwaManager.writeToLog("Total number of files: "+numFilesToDownload, true, HrwaManager.LOG_TYPE_STANDARD);
		HrwaManager.writeToLog("Successfully downloaded: "+successCount+"\nFailed to downloaded: "+failCount, true, HrwaManager.LOG_TYPE_STANDARD);
	}

	private String convertStackTraceToString(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}
	
	private String getDestinationDirForArchiveFile(String captureYearAndMonthString) {
		return HrwaManager.archiveFileDirPath + File.separator + captureYearAndMonthString;
	}
	
	private boolean downloadFile(String downloadUrl, String pathToDownloadLocation, String md5HashToValidateAgainst) throws FailingHttpStatusCodeException, MalformedURLException, IOException, NoSuchAlgorithmException {
		
		HrwaManager.writeToLog("Downloading file at " + downloadUrl, true, HrwaManager.LOG_TYPE_STANDARD);
		System.out.println("This may take a while...");
	    WebClient webClient = webClientAuthenticator();

		Page pageToDownload = webClient.getPage(downloadUrl);
		WebResponse response = pageToDownload.getWebResponse();
		
		long contentLengthFromHeader = Long.parseLong(
			(response.getResponseHeaderValue("Content-length") == null ?
			"0" : response.getResponseHeaderValue("Content-length"))
		);
		
		File archiveFile = new File(pathToDownloadLocation);
		archiveFile.createNewFile();
		FileOutputStream archiveFileOutputStream = new FileOutputStream(archiveFile);
		InputStream pageContentAsInputStream = response.getContentAsStream();
		IOUtils.copy(pageContentAsInputStream, archiveFileOutputStream);
		pageContentAsInputStream.close();
		archiveFileOutputStream.close();
		
		archiveFileOutputStream = null;
		pageContentAsInputStream = null;
		boolean encounteredError = true;
		
		if(contentLengthFromHeader > 0) {
			long sizeInBytesOfDownloadedFile = archiveFile.length();
			
			//Verify that the downloaded file size matches the header Content-Length value
			if(contentLengthFromHeader == sizeInBytesOfDownloadedFile) {
				HrwaManager.writeToLog("-- Header Content-Length matches downloaded file size.", true, HrwaManager.LOG_TYPE_STANDARD);

				
				FileInputStream fis = new FileInputStream(archiveFile);
				String md5HashOfDownloadedFile = computeMD5(fis);
				fis.close();
				
				md5HashToValidateAgainst = md5HashToValidateAgainst.toLowerCase(); //lower case supplied hash to validate against
				
				//Verify that the MD5 hash of the file matches the expected MD5 hash from Archive-It's web page				
				if(md5HashOfDownloadedFile.equals(md5HashToValidateAgainst)) {
					
					HrwaManager.writeToLog("-- Calculated MD5 digest matches expected MD5 digest (" + md5HashToValidateAgainst + ").", true, HrwaManager.LOG_TYPE_STANDARD);
					
					encounteredError = false;
					
				} else {
					HrwaManager.writeToLog("Error: Calculated MD5 digest does not match expected MD5 digest for file at: " + downloadUrl, true, HrwaManager.LOG_TYPE_ERROR);
					HrwaManager.writeToLog("-- Expected value: " + md5HashToValidateAgainst + ", Dynamically calculated value: " + md5HashOfDownloadedFile, true, HrwaManager.LOG_TYPE_ERROR);
				}
			}
			
		} else {
			HrwaManager.writeToLog("Error: Could not determine content length from http header for file at: " + downloadUrl, true, HrwaManager.LOG_TYPE_ERROR);
		}
		
		if(encounteredError) {
			HrwaManager.writeToLog("Download completed with errors. See error log for more details.", true, HrwaManager.LOG_TYPE_STANDARD);
		} else {
			HrwaManager.writeToLog("Download completed successfully.", true, HrwaManager.LOG_TYPE_STANDARD);
		}
		
		webClientCloseWindow(webClient);
		return encounteredError;
	}
	
	/**
	 * Computes the MD5 hash for the file defined by file input stream
	 * @param fis input stream for the file
	 * @return
	 */
	private String computeMD5(FileInputStream fis) throws java.io.IOException, NoSuchAlgorithmException{
		MessageDigest digest = MessageDigest.getInstance("MD5");
		
		byte[] bytesBuffer = new byte[1024];
		int bytesRead = -1;
		while ((bytesRead = fis.read(bytesBuffer)) != -1) {
			digest.update(bytesBuffer, 0, bytesRead);
		}
		fis.close();
		return convertByteArrayToHexString(digest.digest());
	}
	
	
	/**
	 * Converts the bytearray to String
	 * @param arrayBytes
	 * @return
	 */
	private static String convertByteArrayToHexString(byte[] arrayBytes) {
	    StringBuffer stringBuffer = new StringBuffer();
	    for (int i = 0; i < arrayBytes.length; i++) {
	        stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16)
	                .substring(1));
	    }
	    return stringBuffer.toString().toLowerCase();
	}

	/**
	 * Creates the temp download directory where in-progress downloads are placed.  
	 */
	private void createTempDownloadDirectory() {
		(new File(DownloadArchiveFilesFromArchivitTask.pathToTempDownloadDir)).mkdirs();
	}
	
	/**
	 * Checks to see if the temp download directory is empty,
	 * and deletes the directory if it is. If there are files
	 * in the directory, it will not be deleted and an error
	 * will be logged.
	 */
	private void destroyTempDownloadDirectory() {
		
		File tempDownloadDir = new File(DownloadArchiveFilesFromArchivitTask.pathToTempDownloadDir);
		
		if(tempDownloadDir.isDirectory()){
			if(tempDownloadDir.list().length > 0) {
				HrwaManager.writeToLog("Error: Cannot delete temp download dir (at " + DownloadArchiveFilesFromArchivitTask.pathToTempDownloadDir  + ") because it still contains one or more incomplete archive file downloads.", true, HrwaManager.LOG_TYPE_ERROR);
			} else {
				//Directory is empty.  We can delete it!
				tempDownloadDir.delete();
			}
		} else {
			HrwaManager.writeToLog("Error: Could not delete temp download dir at: " + DownloadArchiveFilesFromArchivitTask.pathToTempDownloadDir + ". Directory not found.", true, HrwaManager.LOG_TYPE_ERROR);
		}
	}
	
}
