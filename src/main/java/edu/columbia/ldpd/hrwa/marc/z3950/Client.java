package edu.columbia.ldpd.hrwa.marc.z3950;

import java.util.Map;


public class Client {
    private final Object m_sock = null;

    private String[] m_dbNames = null;

    public Client(String host, int port, String[] options){

    }

    public void setDbNames(String [] dbNames) {
        m_dbNames = dbNames;
    }

    public Response search(String query, String resultSetName, Map<String,String>opts){
        throw new UnsupportedOperationException("unimplemented");
    }

    public Response scan(String query){
        throw new UnsupportedOperationException("unimplemented");
    }

    public void close() {
        throw new UnsupportedOperationException("unimplemented");
    }

    /**
     * return counter for named result sets
     * @return
     */
    public int getNumResultSets(){
        throw new UnsupportedOperationException("unimplemented");
    }

    public String getImplementationId(){
        throw new UnsupportedOperationException("unimplemented");
    }

    public String getImplementationName(){
        throw new UnsupportedOperationException("unimplemented");
    }

    public String getImplementationVersion(){
        throw new UnsupportedOperationException("unimplemented");
    }

    public boolean isConnected(){
        return m_sock != null;
    }

    public Response initialResponse() {
        return null;
    }

}
