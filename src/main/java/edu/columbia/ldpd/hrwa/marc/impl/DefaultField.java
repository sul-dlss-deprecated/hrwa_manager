package edu.columbia.ldpd.hrwa.marc.impl;

import org.marc4j.marc.Record;

import edu.columbia.ldpd.hrwa.marc.Field;


public class DefaultField extends Field {

    public DefaultField(String desc, String label, String[] tags) {
        super(desc, label, tags);
    }

    @Override
    public boolean isFacet() {
        return false;
    }

    @Override
    public String[] getFacetValues(Record marcRecord) {
        return null;
    }

    @Override
    public boolean hasCustomNull() {
        return false;
    }

}
