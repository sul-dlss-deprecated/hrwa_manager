package edu.columbia.ldpd.hrwa.solr;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.Test;

import edu.columbia.ldpd.hrwa.clio.Main;


public class MainTest {

    @Test
    public void testArgs() throws IOException {
        String [] args = new String[]{"echo","-m","foo", "-s", "bar"};
        PrintStream orig = System.out;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(bos);
        System.setOut(capture);
        Main.main(args);
        System.out.flush();
        System.setOut(orig);
        String actual = new String(bos.toByteArray());
        String expected = "marc dir: " + absPath("foo") + "\nsolr dir: " + absPath("bar") + "\n";
        assertEquals(expected, actual);
    }

    @Test
    public void testDefaults() throws IOException {
        String [] args = new String[]{"echo"};
        PrintStream orig = System.out;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(bos);
        System.setOut(capture);
        Main.main(args);
        System.out.flush();
        System.setOut(orig);
        String actual = new String(bos.toByteArray());
        String expected = "marc dir: " + absPath("marc_xml") + "\nsolr dir: " + absPath("solr_xml") + "\n";
        assertEquals(expected, actual);
    }

    private String absPath(String part) {
        return new File(part).getAbsolutePath();
    }

}
