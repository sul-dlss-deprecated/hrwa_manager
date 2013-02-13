package edu.columbia.ldpd.hrwa.marc.z3950;


public class Z3950Exception extends Exception {
    public Z3950Exception(String msg){
        super(msg);
    }
    public Z3950Exception(String msg, Throwable t){
        super(msg,t);
    }
}
