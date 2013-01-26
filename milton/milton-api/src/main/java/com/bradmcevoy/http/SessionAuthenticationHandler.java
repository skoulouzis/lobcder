package com.bradmcevoy.http;

/**
 *
 * @author brad
 * 
 * A handler used to implement session based authentication. An implementation
 * will usually integrate with a forms based login and the HttpSession object
 * 
 */
public interface SessionAuthenticationHandler {
    Auth getSessionAuthentication(Request request);
}
