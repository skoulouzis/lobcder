package com.bradmcevoy.http;

public class RequestParseException extends Exception {
    
    private static final long serialVersionUID = 1L;

    public RequestParseException(String msg, Throwable cause) {
        super(msg,cause);
    }
    
}
