package edu.columbia.ldpd.hrwa.clio;

import static edu.columbia.ldpd.hrwa.marc.Field.EMPTY_STRING_ARRAY;
import static edu.columbia.ldpd.hrwa.marc.Field.FIELDS;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.marc4j.marc.Record;

import edu.columbia.ldpd.hrwa.marc.Field;
import edu.columbia.ldpd.hrwa.marc.MarcCollection;


public class SolrDoc {
    private static final Charset UTF8 = utf8();

    private final Record m_marcRecord;

    public SolrDoc(File marc) throws IOException {
        this(new FileInputStream(marc));
    }

    public SolrDoc(InputStream marc) throws IOException {
        MarcCollection c = new MarcCollection(marc);
        m_marcRecord = c.getRecords().next();
        marc.close();
    }

    public SolrDoc(Record marcRecord){
        m_marcRecord = marcRecord;
    }

    private static Charset utf8() {
        try {
            return Charset.forName("UTF-8");
        } catch (Throwable t) { return null; } //shhh
    }

    @Override
    public String toString(){
        StringBuffer result = new StringBuffer();
        result.append("<add overwrite=\"true\">\n<doc>\n");
        String[] field_keys = FIELDS.keySet().toArray(EMPTY_STRING_ARRAY);
        Arrays.sort(field_keys);
        for (String key:field_keys){
            Field field = FIELDS.get(key);
            String [] vals = field.getValues(m_marcRecord);
            if (field.subfieldsFor("520").equals("a")){
                if (vals.length > 1){
                    System.err.println(bibKey() + " has " + vals.length + " summary values");
                    vals = new String[]{vals[0]};
                }
            }
            for (String val:vals){
                result.append("    <field name=\"").append(key).append("\">")
                      .append(scrub(val)).append("</field>\n");
            }
        }
        result.append("</doc>\n</add>");
        return result.toString();
    }

    private static String scrub(String input) {
        return input.replaceAll("&", "&amp;");
    }

    private String bibKey() {
        return FIELDS.get("bib_key").getValues(m_marcRecord)[0];
    }

    private String fileName(){
         return bibKey() + ".xml";
    }

    /**
     *
     * @param dir - the directory in which to serialize itself as a XML file
     * @return - the File object for the serialized XML
     * @throws IOException
     */
    public File serialize(File dir) throws IOException {
        File file = new File(dir,fileName());
        Writer out = new OutputStreamWriter(new FileOutputStream(file),UTF8);
        out.write(toString());
        out.flush();
        out.close();
        return file;
    }
}
