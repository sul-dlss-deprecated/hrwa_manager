package edu.columbia.ldpd.hrwa.marc.z3950;


public class UnexpectedCloseException extends Z3950Exception {

    public UnexpectedCloseException(String msg) {
        super(msg);
    }
    public UnexpectedCloseException(String msg, Throwable t) {
        super(msg,t);
    }

}
