package com.bradmcevoy.http.http11.auth;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.Auth.Scheme;
import com.bradmcevoy.http.AuthenticationHandler;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class BasicAuthHandler implements AuthenticationHandler {

    private static final Logger log = LoggerFactory.getLogger( BasicAuthHandler.class );

    public boolean supports( Resource r, Request request ) {
        Auth auth = request.getAuthorization();
        if( auth == null ) {
            return false;
        }
        log.trace( "supports: {}", auth.getScheme() );
        return auth.getScheme().equals( Scheme.BASIC );
    }

    public Object authenticate( Resource resource, Request request ) {
        log.trace( "authenticate" );
        Auth auth = request.getAuthorization();
        Object o = resource.authenticate( auth.getUser(), auth.getPassword() );
        log.trace( "result: {}", o );
        return o;
    }

    public String getChallenge( Resource resource, Request request ) {
        return "Basic realm=\"" + resource.getRealm() + "\"";
    }

    public boolean isCompatible( Resource resource ) {
        return true;
    }
}
