package edu.columbia.ldpd.hrwa.marc.impl;

import static edu.columbia.ldpd.hrwa.marc.translation.Translations.LANGUAGE;

import java.util.ArrayList;
import java.util.List;

import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;

import edu.columbia.ldpd.hrwa.marc.Field;


public class LanguageField extends FacetField {
    private static final String[] TAGS = new String[]{"041","008"};
    public LanguageField(String desc, String label) {
        super(desc, label, TAGS);
    }

    @Override
    public String[] getValues(Record marcRecord) {
        List<VariableField> fields = marcRecord.getVariableFields("041");
        ArrayList<String> results = new ArrayList<String>(fields.size());
        for (VariableField vf: fields){
            List<Subfield> sfs = ((DataField)vf).getSubfields();
            for (Subfield sf:sfs){
                String lcode = sf.getData();
                String map = LANGUAGE.get(lcode);
                if (map == null){
                    results.add(lcode + " [041 CODE NOT FOUND IN MARC LIST]");
                } else {
                    results.add(map);
                }
            }
        }
        if (results.size() == 0){
            ControlField df = (ControlField)marcRecord.getVariableField("008");
            String data = df.getData();
            String lcode = (data.length() < 38) ? null : data.substring(35, 38);
            String map = LANGUAGE.get(lcode);
            if (map == null){
                results.add(lcode + " [008(35-37) CODE NOT FOUND IN MARC LIST]");
            } else {
                results.add(map);
            }
        }

        return results.toArray(Field.EMPTY_STRING_ARRAY);
    }

}
