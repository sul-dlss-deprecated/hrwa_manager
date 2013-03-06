package edu.columbia.ldpd.hrwa.marc.z3950;

import java.io.File;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jafer.exception.JaferException;
import org.jafer.query.QueryBuilder;
import org.jafer.record.Field;
import org.jafer.zclient.ZClient;
import org.jafer.zclient.ZClientFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class MARCFetcher {
    private final File targetDir;
    private String host = "clio-db.cc.columbia.edu";
    private int port = 7090;
    private String[] databases = {"voyager"};

    public MARCFetcher(File targetDir) {
        this.targetDir = targetDir;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setDatabases(String[] databases){
        this.databases = databases;
    }

    /**
     * Fetch the MARC records with a 965 == '965hrportal'
     */
    public void fetch(){
        fetch(1,9000,"965hrportal");
    }

    /**
     * Fetch the MARC record with a 001 == id
     * ID is field 1007 in the Semantic set in bib-1
     * @param id
     */
    public void fetch(String id) {
        fetch(1,1007,id);
    }

    private StreamResult getResult(String id){
        if (this.targetDir == null) {
            return new StreamResult(System.out);
        } else {
            File out = new File(targetDir, id + ".xml");
            return new StreamResult(out);
        }
    }


    /**
     * Fetch the MARC records where the Z3950 field indicated matches the query value
     * @param queryType 1 (Semantic), 2 (Relation), 3 (Position), 4 (Structure), 5 (Truncation), 6 (Completeness)
     * @param queryField
     * @param queryValue
     */
    public void fetch(int queryType, int queryField, String queryValue){
        if (!targetDir.exists()) targetDir.mkdirs();
        int[] query = {0,0,0,0,0,0};
        query[queryType - 1] = queryField;
        ZClientFactory zcf = new ZClientFactory();
        zcf.setHost(host);
        zcf.setPort(port);
        zcf.setDatabases(databases);
        ZClient client = (ZClient)zcf.getDatabean();
        client.setParseQuery(false);
        try{
            QueryBuilder b = new QueryBuilder();
            Node node = b.getNode(query,queryValue);
            client.setFetchSize(1024);

            // Below is the default namespace from Jafer, for documentary purposes 
            // String oai_marc = "http://www.openarchives.org/OAI/oai_marc";
            String slim_marc = "http://www.loc.gov/MARC21/slim";
            client.setRecordSchema(slim_marc);
            client.submitQuery(node);

            Field f = null;
            int len = client.getNumberOfResults();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = null;
            try{
                t = tf.newTransformer();
            } catch (TransformerException e){
                System.err.println("Could not create xml transform: " + e.toString());
                return;
            }
            for (int i=1;i<=len;i++){
                client.setRecordCursor(i);
                f = client.getCurrentRecord();
                Node domXml = f.getXML();
                if (!slim_marc.equals(domXml.getNamespaceURI())) {
                	throw new RuntimeException("Received document of type " + domXml.getNamespaceURI() + ", but require type " + slim_marc);
                }
                String id = get001(domXml);
                System.out.println("id: " + id);
                DOMSource xmlSource = new DOMSource(f.getXML());
                StreamResult outputTarget = getResult(id);
                try{
                    t.transform(xmlSource, outputTarget);
                } catch (TransformerException e){
                    System.err.println("Could not transform xml: " + e.toString());
                }
            }

        } catch (JaferException e){
            if (e.hasDiagnostic()){
              System.err.println(e.getDiagnostic().toString());
            }
            e.printStackTrace();
        }
    }
    private static String get001(Node domXml) {
        NodeList ns = domXml.getChildNodes();
        String id = null;
        int nLen = ns.getLength();
        for (int j = 0;j<nLen;j++){
            Node n = ns.item(j);
            Node tag = (n.getAttributes() != null)? n.getAttributes().getNamedItem("tag") : null;
            if (tag != null && "001".equals(tag.getTextContent())){
                id = n.getTextContent();
                break;
            }
        }
        return id;
    }

    public static void main(String[] args){
        MARCFetcher ex = new MARCFetcher(new File("marcdebug"));
        ex.fetch("8966262");
    }

}
