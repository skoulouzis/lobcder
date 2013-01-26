package com.bradmcevoy.http.exceptions;

import com.bradmcevoy.http.Resource;

/**
 *  Base class for exceptions during processing requests on resources
 */
public abstract class MiltonException extends Exception {
    private static final long serialVersionUID = 1L;
    private Resource resource;

    public MiltonException(Resource resource) {
        this.resource = resource;
    }

	public MiltonException() {
	}

	public MiltonException(String message) {
		super(message);
	}
	
	public MiltonException(Throwable cause) {
		super(cause);
	}	

    public Resource getResource() {
        return resource;
    }

    
}
