package com.bradmcevoy.http.http11;

import com.bradmcevoy.http.AuthenticationService;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Response;
import com.bradmcevoy.http.Response.Status;

/**
 * Used to generate error pages from ResponseHandlers.
 * 
 * Can be customised to produce custom pages, such as by including JSP's etc
 *
 * @author brad
 */
public interface ContentGenerator {
	/**
	 * Generate an error page for the given status
	 * 
	 * @param request
	 * @param response
	 * @param status 
	 */
	void generate(Resource resource, Request request, Response response, Status status);
	
	/**
	 * Generate content for a login page, generally when unauthorised
	 * 
	 * @param request
	 * @param response
	 * @param authenticationService 
	 */
	void generateLogin(Resource resource, Request request, Response response, AuthenticationService authenticationService);
}
