package edu.columbia.ldpd.hrwa.marc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;

import edu.columbia.ldpd.hrwa.marc.impl.CreatorField;
import edu.columbia.ldpd.hrwa.marc.impl.DefaultField;
import edu.columbia.ldpd.hrwa.marc.impl.GACField;
import edu.columbia.ldpd.hrwa.marc.impl.LanguageField;
import edu.columbia.ldpd.hrwa.marc.impl.OrgLocField;
import edu.columbia.ldpd.hrwa.marc.impl.OrgTypeField;
import edu.columbia.ldpd.hrwa.marc.impl.SubjectField;
import edu.columbia.ldpd.hrwa.marc.impl.TitleField;
import edu.columbia.ldpd.hrwa.marc.impl.UrlField;

public abstract class Field {
    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    /**
     * @deprecated Use {@link #EMPTY_STRING_ARRAY} instead
     */
    @Deprecated
    protected static final String[] STRING_TYPE = EMPTY_STRING_ARRAY;
    private String m_desc;
    private String m_label;
    private String [] m_tags;
    private Map<String,char[]> m_subfields;
    protected Field(String desc, String label, String[] tags) {
        setDescription(desc);
        setLabel(label);
        setTags(tags);
    }

    public String getDescription() {
        return m_desc;
    }


    public void setDescription(String m_desc) {
        this.m_desc = m_desc;
    }


    public String getLabel() {
        return m_label;
    }


    public void setLabel(String m_label) {
        this.m_label = m_label;
    }


    public String[] getTags() {
        return m_tags;
    }

    public String subfieldsFor(String tag){
        if (m_subfields.containsKey(tag)) {
          return new String(m_subfields.get(tag));
        } else {
            return "";
        }
    }


    public void setTags(String[] newTags) {
        String [] tags = new String[newTags.length];
        HashMap<String,char[]> subfields = new HashMap<String,char[]>(newTags.length);
        Pattern p = Pattern.compile("^([\\d]{3})([a-z]*)\\s?(.?)(.?)");
        for (int i=0;i<newTags.length;i++){
            Matcher m = p.matcher(newTags[i]);
            m.find();
            tags[i] = m.group(1);
            if (m.group(2) != null){
                subfields.put(tags[i], m.group(2).toCharArray());
            }
        }
        this.m_tags = tags;
        this.m_subfields = subfields;
    }

    @SuppressWarnings("unchecked")
    public String[] getValues(Record marcRecord){

        List<VariableField> list = marcRecord.getVariableFields(getTags());
        if (list != null) {
            ArrayList<String> results = new ArrayList<String>(list.size());

            for(VariableField vf:list){
                String [] values = getValues(vf);
                for (String value:values) results.add(value);
            }
            if (results.size() > 0) return results.toArray(Field.EMPTY_STRING_ARRAY);
        }
        return getNullValues();
    }

    public String[] getValues(VariableField vf){
        ArrayList<String> result = new ArrayList<String>(2);
        if (vf == null){
            return Field.EMPTY_STRING_ARRAY;
        }
        if (vf instanceof ControlField) {
            result.add(((ControlField)vf).getData());
        } else {
            DataField df = (DataField)vf;
            char[] sfCodes = m_subfields.get(df.getTag());
            if (sfCodes != null){
                for (char code:sfCodes){
                    Subfield sf = df.getSubfield(code);
                    if (sf != null)
                        result.add(df.getSubfield(code).getData());
                }
            } else { // get 'em all
                for (Object o:df.getSubfields()){
                    Subfield sf = (Subfield)o;
                    result.add(sf.getData());
                }
            }
        }
        return result.toArray(EMPTY_STRING_ARRAY);
    }

    public String[] getNullValues(){
        return new String[0];
    }


    public abstract boolean isFacet();

    public abstract String[] getFacetValues(Record marcRecord);

    public abstract boolean hasCustomNull();


    public static Map<String, Field> FIELDS = new HashMap<String, Field>();
    static {
        FIELDS.put( "bib_key",
                new DefaultField("CLIO bib key, the unique identifier for a site/org",
                        "Site/Organization ID",
                        new String[]{"001"}));
        FIELDS.put( "marc_005_last_modified",
                new DefaultField("Marc last modified field, updated when a CLIO record is modified",
                        "Marc 005 Last Modified",
                        new String[]{"005"}));
        FIELDS.put( "title",
                new TitleField("Website title",
                        "Website title",
                        false));
        FIELDS.put( "title__sort",
                new TitleField("Website title sort field",
                        "Website title Sort Field",
                        true));
        FIELDS.put( "alternate_title",
                new DefaultField("Alternate website title",
                        "Alternate Website Title",
                        new String[]{"246a"}));
        FIELDS.put( "creator_name",
                new CreatorField("Includes organization names and individual names (i.e blog creators)",
                        "Creator Name"));
        FIELDS.put( "organization_type",
                new OrgTypeField("Possible values: Non-governmental institutions, National human rights institutions, Individual site creators, Other organization types",
                        "Organization Type"));
        FIELDS.put( "organization_based_in",
                new OrgLocField("Organization/site based in",
                        "Organization/Site Based in"));
        FIELDS.put( "geographic_focus",
                new GACField("Organization/Site Geographic Focus",
                        "Organization/Site Geographic Focus"));
        FIELDS.put( "subject",
                new SubjectField("Organization/Site Subject Focus",
                        "Organization/Site Subject Focus"));
        FIELDS.put( "summary",
                new DefaultField("summary",
                        "Summary",
                        new String[]{"520a"}));
        FIELDS.put( "language",
                new LanguageField("Website Language(s)",
                        "Website Language(s)"));
        FIELDS.put( "original_urls",
                new UrlField("original URL(s)",
                        "URL(s) at Time of Capture",
                        false));
        FIELDS.put( "archived_urls",
                new UrlField("Archived URL(s)",
                        "See Archived Site",
                        true));
    }
}
