package edu.columbia.ldpd.hrwa.marc.impl;

import org.marc4j.marc.DataField;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;

import edu.columbia.ldpd.hrwa.marc.Field;


public class UrlField extends DefaultField {
    private static final String[] TAGS = new String[]{"920"};
    private final char m_ind2;
    public UrlField(String desc, String label, boolean archived) {
        super(desc, label, TAGS);
        m_ind2 = archived ? '1' : '0';
    }

    public boolean archived() {
        return m_ind2 == '1';
    }

    @Override
    public String [] getValues(VariableField field) {
        DataField df = (DataField)field;
        if (df.getIndicator1() == '4' && df.getIndicator2() == m_ind2) {
            Subfield u = df.getSubfield('u');
            if (u != null) {
                return new String[]{df.getSubfield('u').getData()};
            }
        }
        return Field.EMPTY_STRING_ARRAY;
    }

}
