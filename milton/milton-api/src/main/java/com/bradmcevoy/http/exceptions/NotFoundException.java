package com.bradmcevoy.http.exceptions;

/**
 *
 * @author bradm
 */
public class NotFoundException extends MiltonException {
    private static final long serialVersionUID = 1L;

    private final String reason;

	
    public NotFoundException(String reason) {
        super();
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
