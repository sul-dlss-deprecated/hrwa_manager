package edu.columbia.ldpd.hrwa.marc.z3950;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class Connection {
    private static final String[] NOT_IMPLEMENTED = new String[]{
      "piggyback",
      "schema",
      "async"
    };

    private static final String[] SEARCH_ATTRS = new String[]{
       "smallSetUpperBound",
       "largeSetUpperBound",
       "mediumSetPresentNumber",
       "smallSetElementSetNames",
       "mediumSetElementSetNames"
    };

    private static final String[] INIT_ATTRS = new String[]{
        "user",
        "password",
        "group",
        "maximumRecordSize",
        "preferredMessageSize",
        "lang",
        "charset",
        "implementationId",
        "implementationName",
        "implementationVersion"
     };

    private static final String[] OTHER_ATTRS = new String[]{
        "databaseName",
        "namedResultSets",
        "preferredRecordSyntax",
        "elementSetName",
        "presentChunk",
        "targetImplementationId",
        "targetImplementationName",
        "targetImplementationVersion",
        "host",
        "port"
    };

    private static final HashMap<String,String> SCAN_ZOOM_TO_Z3950 = scanMap();

    public static final String[] ATTRS = allAttrs(SEARCH_ATTRS, INIT_ATTRS, OTHER_ATTRS, SCAN_ZOOM_TO_Z3950);

    public static String DEFAULT_RESULT_SET_NAME = "";

    private static String[] allAttrs(String[] search, String[] init, String[] others, Map<String,String>scan){
        int len = search.length + init.length + scan.size() + ErrorHandler.ATTRS.length + others.length ;
        ArrayList<String> result = new ArrayList<String>(search.length + init.length + scan.size());
        for (String s:search) result.add(s);
        for (String s:init) result.add(s);
        for (String s:others) result.add(s);
        for (String s:ErrorHandler.ATTRS) result.add(s);
        result.addAll(scan.keySet());
        return result.toArray(search);
    }

    private static HashMap<String,String> scanMap(){
        HashMap<String,String> result = new HashMap<String,String>(3);
        result.put("stepSize", "stepSize");
        result.put("numberOfEntries", "numberOfTermsRequested");
        result.put("responsePosition", "preferredPositionInResponse");
        return result;
    }

    private final String[] m_queryTypes = new String[]{"S-CQL","S-CCL","RPN","ZSQL"};

    private Client m_client = null;

    private final String m_hostName;
    private final int m_port;
    private final Map<String,String> m_opts;
    private int m_resultCtr = 0;
    private int m_lastCtr = 0;
    private int m_namedResultSets = 1;
    private final String m_elementSetName = "F";
    private final String m_preferredRecordSyntax = "USMARC";
    private final int m_preferredMessageSize = 32;
    private final int m_maximumRecordSize = 32;
    private final int m_stepSize = 0;
    private final int m_numberOfEntries = 20;
    private final int m_responsePosition = 1;
    private String m_dbName = "Default";
    private String m_implementationId = "HRWAZ3950";
    private String m_implementationName = "HRWA Z.3950 Client";
    private String m_implementationVersion = "1.0";
    private final String m_lang = null;
    private final String m_charset = null;
    private final String m_user = null;
    private final String m_password = null;
    private final String m_group = null;
    private final int m_presetnChunk = 20;

    public Connection(String hostName, int port, Map<String,String> opts) throws Z3950Exception {
        this(hostName, port, true, opts);
    }

    public Connection(String hostName, int port, boolean connect, Map<String,String> opts) throws Z3950Exception {
        m_hostName = hostName;
        m_port = port;
        m_opts = opts;
        if (connect) connect();
    }

    public void setDbName(String dbName){
        m_dbName = dbName;
    }

    public void connect() throws Z3950Exception {
        m_resultCtr++;
        m_lastCtr = m_resultCtr;
        if (m_client != null && m_client.isConnected()) return;
        String[] opts;
        if (m_namedResultSets > 0) {
            opts = new String[]{"namedResultSets"};
        } else opts = new String[]{};

        m_client = new Client(m_hostName, m_port, opts);
        m_namedResultSets = m_client.getNumResultSets();
        m_implementationId = m_client.getImplementationId();
        m_implementationName = m_client.getImplementationName();
        m_implementationVersion = m_client.getImplementationVersion();
        UserInformationField uif = m_client.initialResponse().getUserInformationField();
        if (uif != null){
            OIDValue df = uif.getDirectReference();
            if (df != null && df.equals(OIDRegistry.Z3950_USR_PRIVATE_OCLC_INFO_OID)) {
                Encoding e = uif.getEncoding();
                if (e.getFailReason() != null) {
                    throw new UnexpectedCloseException("OCLC_Info " + e.getFailReason() + e.text());
                }
            }
        }
    }

    public Response search(String query) throws Z3950Exception {
        if (m_client == null) connect();
        String [] dbNames = m_dbName.split("[+]");
        m_client.setDbNames(dbNames);
        String rsn = resultSetName();
        Response r = m_client.search(query,rsn,null);
        m_resultCtr++;
        return new ResultSet(r,rsn,m_resultCtr);
    }

    public Response scan(String query) throws Z3950Exception {
        if (m_client == null) connect();
        return new ScanSet(m_client.scan(query));
    }

    public void close() throws Z3950Exception {
        if (m_client != null) {
            m_client.close();
        }
    }

    private String resultSetName() {
        if (m_namedResultSets > 0) {
            return "rs" + m_resultCtr;
        } else {
            return DEFAULT_RESULT_SET_NAME;
        }
    }


}
