package edu.columbia.ldpd.hrwa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;
import org.junit.Test;

public class RegexTest {

	final Pattern EXCLUDE = Pattern.compile("^(image/|audio/|video/|application/zip|application/shockwave|application/javascript|application/x-rar-compressed).*");

	@Test
	public void regexTest() {
		 assertEquals(EXCLUDE.matcher("image/jpeg").matches(), true);
         assertEquals(EXCLUDE.matcher("audio/x-aiff").matches(), true);
         assertEquals(EXCLUDE.matcher("video/ogg").matches(), true);
         assertEquals(EXCLUDE.matcher("application/zip").matches(), true);
         assertEquals(EXCLUDE.matcher("application/shockwave").matches(), true);
         assertEquals(EXCLUDE.matcher("application/javascript").matches(), true);
         assertEquals(EXCLUDE.matcher("application/x-rar-compressed").matches(), true);
		 
         assertEquals(EXCLUDE.matcher("something/else").matches(), false);
		 assertEquals(EXCLUDE.matcher("").matches(), false);
	}
	
	@Test
	public void stringComparisonSpeedTest() {
		
		long startTime;
		String detectedMimetype = "application/x-rar-compressed";
		int loopIterations = 1000000;
		
		startTime = System.nanoTime();
		boolean checkLang = false;
		for(int i = 0; i < loopIterations; i++)
		{
			checkLang = (detectedMimetype != null) &&
								!(detectedMimetype.startsWith("image")) &&
				                !(detectedMimetype.startsWith("audio")) &&
				                !(detectedMimetype.startsWith("video")) &&
				                !(detectedMimetype.startsWith("application/zip")) &&
				                !(detectedMimetype.startsWith("application/shockwave")) &&
				                !(detectedMimetype.startsWith("application/javascript")) &&
				                !(detectedMimetype.startsWith("application/x-rar-compressed"));
			
		}
		long startsWithTime = (System.nanoTime() - startTime)/loopIterations;
		
		
		startTime = System.nanoTime();
		for(int i = 0; i < loopIterations; i++)
		{
			checkLang = EXCLUDE.matcher("image/audio").matches();
		}
		long matcherTime = (System.nanoTime() - startTime)/loopIterations;
		
		if(checkLang) {
			//Do nothing. This if statement is here in order to eliminate a Java warning because the value of checkLang isn't actually used.
		}
		
		if(startsWithTime < matcherTime) {
			System.out.println("stringComparisonSpeedTest(): Using startsWith is faster than using a matcher with the current regex.");
		} else {
			System.out.println("stringComparisonSpeedTest(): Using startsWith is NOT faster than using a matcher with the current regex.");
		}
		
		System.out.println("Time Difference:");
		System.out.println("startsWith: " + startsWithTime);
		System.out.println("matcher: " + matcherTime);
		
		assertTrue("Using startsWith is faster than using a matcher with the current regex.", startsWithTime < matcherTime);
	}

}
