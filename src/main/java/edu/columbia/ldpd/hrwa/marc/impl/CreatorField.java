package edu.columbia.ldpd.hrwa.marc.impl;

import java.util.Iterator;
import java.util.List;

import org.marc4j.marc.DataField;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;


public class CreatorField extends FacetField {
    private static final String [] TAGS = new String[]{"110", "710", "100"};

    public CreatorField(String desc, String label) {
        super(desc, label, TAGS);
    }

    @Override
    public String [] getValues(VariableField vf) {
        DataField df = (DataField)vf;
        String value = df.getSubfield('a').getData();
        String tag = df.getTag();
        if (tag.equals("100")) {
            Subfield csf = df.getSubfield('c');
            if (csf != null) {
                String data = csf.getData();
                int dlen = data.length();
                StringBuffer sb = new StringBuffer(value.length() + data.length() + 1);
                sb.append(value);
                sb.append(' ');
                char c = data.charAt(dlen - 1);
                if (c == ',' || c == '.'){
                    sb.append(data,0,dlen-1);
                } else sb.append(data);
                value = sb.toString();
            }
        } else { // 110 & 710
            List<Subfield> sfs = df.getSubfields('b');
            if (sfs.size() > 0) {
                StringBuffer buf = new StringBuffer(value.length() + sfs.size() * 5);
                buf.append(value);
                Iterator<Subfield> sfi = sfs.iterator();
                while(sfi.hasNext()){
                    buf.append(' ');
                    buf.append(sfi.next().getData());
                }
                int last = buf.length() - 1;
                char c  = buf.charAt(last);
                if (c == ',' || c == '.'){
                    buf.setLength(last);
                }
                value = buf.toString();
            }
        }
        return new String[]{trim(value).toString()};
    }

    @Override
    public boolean hasCustomNull(){
        return true;
    }

    @Override
    public String[] getNullValues(){
        return new String[]{"[unknown]"};
    }

    private CharSequence trim(CharSequence src){
        int slen = src.length() - 1;
        char c = src.charAt(slen);
        if (c == '.' || c == ','){
            return src.subSequence(0,slen);
        } else return src;
    }

}
