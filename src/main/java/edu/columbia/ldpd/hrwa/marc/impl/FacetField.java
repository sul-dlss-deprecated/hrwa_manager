package edu.columbia.ldpd.hrwa.marc.impl;

import org.marc4j.marc.Record;

import edu.columbia.ldpd.hrwa.marc.Field;


public class FacetField extends Field {

    public FacetField(String desc, String label, String[] tags) {
        super(desc, label, tags);
    }

    @Override
    public boolean isFacet() {
        return true;
    }

    @Override
    public String[] getFacetValues(Record marcRecord) {
        return getValues(marcRecord);
    }

    @Override
    public boolean hasCustomNull() {
        return false;
    }

}
