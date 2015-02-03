package com.bradmcevoy.http.http11.auth;

import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;

/**
 * Provides a source of nonce values to be used in Digest authentication,
 * and a means to validate nonce values.
 *
 * Implementations should ensure that nonce values are available across all
 * servers in a cluster, and that they expire appropriately.
 *
 * Implementations should also ensure that nonce-count values are always
 * increasing, if provided.
 *
 * @author brad
 */
public interface NonceProvider {

   

    public enum NonceValidity {

        OK,
        EXPIRED,
        INVALID
    }

    /**
     * Check to see if the given nonce is known. If known, is it still valid
     * or has it expired.
     *
     * The request may also be considered invalid if the nonceCount value is
     * non-null and is not greater then any previous value for the valid nonce value.
     *
     * @param nonce - the nonce value given by a client to be checked.
     * @param nonceCount - may be null for non-auth requests. otherwise this should
     * be a monotonically increasing value. The server should record the previous
     * value and ensure that this value is greater then any previously given.
     * @return
     */
    NonceValidity getNonceValidity( String nonce, Long nonceCount );

    /**
     * Create and return a nonce value to be used for an authentication session.
     *
     *
     * @param resource - the resource being accessed.
     * @param request - the current request
     * @return - some string to be used as a nonce value.
     */
    String createNonce( Resource resource, Request request );
}
