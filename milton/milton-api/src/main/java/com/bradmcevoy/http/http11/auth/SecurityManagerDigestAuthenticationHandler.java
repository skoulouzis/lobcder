package com.bradmcevoy.http.http11.auth;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.AuthenticationHandler;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.SecurityManager;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class SecurityManagerDigestAuthenticationHandler implements AuthenticationHandler {

    private static final Logger log = LoggerFactory.getLogger( SecurityManagerDigestAuthenticationHandler.class );
    private final NonceProvider nonceProvider;

    private final SecurityManager securityManager;
    private final DigestHelper digestHelper;

    public SecurityManagerDigestAuthenticationHandler( NonceProvider nonceProvider, SecurityManager securityManager ) {
        this.nonceProvider = nonceProvider;
        this.securityManager = securityManager;
        digestHelper = new DigestHelper(nonceProvider);
    }

    public SecurityManagerDigestAuthenticationHandler(SecurityManager securityManager) {
		Map<UUID, Nonce> nonces = new ConcurrentHashMap<UUID, Nonce>();
		int nonceValiditySeconds = 60*60*24;
		ExpiredNonceRemover expiredNonceRemover = new ExpiredNonceRemover(nonces, nonceValiditySeconds);
		this.nonceProvider = new SimpleMemoryNonceProvider(nonceValiditySeconds, expiredNonceRemover, nonces);
        this.securityManager = securityManager;
        digestHelper = new DigestHelper(nonceProvider);
    }

    public boolean supports( Resource r, Request request ) {
        Auth auth = request.getAuthorization();
        if( auth == null ) {
            return false;
        }
        return  Auth.Scheme.DIGEST.equals( auth.getScheme() );
    }

    public Object authenticate( Resource r, Request request ) {
        Auth auth = request.getAuthorization();
        DigestResponse resp = digestHelper.calculateResponse(auth, securityManager.getRealm(request.getHostHeader()), request.getMethod());
        if( resp == null ) {
            log.debug("requested digest authentication is invalid or incorrectly formatted");
            return null;
        } else {
            Object o = securityManager.authenticate( resp );
            return o;
        }

    }

    public String getChallenge( Resource resource, Request request ) {
        String nonceValue = nonceProvider.createNonce( resource, request );
        return digestHelper.getChallenge(nonceValue, request.getAuthorization(), securityManager.getRealm(request.getHostHeader()));
    }

    public boolean isCompatible( Resource resource ) {
        return true;
    }
}

