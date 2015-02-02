package com.bradmcevoy.http.entity;

import com.bradmcevoy.http.Response;

/**
 * Represents a means of writing entities to the HTTP response. For most
 * containers this is trivial, simply write to the output stream on the Responsee
 * 
 * However, some containers have an architecture where the content transmission can
 * be deferred, and this abstraction exists to support that.
 * 
 * For example, Restlet uses an API with deferred content transmission. But also
 * SEDA (http://www.eecs.harvard.edu/~mdw/proj/seda/) servers generally will want
 * to use a seperate thread pool for generating content from that which processes
 * request headers etc
 *
 * @author brad
 */
public interface EntityTransport {
	public void sendResponseEntity(Response response) throws Exception;

	public void closeResponse(Response response);
}
