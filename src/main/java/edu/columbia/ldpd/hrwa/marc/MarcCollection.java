package edu.columbia.ldpd.hrwa.marc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.marc4j.MarcXmlReader;
import org.marc4j.marc.Record;

public class MarcCollection {
    private final Set<Record> records = new HashSet<Record>(1);
    public MarcCollection(InputStream input) {
        MarcXmlReader reader = new MarcXmlReader(input);
        while (reader.hasNext()) {
            records.add(reader.next());
        }
    }
    public MarcCollection(File input) throws IOException {
        this(new FileInputStream(input));
    }

    public Iterator<Record> getRecords() {
        return this.records.iterator();
    }

    public CharSequence toSolrDocument() {
        StringBuffer buffer = new StringBuffer(2048);
        buffer.append("<add overwrite=\"true\">");
        for (Record marcRecord: records){
            buffer.append("<doc>");
            for (String fieldId : Field.FIELDS.keySet()){
                Field solrField = Field.FIELDS.get(fieldId);
                solrField.getValues(marcRecord);
            }
            buffer.append("</doc>");
        }
        buffer.append("</add>");
        return buffer;
    }

    public static void main(String[] args) throws IOException{
        MarcCollection mc = new MarcCollection(new File("marc_xml/8966262.xml"));
        mc.getRecords().next();
    }
}
