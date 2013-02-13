package edu.columbia.ldpd.hrwa.solr;

import java.io.File;

import org.junit.Test;

import edu.columbia.ldpd.hrwa.marc.z3950.MARCFetcher;


public class MARCFetcherTest {

    @Test
    public void test() {
        new MARCFetcher(new File("marc_xml")).fetch();
    }

}
