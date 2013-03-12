package org.jafer.record;

import static org.junit.Assert.assertEquals;

import java.nio.charset.Charset;

import org.jafer.exception.JaferException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MARC8UnicodeTests {
	
	MARC8Unicode test;
	
	@Before
	public void setUp(){
		test = new MARC8Unicode();
	}
	
	@After
	public void tearDown(){
		test = null;
	}
	
	@Test
	public void testMultiByteLatin() throws JaferException{
		String marc8 = "FA";
		// utf-8: efb8a2
		// ucs/utf-16: FE22
		String input = new String(new char[]{(char)Integer.parseInt(marc8,16)});
		String output = test.toUnicode(input);
		byte[] bytes = output.getBytes(Charset.forName("UTF16"));
		String actual = "U+";
		for (byte b:bytes) { actual += Integer.toHexString(b);}
		String expected = "U+FE22";
		assertEquals("EACC FA expected to map to U+FE22", expected, actual);
	}

	@Test
	public void testEACC() throws JaferException{
		char c = new Character((char)Integer.parseInt("3007",16));
		assertEquals("3007", Integer.toHexString(c));
		String marc8 = "212F30";
		// utf-8: e38087
		// ucs/utf-16: 3007
		String input = new String(new char[]{(char)Integer.parseInt(marc8,16)});
		String output = test.toUnicode(input);
		byte[] bytes = output.getBytes(Charset.forName("UTF16"));
		String actual = "U+";
		for (byte b:bytes) { actual += Integer.toHexString(b);}
		String expected = "U+3007";
		assertEquals("EACC 212F30 expected to map to U+3007", expected, actual);
	}
	
	@Test
	public void testEACCrev20020206() {
		
	}

	@Test
	public void testEACCrev20030206() {
		
	}

	@Test
	public void testEACCrev20040608() {
		
	}
	
	@Test
	public void testToMultiByte(){
		String multiByteHexValue = "6F7625";
		char [] expected = new char[]{(char)0x6f,(char)0x76,(char)0x25};
		char [] actual = test.toMultiByte(multiByteHexValue);
		assertEquals(new String(expected), new String(actual));
		multiByteHexValue = "6f4d3224";
		expected = new char[]{(char)0x6f,(char)0x4d,(char)0x32};
		actual = test.toMultiByte(multiByteHexValue);
		assertEquals(new String(expected), new String(actual));
	}

	@Test
	public void testEACCrev20040902() throws JaferException {
		String marc8 = "6F7625";
		String input = new String(new char[]{(char)Integer.parseInt(marc8,16)});
		String output = test.toUnicode(input);
		byte[] bytes = output.getBytes(Charset.forName("UTF-8"));
		String actual = "U+";
		for (byte b:bytes) { actual += Integer.toHexString(b);}
		String expected = "U+318D";
		assertEquals("EACC 6F7625 expected to map to U+318D", expected, actual);
	}
	
	@Test
	public void testUnicodeToMARC8() throws JaferException{
		// 50EE => 21324F
		String input = new String(new char[]{(char)0x0431});
		String output = test.toMARC8(input);
		byte[] bytes = output.getBytes(Charset.forName("ISO-8859-1"));
		String actual = "U+";
		for (byte b:bytes) { actual += Integer.toHexString(b);}
		String expected = "U+21324f";
		assertEquals(expected, actual);
	}
}
