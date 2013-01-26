package com.ettrema.sso;

import com.bradmcevoy.http.AuthenticationHandler;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;

/**
 * This is a post resource-resolution authentication handler. 
 * 
 * It assumes that the SsoResourceFactory has populated the _sso_user
 * request attribute if appropriate
 *
 * @author brad
 */
public class SsoAuthenticationHandler implements AuthenticationHandler {


	
	public boolean supports(Resource r, Request request) {
		boolean b = request.getAttributes().get("_sso_user") != null;		
		return b;
	}

	public Object authenticate(Resource resource, Request request) {
		return request.getAttributes().get("_sso_user");
	}

	public String getChallenge(Resource resource, Request request) {
		return null;
	}

	public boolean isCompatible(Resource resource) {
		return true;
	}	
}
