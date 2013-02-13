package edu.columbia.ldpd.hrwa.marc.impl;

import java.util.List;

import org.marc4j.marc.DataField;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;

import edu.columbia.ldpd.hrwa.marc.Field;


public class OrgTypeField extends FacetField {
    private static final String[] TAGS = new String[]{"653"};
    public OrgTypeField(String desc, String label) {
        super(desc, label, TAGS);
    }

    @Override
    public String [] getValues(VariableField field) {
        DataField df = (DataField)field;
        if (df.getIndicator1() != ' ' || df.getIndicator2() != '0') return Field.EMPTY_STRING_ARRAY;
        List<Subfield> sfs = df.getSubfields();
        String [] results = new String[sfs.size()];
        for (int i=0;i<results.length;i++) {
            results[i] = sfs.get(i).getData();
        }
        return results;
    }

}
