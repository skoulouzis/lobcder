package com.bradmcevoy.http.http11.auth;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.AuthenticationHandler;
import com.bradmcevoy.http.DigestResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class DigestAuthenticationHandler implements AuthenticationHandler {

    private static final Logger log = LoggerFactory.getLogger( DigestAuthenticationHandler.class );
    private final NonceProvider nonceProvider;
    private final DigestHelper digestHelper;


    public DigestAuthenticationHandler( NonceProvider nonceProvider ) {
        this.nonceProvider = nonceProvider;
        this.digestHelper = new DigestHelper(nonceProvider);
    }

    public boolean supports( Resource r, Request request ) {
        Auth auth = request.getAuthorization();
        if( auth == null ) {
            return false;
        }
        boolean b;
        if( r instanceof DigestResource ) {
            DigestResource dr = (DigestResource) r;
            if( dr.isDigestAllowed()) {
                b = Auth.Scheme.DIGEST.equals( auth.getScheme() );
            } else {
                log.trace("digest auth is not allowed");
                b = false;
            }
        } else {
            log.trace( "resource is not an instanceof DigestResource" );
            b = false;
        }
        return b;
    }

	@Override
    public Object authenticate( Resource r, Request request ) {
        DigestResource digestResource = (DigestResource) r;
        Auth auth = request.getAuthorization();
        DigestResponse resp = digestHelper.calculateResponse(auth, r.getRealm(), request.getMethod());
        if( resp == null ) {
            log.debug("requested digest authentication is invalid or incorrectly formatted");
            return null;
        } else {
            Object o = digestResource.authenticate( resp );
            return o;
        }
    }

	@Override
    public String getChallenge( Resource resource, Request request ) {

        String nonceValue = nonceProvider.createNonce( resource, request );
        return digestHelper.getChallenge(nonceValue, request.getAuthorization(), resource.getRealm());
    }

    public boolean isCompatible( Resource resource ) {
        if ( resource instanceof DigestResource ) {
			DigestResource dr = (DigestResource) resource;
			return dr.isDigestAllowed();
		} else {
			return false;
		}
    }
}

