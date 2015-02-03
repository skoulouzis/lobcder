package com.ettrema.sso;

import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Response;

/**
 * Represents a method of authenticating users using remote sites. This is generally
 * when the user agent must be redirected to a login form on another site, so is not applicable
 * for webdav user agents which don't support html web page interaction.
 * 
 * Examples are SAML and OpenID
 *
 * @author brad
 */
public interface ExternalIdentityProvider {
	/**
	 * This will identify the provider when the user selects it (if selection is required)
	 * 
	 * @return 
	 */
	String getName();

	/**
	 * Begin the external authentication process. This will usually involve redirecting
	 * the user's browser to a remote site.
	 * 
	 * @param resource
	 * @param request
	 * @param response 
	 */
	void initiateExternalAuth(Resource resource, Request request, Response response);
}
