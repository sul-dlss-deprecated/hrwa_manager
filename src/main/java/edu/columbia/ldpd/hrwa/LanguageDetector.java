package edu.columbia.ldpd.hrwa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayDeque;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import com.cybozu.labs.langdetect.Language;

public class LanguageDetector {

    private static boolean profileIsLoaded = false;
    // If the language detection fails, we return a new Language object
    // where the detected language == null and the language detection
    // probability == 0
    private static Language FAILURE = new Language(null,0);

    public LanguageDetector(String profileDirectory)
    {
        loadProfileOnce(profileDirectory);
    }

    /**
     * DetectorFactory throws an error if you try to load a profile more than once during the entire run of the application.
     */
    public static void loadProfileOnce(String profileDirectory) {
        if( ! profileIsLoaded )
        {
            try {
                DetectorFactory.loadProfile(profileDirectory);
                profileIsLoaded = true;
            } catch (LangDetectException e) {
            	System.err.println("Error: Could not load language-detection profiles.");
                //e.printStackTrace();
            }
        }
    }

    public Language getLanguage(Reader reader) throws IOException {

        if(reader == null) return FAILURE;

        //Use DetectorFactory to create a new Detector instance.
        //This needs to be done every single time that you perform detection a new string/file
        //http://code.google.com/p/language-detection/issues/detail?id=15
        Detector detector = null;
        try {
            detector = checkout();
            //Send the text-to-detect to the detector
            detector.append(reader);
            // "The Detector#getProbabilities method returns a languages list with their probabilities. The list is in order of probability."
            // http://code.google.com/p/language-detection/wiki/Tutorial
            // So we'll return the first one in the list! If this method fails
            // to detect a language, then the LangDetectException below will
            // occur and we won't have an ArrayList to work with.
            return detector.getProbabilities().get(0);
        } catch (Exception e) {
            //e.printStackTrace();
        	if(HrwaManager.verbose) { System.err.println("Notice: Unable to detect a language for the given Reader."); }
            return FAILURE;
        } finally {
            if (detector != null) checkin(detector);
        }
    }

    // Not sure why this one implementation should throw the IOException
    // instead of returning the failure Language, but that's what code expects
    public Language getLanguage(InputStream inputStream) throws IOException {
        return getLanguage(new InputStreamReader(inputStream));
    }


    /**
     * Don't use this method if you can help it.  Bad performance compared to other getLanguage methods.
     * @param stringToScan
     * @return
     */
    public Language getLanguage(String stringToScan) {
        // this is a performance hit, but we never use this method anyway
        try {
            return getLanguage(new StringReader(stringToScan));
        } catch (IOException e) {
            //e.printStackTrace();
        	if(HrwaManager.verbose) { System.err.println("Notice: Unable to detect a language for the given String."); }
            return FAILURE;
        }
    }

    public Language getLanguage(File file) {
        try {
            return getLanguage(new BufferedReader(new FileReader(file)));
        } catch (Exception e) {
            //e.printStackTrace();
        	if(HrwaManager.verbose) { System.err.println("Notice: Unable to detect a language for the given File."); }
            return FAILURE;
        }
    }

    private static ArrayDeque<Detector> DETECTOR_POOL = new ArrayDeque<Detector>();

    private static Detector checkout() throws LangDetectException {
        synchronized(DETECTOR_POOL) {
            if (DETECTOR_POOL.isEmpty()) {
                return DetectorFactory.create();
            }
            else {
                return DETECTOR_POOL.pop();
            }
        }
    }
    private static void checkin(Detector detector) {
        try {
            recycle(detector);
        } catch (Exception e) {
            System.err.println("Could not recycle language detector: " + e.toString());
            return;
        }
        synchronized(DETECTOR_POOL) {
            DETECTOR_POOL.push(detector);
        }
    }
    private static Field TEXT_FIELD = getDetectorField("text");
    private static Field LANGPROB_FIELD = getDetectorField("langprob");
    private static Field ALPHA_FIELD = getDetectorField("alpha");
    private static Field getDetectorField(String name) {
        try {
            Field result = Detector.class.getDeclaredField(name);
            result.setAccessible(true);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static void recycle(Detector d) throws Exception {
        StringBuffer x = (StringBuffer)TEXT_FIELD.get(d);
        //x.trimToSize();
        x.setLength(0);
        LANGPROB_FIELD.set(d,  null);
        ALPHA_FIELD.set(d,0.5);
    }
}
