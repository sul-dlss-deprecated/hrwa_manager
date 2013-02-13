package edu.columbia.ldpd.hrwa.marc.impl;

import static edu.columbia.ldpd.hrwa.marc.translation.Translations.GAC;

import java.util.ArrayList;
import java.util.List;

import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;

import edu.columbia.ldpd.hrwa.marc.Field;


public class GACField extends FacetField {
    private static String [] TAGS = new String[]{"043","965"};
    private static String[] GLOBAL_FOCUS = new String[]{"[Global focus]"};
    public GACField(String desc, String label) {
        super(desc, label, TAGS);
    }

    @Override
    public String[] getValues(Record marcRecord) {
        List<VariableField> fields = marcRecord.getVariableFields("043");
        /**
         * Check for special case "Global focus"
         * No 043 and one 965 = "Global focus" means that the website focus is global.
         * We did not want to input a non-MARC value in CLIO in service of the WorldCat export
         */
        if (fields == null || fields.size() == 0) {
            @SuppressWarnings("unchecked")
            List<VariableField> vfs = marcRecord.getVariableFields("965");
            for (VariableField vf:vfs){
                DataField df = (DataField)vf;
                if (df != null && df.getSubfield('a').getData().equals("Global focus")){
                    return GLOBAL_FOCUS;
                }
            }
            String bibkey = ((ControlField)marcRecord.getVariableField("001")).getData();
            System.out.println(bibkey + " ERROR: No 043 and no 965a == 'Global focus'");
            return Field.EMPTY_STRING_ARRAY;
        }
        ArrayList<String> results = new ArrayList<String>(fields.size());
        for (VariableField field:fields){
            DataField df = (DataField)field;
            List<Subfield> sfs = df.getSubfields();
            for (Subfield sf:sfs){
                // CLIO Records pad values with terminal '-'
                String data = sf.getData().trim().replaceFirst("[-]*$", "");
                String gac = GAC.get(data);
                if (gac == null) {
                    System.out.println("ERROR: no GAC for 043 code '" + data + "'");
                    results.add(data + " [043 CODE NOT FOUND IN MARC LIST]");
                } else {
                    results.add(gac);
                }
            }
        }
        return results.toArray(Field.EMPTY_STRING_ARRAY);
    }

}
