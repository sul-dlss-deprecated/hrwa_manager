package edu.columbia.ldpd.hrwa.solr;

import static edu.columbia.ldpd.hrwa.marc.Field.FIELDS;
import static org.junit.Assert.assertArrayEquals;

import java.io.InputStream;

import org.junit.BeforeClass;
import org.junit.Test;
import org.marc4j.marc.Record;

import edu.columbia.ldpd.hrwa.marc.Field;
import edu.columbia.ldpd.hrwa.marc.MarcCollection;
import edu.columbia.ldpd.hrwa.marc.impl.CreatorField;
import edu.columbia.ldpd.hrwa.marc.impl.DefaultField;
import edu.columbia.ldpd.hrwa.marc.impl.GACField;
import edu.columbia.ldpd.hrwa.marc.impl.LanguageField;
import edu.columbia.ldpd.hrwa.marc.impl.OrgLocField;
import edu.columbia.ldpd.hrwa.marc.impl.OrgTypeField;
import edu.columbia.ldpd.hrwa.marc.impl.SubjectField;
import edu.columbia.ldpd.hrwa.marc.impl.TitleField;
import edu.columbia.ldpd.hrwa.marc.impl.UrlField;


public class FacetFieldTest {
    private static Record marc8540838;
    private static Record marc8540895;
    @BeforeClass
    public static void setUp() throws Exception {
        InputStream input = FacetFieldTest.class.getResourceAsStream("/8540838.marc.xml");
        MarcCollection c = new MarcCollection(input);
        marc8540838 = c.getRecords().next();
        input.close();
        input = FacetFieldTest.class.getResourceAsStream("/8540895.marc.xml");
        c = new MarcCollection(input);
        marc8540895 = c.getRecords().next();
    }

    @Test
    public void subjectField() {
        String [] expected = new String[]{"Human rights (General)"};
        SubjectField test = (SubjectField)Field.FIELDS.get("subject");
        String [] actual = test.getValues(marc8540838);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void defaultField(){
        String [] expected = new String[]{"8540838"};
        DefaultField test = (DefaultField)Field.FIELDS.get("bib_key");
        String [] actual = test.getValues(marc8540838);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void titleField(){
        String [] expected = new String[]{"Amnesty International Philippines"};
        TitleField test = (TitleField)FIELDS.get("title");
        String [] actual = test.getValues(marc8540838);
        assertArrayEquals(expected, actual);
        expected = new String[]{"AMNESTY INTERNATIONAL PHILIPPINES"};
        test = (TitleField)FIELDS.get("title__sort");
        actual = test.getValues(marc8540838);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void creatorField(){
        String [] expected = new String[]{"Amnesty International. F\u00F8roya deild"}; // \u00F8 == ø (small letter o with slash) 
        CreatorField test = (CreatorField)FIELDS.get("creator_name");
        String [] actual = test.getValues(marc8540895);
        assertArrayEquals(expected, actual);
        expected = new String[]{"Amnesty International Pilipinas"};
        actual = test.getValues(marc8540838);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void languageField(){
        String[] expected = {"Faroese"};
        LanguageField test = (LanguageField)FIELDS.get("language");
        String [] actual = test.getValues(marc8540895);
        assertArrayEquals(expected, actual);
        expected = new String[]{"English"};
        actual = test.getValues(marc8540838);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void orgTypeField(){
        String [] expected = new String[]{"Non-governmental organizations"};
        OrgTypeField test = (OrgTypeField)FIELDS.get("organization_type");
        String [] actual = test.getValues(marc8540895);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void orgLocField(){
        String [] expected = new String[]{"Faroe Islands"};
        OrgLocField test = (OrgLocField)FIELDS.get("organization_based_in");
        String [] actual = test.getValues(marc8540895);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void urlField() {
        String [] expected = new String[]{"http://wayback.archive-it.org/1068/*/http://www.amnesty.fo/"};
        UrlField test = (UrlField)FIELDS.get("archived_urls");
        String [] actual = test.getValues(marc8540895);
        assertArrayEquals(expected, actual);
        test = (UrlField)FIELDS.get("original_urls");
        expected = new String[]{"http://www.amnesty.fo/"};
        actual = test.getValues(marc8540895);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void gacField() {
        String [] expected = new String[]{"Faroe Islands"};
        GACField test = (GACField)FIELDS.get("geographic_focus");
        String [] actual = test.getValues(marc8540895);
        assertArrayEquals(expected, actual);
    }
}
