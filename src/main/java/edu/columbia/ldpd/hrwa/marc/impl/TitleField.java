package edu.columbia.ldpd.hrwa.marc.impl;

import org.marc4j.marc.DataField;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;

import edu.columbia.ldpd.hrwa.marc.Field;


public class TitleField extends DefaultField {
    private static final String [] TAGS = new String[]{"245"};
    private static final String [] SORT_TAGS = new String[]{"245"};

    private final boolean sort;

    public TitleField(String desc, String label, boolean sort) {
        super(desc, label, Field.EMPTY_STRING_ARRAY);
        this.sort = sort;
    }

    @Override
    public String[] getTags() {
        return sort ? SORT_TAGS : TAGS;
    }

    @Override
    public String[] getNullValues(){
        return new String[]{"[ERROR: Title not found]"};
    }

    @Override
    public String[] getValues(VariableField vf){
        if (vf == null) return Field.EMPTY_STRING_ARRAY;
        DataField df = (DataField)vf;
        Subfield a = df.getSubfield('a');
        if (a == null) return Field.EMPTY_STRING_ARRAY;
        String aVal = a.getData();
        aVal = aVal.replaceFirst("\\s*=\\s*$","");
        if (this.sort){
            int offset = Character.getNumericValue(df.getIndicator2());
            aVal = aVal.substring(offset).toUpperCase();
        }
        return new String[]{aVal};
    }

}
