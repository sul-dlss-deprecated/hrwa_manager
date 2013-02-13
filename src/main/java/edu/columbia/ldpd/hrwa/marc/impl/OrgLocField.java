package edu.columbia.ldpd.hrwa.marc.impl;
import static edu.columbia.ldpd.hrwa.marc.translation.Translations.COUNTRY;

import org.marc4j.marc.ControlField;
import org.marc4j.marc.VariableField;


public class OrgLocField extends FacetField {
    private static final String[] TAGS = new String[]{"008"};
    public OrgLocField(String desc, String label) {
        super(desc, label, TAGS);
    }

    @Override
    public String[] getValues(VariableField field){
        ControlField cf = (ControlField)field;
        String data = cf.getData();
        String key = null;
        if (data.length() >= 18) {
            key = data.substring(15,18).trim();
        }
        if ("xx".equals(key)) return new String[]{"undetermined"}; // HRWA-364
        String value = COUNTRY.get(key);
        if (value == null) {
            System.err.println("ERROR: no country code for 008 '" + key + "'");
            return new String[]{"" + key + " [008(15-17) CODE NOT FOUND]"};
        }
        return new String[]{value};
    }

}
