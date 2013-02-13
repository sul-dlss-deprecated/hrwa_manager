package edu.columbia.ldpd.hrwa.marc.impl;

import java.util.regex.Pattern;

import org.marc4j.marc.DataField;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;

import edu.columbia.ldpd.hrwa.marc.Field;


public class SubjectField extends FacetField {
    private static final Pattern[] EXCLUSIONS = new Pattern[]{
        Pattern.compile("^\\s*human rights advocacy\\s*$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\s*national human rights institutions\\s*$", Pattern.CASE_INSENSITIVE)
    };
    private static final Pattern HR = Pattern.compile("^human rights$", Pattern.CASE_INSENSITIVE);
    private static final String [] TAGS = new String[]{"650"};
    public SubjectField(String desc, String label) {
        super(desc, label, Field.EMPTY_STRING_ARRAY);
    }

    @Override
    public String[] getTags() {
        return TAGS;
    }

    @Override
    public String[] getValues(VariableField vf) {

        if (vf == null) return Field.EMPTY_STRING_ARRAY;
        DataField df = (DataField)vf;
        Subfield a = df.getSubfield('a');
        Subfield x = df.getSubfield('x');
        String aVal = a.getData().replaceFirst("\\.$","");
        String xVal = (x == null)? null : x.getData().replaceFirst("\\.$","");
        for (Pattern p:EXCLUSIONS){
            if (p.matcher(aVal).matches()) return Field.EMPTY_STRING_ARRAY;
        }
        if (HR.matcher(aVal).matches()) aVal = "Human rights (General)";
        if (xVal == null) return new String[]{aVal};
        return new String[]{aVal + " -- " + xVal};
    }

}
