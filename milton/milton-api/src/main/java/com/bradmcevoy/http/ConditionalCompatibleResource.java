package com.bradmcevoy.http;

import com.bradmcevoy.http.Request.Method;

/**
 * Resources may implement this, to allow them to decide dynamically whether
 * to support particular HTTP methods.
 *
 * Note that this must not be used for authorisation, use the authorise method
 * on Resource instead.
 *
 * This should only be used to determine whether a resource permits a certain
 * http method regardless of user or application state. Ie is should reflect
 * a configuration choice, and as such be static for the lifetime of the application
 *
 * @author brad
 */
public interface ConditionalCompatibleResource {
    /**
     * Return whether or not this resource might be compatible with the given
     * HTTP method.
     *
     * Note that a resource MUST also implement the corresponding milton interface
     * (E.g. GetableResource)
     *
     * @param m - the HTTP method in the current request
     * @return - false to say that this resource must not handle this request, true
     * to indicate that it might, if it also implements the appropriate method interface
     */
    boolean isCompatible(Method m);
}
