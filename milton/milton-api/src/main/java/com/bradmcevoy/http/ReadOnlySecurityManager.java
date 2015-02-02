package com.bradmcevoy.http;

import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.http11.auth.DigestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class ReadOnlySecurityManager implements SecurityManager{

    private Logger log = LoggerFactory.getLogger( ReadOnlySecurityManager.class );

    private final String realm;

    public ReadOnlySecurityManager( String realm ) {
        this.realm = realm;
    }

    public ReadOnlySecurityManager() {
        this.realm = null;
    }



    public Object authenticate( String user, String password ) {
        return "ok";
    }

    public Object authenticate( DigestResponse digestRequest ) {
        return digestRequest.getUser();
    }



    public boolean authorise( Request request, Method method, Auth auth, Resource resource ) {
        switch(method) {
            case GET: return true;
            case HEAD: return true;
            case OPTIONS: return true;
            case PROPFIND: return true;
        }
        log.debug("denying access to method {} on {}", method, request.getAbsolutePath());
        return false;
    }

    /**
     * Will return the configured realm if it is not null. Otherwise, will return
     * the requested hostname as the realm if it is not blank, otherwise will
     * return "ReadOnlyRealm"
     *
     * @param host - the requested host name
     * @return
     */
    public String getRealm(String host) {
        if( realm != null ) {
            return realm;
        } else {
            if( host != null && host.length() > 0 ) {
                return host;
            } else {
                return "ReadOnlyRealm";
            }
        }
    }



	public boolean isDigestAllowed() {
		return true;
	}

}
