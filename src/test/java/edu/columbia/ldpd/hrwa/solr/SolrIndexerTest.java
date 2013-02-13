package edu.columbia.ldpd.hrwa.solr;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.junit.Test;

import edu.columbia.ldpd.hrwa.clio.Main;


public class SolrIndexerTest {
    /**
     * This is not really a test, but a cheap rig to run the prototype code
     * @throws IOException
     */
    @Test
    public void test() throws IOException {
        Properties config = new Properties();
        config.load(getClass().getResourceAsStream("/solr.properties"));
        File marcDir = new File("marc_xml");
        File solrDir = new File("solr_xml");
        if (!marcDir.exists()) marcDir.mkdirs();
        if (!solrDir.exists()) solrDir.mkdirs();
        Main.createSolrFiles(marcDir, solrDir);
        SolrIndexer si = new SolrIndexer(config);
        si.index(solrDir);
    }

}
