package edu.columbia.ldpd.hrwa.detector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

import org.junit.Test;

import edu.columbia.ldpd.hrwa.MimetypeDetector;

public class MimetypeDetectorTest {

	@Test
	public void mimetypeDetectionTest_null() throws FileNotFoundException {
		File file = null;
		String expectedMimetype = null;
		
		String actualMimetype = new MimetypeDetector().getMimetype(null);
		//System.out.println("Expected: " + expectedMimetype + ", Received: " + actualMimetype);
		assertEquals(expectedMimetype, actualMimetype);
	}

	@Test
	public void mimetypeDetectionTest_text_html() throws FileNotFoundException {
		InputStream is = this.getClass().getResourceAsStream("/mimetype_detector/test_html.html");
		String expectedMimetype = "text/html";
		
		String actualMimetype = new MimetypeDetector().getMimetype(is, "test_html.html");
		//System.out.println("Expected: " + expectedMimetype + ", Received: " + actualMimetype);
		assertEquals(expectedMimetype, actualMimetype);
		
		//File test
		File file = new File("./src/test/resources/mimetype_detector/test_html.html");
		actualMimetype = new MimetypeDetector().getMimetype(file);
		//System.out.println("Expected: " + expectedMimetype + ", Received: " + actualMimetype);
		assertEquals(expectedMimetype, actualMimetype);
	}

	@Test
	public void mimetypeDetectionTest_pdf() throws FileNotFoundException {
		InputStream is = this.getClass().getResourceAsStream("/mimetype_detector/test_pdf.pdf");
		String expectedMimetype = "application/pdf";
		
		String actualMimetype = new MimetypeDetector().getMimetype(is, "test_pdf.pdf");
		//System.out.println("Expected: " + expectedMimetype + ", Received: " + actualMimetype);
		assertEquals(expectedMimetype, actualMimetype);
		
		//File test
		File file = new File("./src/test/resources/mimetype_detector/test_pdf.pdf");
		actualMimetype = new MimetypeDetector().getMimetype(file);
		//System.out.println("Expected: " + expectedMimetype + ", Received: " + actualMimetype);
		assertEquals(expectedMimetype, actualMimetype);
	}

	@Test
	public void mimetypeDetectionTest_powerpoint() throws FileNotFoundException {
		InputStream is = this.getClass().getResourceAsStream("/mimetype_detector/test_powerpoint.pptx");
		String[] expectedMimetypes = {"application/vnd.openxmlformats-officedocument.presentationml.presentation"};
		
		String actualMimetype = new MimetypeDetector().getMimetype(is, "test_powerpoint.pptx");
		//System.out.println("Expected one of: " + Arrays.deepToString(expectedMimetypes) + ", Received: " + actualMimetype);
		assertTrue("Expected one of: " + Arrays.deepToString(expectedMimetypes) + ", Received: " + actualMimetype, Arrays.asList(expectedMimetypes).contains(actualMimetype));
		
		//File test
		File file = new File("./src/test/resources/mimetype_detector/test_powerpoint.pptx");
		actualMimetype = new MimetypeDetector().getMimetype(file);
		//System.out.println("Expected one of: " + Arrays.deepToString(expectedMimetypes) + ", Received: " + actualMimetype);
		assertTrue("Expected one of: " + Arrays.deepToString(expectedMimetypes) + ", Received: " + actualMimetype, Arrays.asList(expectedMimetypes).contains(actualMimetype));
	}

	@Test
	public void mimetypeDetectionTest_word() throws FileNotFoundException {
		InputStream is = this.getClass().getResourceAsStream("/mimetype_detector/test_word.docx");
		String[] expectedMimetypes = {"application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
		
		String actualMimetype = new MimetypeDetector().getMimetype(is, "test_word.docx");
		//System.out.println("Expected one of: " + Arrays.deepToString(expectedMimetypes) + ", Received: " + actualMimetype);
		assertTrue("Expected one of: " + Arrays.deepToString(expectedMimetypes) + ", Received: " + actualMimetype, Arrays.asList(expectedMimetypes).contains(actualMimetype));
		
		//File test
		File file = new File("./src/test/resources/mimetype_detector/test_word.docx");
		actualMimetype = new MimetypeDetector().getMimetype(file);
		//System.out.println("Expected one of: " + Arrays.deepToString(expectedMimetypes) + ", Received: " + actualMimetype);
		assertTrue("Expected one of: " + Arrays.deepToString(expectedMimetypes) + ", Received: " + actualMimetype, Arrays.asList(expectedMimetypes).contains(actualMimetype));
	}

}