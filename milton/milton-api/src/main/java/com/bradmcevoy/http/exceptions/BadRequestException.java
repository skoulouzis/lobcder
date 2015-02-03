package com.bradmcevoy.http.exceptions;

import com.bradmcevoy.http.Resource;

/**
 *
 * @author brad
 */
public class BadRequestException extends MiltonException {
    private static final long serialVersionUID = 1L;

    private final String reason;

    public BadRequestException(Resource r) {
        super(r);
        this.reason = null;
    }

    public BadRequestException(Resource r, String reason) {
        super(r);
        this.reason = reason;
    }
	
    public BadRequestException(String reason) {
        super();
        this.reason = reason;
    }	
	
    public BadRequestException(String reason, Throwable cause) {
        super(cause); 
        this.reason = reason;
    }		

    /**
     * Optional property, which describe the cause of the exception
     * @return
     */
    public String getReason() {
        return reason;
    }



}
