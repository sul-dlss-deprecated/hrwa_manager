package edu.columbia.ldpd.hrwa.marc.z3950;

import java.util.HashSet;
import java.util.Set;


public class Query {
    Set<String> VALS = new HashSet<String>(java.util.Arrays.asList(new String[]{
            "CCL", "S-CCL", "CQL", "S-CQL", "PQF", "C2", "ZSQL", "CQL-TREE"
    }));
    private String m_type = null;
    private String m_query;
    private String m_encoding;
    private String m_encoding_type;
    public Query(String type, String query) {
        type = type.toUpperCase();
        if ("CCL".equals(type)) {
            m_type = "RPN";
        } else if ("S-CCL".equals(type)){
            m_type = "S-CCL";
        } else if ("CQL".equals(type)){
            m_type = "CQL";
        } else if ("S-CQL".equals(type)){
            m_type = "S-CQL";
        } else if ("PQF".equals(type)){
            m_type = "PQF";
        } else if ("C2".equals(type)){
            m_type = "C2";
        } else if ("ZSQL".equals(type)){
            m_type = "ZSQL";
        } else if ("CQL-TREE".equals(type)){
            m_type = "CQL-TREE";
        }
    }
}
