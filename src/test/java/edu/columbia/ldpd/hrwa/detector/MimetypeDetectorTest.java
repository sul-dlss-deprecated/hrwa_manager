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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

import edu.columbia.ldpd.hrwa.MimetypeDetector;

public class MimetypeDetectorTest {
	
	private int numLoopsForMultithreadedTest = 500;
	private int numThreadsToCreateForMultithreadedTest = 4;

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
		//Close input stream
		try { is.close(); }
		catch (IOException e) { e.printStackTrace(); }
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
		//Close input stream
		try { is.close(); }
		catch (IOException e) { e.printStackTrace(); }
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
		//Close input stream
		try { is.close(); }
		catch (IOException e) { e.printStackTrace(); }
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
		//Close input stream
		try { is.close(); }
		catch (IOException e) { e.printStackTrace(); }
		//System.out.println("Expected one of: " + Arrays.deepToString(expectedMimetypes) + ", Received: " + actualMimetype);
		assertTrue("Expected one of: " + Arrays.deepToString(expectedMimetypes) + ", Received: " + actualMimetype, Arrays.asList(expectedMimetypes).contains(actualMimetype));
		
		//File test
		File file = new File("./src/test/resources/mimetype_detector/test_word.docx");
		actualMimetype = new MimetypeDetector().getMimetype(file);
		//System.out.println("Expected one of: " + Arrays.deepToString(expectedMimetypes) + ", Received: " + actualMimetype);
		assertTrue("Expected one of: " + Arrays.deepToString(expectedMimetypes) + ", Received: " + actualMimetype, Arrays.asList(expectedMimetypes).contains(actualMimetype));
	}
	
	@Test
	public void mimetypeDecectorWorksInMultithreadedScenario() {
	
		System.out.println("Multithreading speed test:");
		
		long startTime;
		String expectedMimetype;
		InputStream is;
		String actualMimetype;
		
		//Ignore first run below (just used to initialize any first-time-run stuff)
		expectedMimetype = "application/pdf";
		is = this.getClass().getResourceAsStream("/mimetype_detector/test_pdf.pdf");
		actualMimetype = new MimetypeDetector().getMimetype(is, "test_pdf.pdf");
		//Close input stream
		try { is.close(); }
		catch (IOException e) { e.printStackTrace(); }
		assertEquals(expectedMimetype, actualMimetype);
		
		//Standard mimetype detection
		startTime = System.currentTimeMillis();
		
		for(int i = 0; i < numLoopsForMultithreadedTest; i++) {
			expectedMimetype = "application/pdf";
			is = this.getClass().getResourceAsStream("/mimetype_detector/test_pdf.pdf");
			actualMimetype = new MimetypeDetector().getMimetype(is, "test_pdf.pdf");
			//Close input stream
			try { is.close(); }
			catch (IOException e) { e.printStackTrace(); }
			assertEquals(expectedMimetype, actualMimetype);
		}
		
		long singleThreadedRunTime = System.currentTimeMillis() - startTime;
		
		System.out.println("-- Single threaded total time: " + singleThreadedRunTime + " ms");
		
		Thread[] threads = new Thread[numThreadsToCreateForMultithreadedTest];
		Future[] futures = new Future[numThreadsToCreateForMultithreadedTest];
		
		for(int i = 0; i < numThreadsToCreateForMultithreadedTest; i++) {
			threads[i] = new Thread("thread" + i) {
				
				public void run() {
					
					String expectedMimetype;
					InputStream is;
					String actualMimetype;
					
					int fractionOfLoopsForDistributedWork = numLoopsForMultithreadedTest/numThreadsToCreateForMultithreadedTest;
					
					for(int i = 0; i < fractionOfLoopsForDistributedWork; i++) {
						expectedMimetype = "application/pdf";
						is = this.getClass().getResourceAsStream("/mimetype_detector/test_pdf.pdf");
						actualMimetype = new MimetypeDetector().getMimetype(is, "test_pdf.pdf");
						//Close input stream
						try { is.close(); }
						catch (IOException e) { e.printStackTrace(); }
						assertEquals(expectedMimetype, actualMimetype);
					}
				}
			
			};
		}
		
		startTime = System.currentTimeMillis();
		ExecutorService es = Executors.newFixedThreadPool(threads.length);
		
		startTime = System.currentTimeMillis();
		
		for(int i = 0; i < numThreadsToCreateForMultithreadedTest; i++) {
			futures[i] = es.submit(threads[i]); 
		}
		
		while( true ) {
			
			boolean foundRunningThread = false;
			
			for(Future singleFuture : futures) {
				if( ! singleFuture.isDone() ) {
					foundRunningThread = true;
				}
			}
			
			if(! foundRunningThread ) {
				break;
			}
			
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		long multiThreadedRunTime = System.currentTimeMillis() - startTime;
		
		System.out.println("-- Multithreaded distribution between " + numThreadsToCreateForMultithreadedTest + " threads: " + multiThreadedRunTime + " ms");
		
	}

}