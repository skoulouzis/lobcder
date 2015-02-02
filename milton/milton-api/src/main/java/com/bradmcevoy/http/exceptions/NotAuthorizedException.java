package com.bradmcevoy.http.exceptions;

import com.bradmcevoy.http.Resource;

/**
 *  Indicates that the current user is not able to perform the requested operation
 *
 * This should not normally be used. Instead, a resource should determine if
 * a user can perform an operation in its authorised method
 *
 * However, this exception allows for cases where the authorised status can
 * only be determined during processing
 */
public class NotAuthorizedException extends MiltonException{
    private static final long serialVersionUID = 1L;

    public NotAuthorizedException(Resource r) {
        super(r);
    }

	public NotAuthorizedException() {
	}

	public NotAuthorizedException(String message) {
		super(message);
	}
	
	

}
