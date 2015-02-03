package com.bradmcevoy.http.exceptions;

import com.bradmcevoy.http.Resource;

/**
 *  Indicates that the requested operation could not be performed because of
 * prior state. Ie there is an existing resource preventing a new one from being
 * created.
 */
public class ConflictException extends MiltonException {

	private final String message;

	/**
	 * The resource idenfitied by the URI.
	 *
	 * @param r
	 */
	public ConflictException(Resource r) {
		super(r);
		this.message = "Conflict exception: " + r.getName();
	}

	public ConflictException(Resource r, String message) {
		super(r);
		this.message = message;
	}

	public ConflictException() {
		this.message = "Conflict";
	}

	public ConflictException(String message) {
		this.message = message;
	}
	
	

	@Override
	public String getMessage() {
		return message;
	}
}
