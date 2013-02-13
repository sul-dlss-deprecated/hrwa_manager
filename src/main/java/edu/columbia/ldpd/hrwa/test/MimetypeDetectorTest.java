package edu.columbia.ldpd.hrwa.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;

import org.junit.Test;

import edu.columbia.ldpd.hrwa.MimetypeDetector;

public class MimetypeDetectorTest {

	@Test
	public void mimetypeDetectionTest_null() throws FileNotFoundException {
		File file = null;
		String expectedMimetype = null;
		
		String actualMimetype;
		actualMimetype = new MimetypeDetector().getMimetype(file);
		//System.out.println("Expected: " + expectedMimetype + ", Received: " + actualMimetype);
		assertEquals(expectedMimetype, actualMimetype);
		
		actualMimetype = new MimetypeDetector().getMimetype(null, null);
		//System.out.println("Expected: " + expectedMimetype + ", Received: " + actualMimetype);
		assertEquals(expectedMimetype, actualMimetype);
	}

	@Test
	public void mimetypeDetectionTest_text_html() throws FileNotFoundException {
		File file = new File("./sample_data/test_html.html");
		String expectedMimetype = "text/html";
		
		String actualMimetype;
		actualMimetype = new MimetypeDetector().getMimetype(file);
		//System.out.println("Expected: " + expectedMimetype + ", Received: " + actualMimetype);
		assertEquals(expectedMimetype, actualMimetype);
		
		actualMimetype = new MimetypeDetector().getMimetype(new FileInputStream(file), file.getName());
		//System.out.println("Expected: " + expectedMimetype + ", Received: " + actualMimetype);
		assertEquals(expectedMimetype, actualMimetype);
	}

	@Test
	public void mimetypeDetectionTest_pdf() throws FileNotFoundException {
		File file = new File("./sample_data/test_pdf.pdf");
		String expectedMimetype = "application/pdf";
		
		String actualMimetype;
		actualMimetype = new MimetypeDetector().getMimetype(file);
		//System.out.println("Expected: " + expectedMimetype + ", Received: " + actualMimetype);
		assertEquals(expectedMimetype, actualMimetype);
		
		actualMimetype = new MimetypeDetector().getMimetype(new FileInputStream(file), file.getName());
		//System.out.println("Expected: " + expectedMimetype + ", Received: " + actualMimetype);
		assertEquals(expectedMimetype, actualMimetype);
	}

	@Test
	public void mimetypeDetectionTest_powerpoint() throws FileNotFoundException {
		File file = new File("./sample_data/test_powerpoint.pptx");
		String[] expectedMimetypes = {"application/vnd.openxmlformats-officedocument.presentationml.presentation"};
		
		String actualMimetype;
		actualMimetype = new MimetypeDetector().getMimetype(file);
		System.out.println("Expected one of: " + Arrays.deepToString(expectedMimetypes) + ", Received: " + actualMimetype);
		assertTrue("Expected one of: " + Arrays.deepToString(expectedMimetypes) + ", Received: " + actualMimetype, Arrays.asList(expectedMimetypes).contains(actualMimetype));

		actualMimetype = new MimetypeDetector().getMimetype(new FileInputStream(file), file.getName());
		System.out.println("Expected one of: " + Arrays.deepToString(expectedMimetypes) + ", Received: " + actualMimetype);
		assertTrue("Expected one of: " + Arrays.deepToString(expectedMimetypes) + ", Received: " + actualMimetype, Arrays.asList(expectedMimetypes).contains(actualMimetype));
	}

	@Test
	public void mimetypeDetectionTest_word() throws FileNotFoundException {
		File file = new File("./sample_data/test_word.docx");
		String[] expectedMimetypes = {"application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
		
		String actualMimetype;
		actualMimetype = new MimetypeDetector().getMimetype(file);
		System.out.println("Expected one of: " + Arrays.deepToString(expectedMimetypes) + ", Received: " + actualMimetype);
		assertTrue("Expected one of: " + Arrays.deepToString(expectedMimetypes) + ", Received: " + actualMimetype, Arrays.asList(expectedMimetypes).contains(actualMimetype));

		actualMimetype = new MimetypeDetector().getMimetype(new FileInputStream(file), file.getName());
		System.out.println("Expected one of: " + Arrays.deepToString(expectedMimetypes) + ", Received: " + actualMimetype);
		assertTrue("Expected one of: " + Arrays.deepToString(expectedMimetypes) + ", Received: " + actualMimetype, Arrays.asList(expectedMimetypes).contains(actualMimetype));
	}

}