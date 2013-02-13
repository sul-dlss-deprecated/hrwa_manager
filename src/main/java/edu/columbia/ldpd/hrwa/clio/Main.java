package edu.columbia.ldpd.hrwa.clio;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.marc4j.MarcException;

import edu.columbia.ldpd.hrwa.marc.z3950.MARCFetcher;


public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length == 0 || args[0].equals("help")) {
            usage();
            System.exit(0);
        }
            String marcPath = "marc_xml";
            String solrPath = "solr_xml";
            String configPath = "test.properties";
            for (int i=1; i<args.length;i++){
                if ("-m".equals(args[i])) {
                    if (args.length < i+2) {
                        System.out.println("ERROR: -m flag requires an argument");
                        usage();
                        return;
                    }
                    marcPath = args[++i];
                }
                if ("-s".equals(args[i])) {
                    if (args.length < i+2) {
                        System.out.println("ERROR: -s flag requires an argument");
                        usage();
                        return;
                    }
                    solrPath = args[++i];
                }
                if ("-p".equals(args[i])) {
                    if (args.length < i+2) {
                        System.out.println("ERROR: -p flag requires an argument");
                        usage();
                        return;
                    }
                    configPath = args[++i];
                }
            }
            File marcDir = new File(marcPath);
            File solrDir = new File(solrPath);
            File solrConfig = new File(configPath);
            if ("echo".equals(args[0])){
                System.out.println("marc dir: " + marcDir.getAbsolutePath());
                System.out.println("solr dir: " + solrDir.getAbsolutePath());
                return;
            }
            else if ("reindex".equals(args[0])){
                Properties solrProps = new Properties();
                if (solrConfig.exists()){
                    FileReader rdr = new FileReader(solrConfig);
                    solrProps.load(rdr);
                    rdr.close();
                } else {
                    solrProps.load(Main.class.getResourceAsStream(solrPath));
                }
                File [] marcFiles = marcDir.listFiles();
                for (File marcFile:marcFiles) marcFile.delete();

                File [] solrFiles = solrDir.listFiles();
                for (File solrFile:solrFiles) solrFile.delete();

                fetchMARC(marcDir);
                createSolrFiles(marcDir, solrDir);
                postSolrFiles(solrDir, solrProps);
                generateFacetView();
            }
    }

    public static void fetchMARC(File marcDir) {
        new MARCFetcher(marcDir).fetch();
    }

    public static void createSolrFiles(File marcDir, File solrDir) {
        File [] marcFiles = marcDir.listFiles();
        for (File marc:marcFiles){
            try{
                new SolrDoc(marc).serialize(solrDir);
            } catch (IOException e) {
                System.err.println("ERROR: " + e.toString() + "( " + marc.getName() + " )");
            } catch (MarcException e) {
                System.err.println("ERROR: " + e.toString() + "( " + marc.getName() + " )");
            }
        }
    }

    public static void postSolrFiles(File solrDir, Properties solrConfig) {
        File[] solrFiles = solrDir.listFiles();
        for (File file:solrFiles) {

        }
        throw new UnsupportedOperationException("method not implemented");
    }

    public static void generateFacetView(){
        throw new UnsupportedOperationException("method not implemented");
    }

    private static void usage() {
        System.out.println("Usage: java -jar {jar name} [help|reindex] [-m {marc dir}] [-s {solr dir}] [-p {solr config path}]");
        System.out.println("Default marc dir is " + new File("marc_xml").getAbsolutePath());
        System.out.println("Default solr dir is " + new File("solr_xml").getAbsolutePath());
        System.out.println("Default solr config is " + new File("test.properties").getAbsolutePath());
    }
}
