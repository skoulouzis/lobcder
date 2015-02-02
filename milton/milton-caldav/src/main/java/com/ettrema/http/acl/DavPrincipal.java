package com.ettrema.http.acl;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.Resource;

/**
 * A common webdav group
 * 
 * @author brad
 */
public interface DavPrincipal extends Principal {

    /**
     * Does the current user match this group
     *
     * @param auth
     * @return
     */
    boolean matches( Auth auth, Resource current );


}
