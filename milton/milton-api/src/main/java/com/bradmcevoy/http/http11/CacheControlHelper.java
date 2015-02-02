package com.bradmcevoy.http.http11;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.Response;

/**
 * Generates the cache-control header on the response
 *
 * @author brad
 */
public interface CacheControlHelper {
    /**
     *
     * @param resource
     * @param response
     * @param auth
     * @param notMod - true means we're sending a not modified response
     */
    void setCacheControl( final GetableResource resource, final Response response, Auth auth);
}
