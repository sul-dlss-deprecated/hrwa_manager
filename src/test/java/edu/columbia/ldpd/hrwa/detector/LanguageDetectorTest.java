package edu.columbia.ldpd.hrwa.detector;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import com.cybozu.labs.langdetect.Language;

import edu.columbia.ldpd.hrwa.LanguageDetector;

public class LanguageDetectorTest {

	private final String languageDetectorProfileDirectory = "./src/main/resources/langdetect-profiles";
	private final LanguageDetector languageDetector = new LanguageDetector(languageDetectorProfileDirectory);

	@Test
	public void languageDetectionTest_null() {

		File file = null;
		String expectedLanguage = null;
		Language returnedLanguageObject = languageDetector.getLanguage(file);

		String detectedLanguage = null;
		double detectedLanguageProbability = 0;

		if(returnedLanguageObject != null)
		{
			detectedLanguage = returnedLanguageObject.lang;
			detectedLanguageProbability = returnedLanguageObject.prob;
		}

		System.out.println("Expected: " + expectedLanguage + ", Received: " + detectedLanguage + " (with probability: " + detectedLanguageProbability + ")");
		assertEquals(expectedLanguage, detectedLanguage);

		file = new File("./src/test/resources/language_detector/test_french.html");
		expectedLanguage = "fr";

		returnedLanguageObject = languageDetector.getLanguage(file);

		detectedLanguage = null;
		detectedLanguageProbability = 0;

		if(returnedLanguageObject != null)
		{
			detectedLanguage = returnedLanguageObject.lang;
			detectedLanguageProbability = returnedLanguageObject.prob;
		}

		System.out.println("Expected: " + expectedLanguage + ", Received: " + detectedLanguage + " (with probability: " + detectedLanguageProbability + ")");
		assertEquals(expectedLanguage, detectedLanguage);
	}

	@Test
	public void languageDetectionTest_spanish() {
		File file = new File("./src/test/resources/language_detector/test_spanish.html");
		String expectedLanguage = "es";

		Language returnedLanguageObject = languageDetector.getLanguage(file);

		String detectedLanguage = null;
		double detectedLanguageProbability = 0;

		if(returnedLanguageObject != null)
		{
			detectedLanguage = returnedLanguageObject.lang;
			detectedLanguageProbability = returnedLanguageObject.prob;
		}

		System.out.println("Expected: " + expectedLanguage + ", Received: " + detectedLanguage + " (with probability: " + detectedLanguageProbability + ")");
		assertEquals(expectedLanguage, detectedLanguage);
	}

}
