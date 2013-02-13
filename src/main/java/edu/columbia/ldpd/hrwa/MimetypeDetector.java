package edu.columbia.ldpd.hrwa;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.tika.Tika;

public class MimetypeDetector {
	private final Tika tika;
	public MimetypeDetector() {
	    this.tika = new Tika();
	}
	
	/**
	 * Detect and return the mimetype of the supplied file InputStream. 
	 * @param inputStream The supplied file InputStream (to check).
	 * @param recordOriginalURL The full, original URL of this crawled record file (for file-name-based mimetype hinting). Can be null.
	 * @return The detected mimetype.
	 */
    public String getMimetype(InputStream inputStream, String fileName) {

    	if(inputStream == null)
    	{
    		System.err.println("Error: Null inputStream supplied to MimetypeDetector.getMimetype()");
    		return null;
    	}

        String mimeType = null;
        
        try {
            // Tika.detect should be threadsafe
            // Detecting MIME Type of the inputStream
            mimeType = tika.detect(inputStream, fileName);

        } catch (IOException e) {
			//e.printStackTrace();
        	if(HrwaManager.verbose) { System.err.println("Notice: Unable to detect a mimetype for the given InputStream."); }
		}
        // returning detected MIME Type
        return mimeType;

    }
    
    public String getMimetypeFromInputStreamAndURL(InputStream inputStream, URL urlThatContainsTheFileName) {
    	
    	String fileName = null;
		// If the URL isn't valid, then we'll just pass null along like this:
		// getMimeType(inputStream, null).
		// The file name (normally inferred from the URL) won't be factored into
		// mimetype detection, but that's okay. Mimetype detection is still
		// possible without the file name.
    	
    	if(urlThatContainsTheFileName != null)
    	{
	        //Filename parsing code partially based on:
	        //http://svn.apache.org/repos/asf/tika/tags/1.0/tika-core/src/main/java/org/apache/tika/io/TikaInputStream.java
	        //See: public static TikaInputStream get(URL url, Metadata metadata)
	    	String path = urlThatContainsTheFileName.getPath();
			int slash = path.lastIndexOf('/');
	        if (slash + 1 < path.length()) { // works even with -1!
	        	fileName = path.substring(slash + 1);
	        }
    	}
        
        return getMimetype(inputStream, fileName);
    }

    //Note: The overloaded getMimetype method below should be DRY-er, considering the alternate InputStream-reading version
	/**
     * Detect and return the mimetype of the supplied file.
     * @param The supplied file (to check).
     * @return The detected mimetype.
     */
    public String getMimetype(File file) {
    	if(file == null) return null;

        String mimeType = null;

        try {
            // Tika.detect should be threadsafe
            // Detecting MIME Type of the File
            mimeType = tika.detect(file);

        } catch (IOException e) {
            //e.printStackTrace();
            if(HrwaManager.verbose) { System.err.println("Notice: Unable to detect a mimetype for the given File."); }
        }
        // returning detected MIME Type
        return mimeType;
    }

}
