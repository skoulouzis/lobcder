package com.bradmcevoy.http.http11.auth;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.Auth.Scheme;
import com.bradmcevoy.http.AuthenticationHandler;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class SecurityManagerBasicAuthHandler implements AuthenticationHandler {

    private static final Logger log = LoggerFactory.getLogger(SecurityManagerBasicAuthHandler.class);
    private final com.bradmcevoy.http.SecurityManager securityManager;

    public SecurityManagerBasicAuthHandler(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public boolean supports(Resource r, Request request) {
        Auth auth = request.getAuthorization();
        if (auth == null) {
            return false;
        }

        if (log.isTraceEnabled()) {
            log.trace("supports basic? requested scheme: " + auth.getScheme());
        }
        return auth.getScheme().equals(Scheme.BASIC);
    }

    public Object authenticate(Resource resource, Request request) {
        log.debug("authenticate");
        Auth auth = request.getAuthorization();
        Object o = securityManager.authenticate(auth.getUser(), auth.getPassword());
        log.debug("result: " + o);
        return o;
    }

    public String getChallenge(Resource resource, Request request) {
        String realm = securityManager.getRealm(request.getHostHeader());
        return "Basic realm=\"" + realm + "\"";
    }

    public boolean isCompatible(Resource resource) {
        return true;
    }

    public SecurityManager getSecurityManager() {
        return securityManager;
    }
}
