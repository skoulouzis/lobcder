package com.bradmcevoy.http.http11.auth;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.http11.auth.NonceProvider.NonceValidity;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class DigestHelper {

    private static final Logger log = LoggerFactory.getLogger( DigestHelper.class );

    private final NonceProvider nonceProvider;

    public DigestHelper(NonceProvider nonceProvider) {
        this.nonceProvider = nonceProvider;
    }
                
    public DigestResponse calculateResponse( Auth auth, String expectedRealm, Method method ) {
        // Check all required parameters were supplied (ie RFC 2069)
        if( ( auth.getUser() == null ) || ( auth.getRealm() == null ) || ( auth.getNonce() == null ) || ( auth.getUri() == null ) ) {
            log.warn( "missing params" );
            return null;
        }

        // Check all required parameters for an "auth" qop were supplied (ie RFC 2617)
        Long nc;
        if( "auth".equals( auth.getQop() ) ) {
            if( ( auth.getNc() == null ) || ( auth.getCnonce() == null ) ) {
                log.warn( "missing params: nc and/or cnonce" );
                return null;
            }
            nc = Long.parseLong( auth.getNc(), 16); // the nonce-count. hex value, must always increase
        } else {
            nc = null;
        }

        // Check realm name equals what we expected
        if( expectedRealm == null ) throw new IllegalStateException( "realm is null");
        if( !expectedRealm.equals( auth.getRealm() ) ) {
            log.warn( "incorrect realm: resource: " + expectedRealm + " given: " + auth.getRealm() );
            return null;
        }

        // Check nonce was a Base64 encoded (as sent by DigestProcessingFilterEntryPoint)
        if( !Base64.isArrayByteBase64( auth.getNonce().getBytes() ) ) {
            log.warn( "nonce not base64 encoded" );
            return null;
        }

        log.debug( "nc: " + auth.getNc());


        // Decode nonce from Base64
        // format of nonce is
        //   base64(expirationTime + "" + md5Hex(expirationTime + "" + key))
        String plainTextNonce = new String( Base64.decodeBase64( auth.getNonce().getBytes() ) );
        NonceValidity validity = nonceProvider.getNonceValidity( plainTextNonce, nc );
//        if( NonceValidity.INVALID.equals( validity ) ) {
//            log.debug( "invalid nonce: " + plainTextNonce );
//            return null;
//        } else if( NonceValidity.EXPIRED.equals( validity ) ) {
//            log.debug( "expired nonce: " + plainTextNonce );
//            // make this known so that we can add stale field to challenge
//            auth.setNonceStale( true );
//            return null;
//        }

        DigestResponse resp = toDigestResponse( auth, method );
        return resp;
    }

    public String getChallenge( String nonceValue, Auth auth, String actualRealm ) {

        String nonceValueBase64 = new String( Base64.encodeBase64( nonceValue.getBytes() ) );

        // qop is quality of protection, as defined by RFC 2617.
        // we do not use opaque due to IE violation of RFC 2617 in not
        // representing opaque on subsequent requests in same session.
        String authenticateHeader = "Digest realm=\"" + actualRealm
            + "\", " + "qop=\"auth\", nonce=\"" + nonceValueBase64
            + "\"";

        if( auth != null ) {
            if( auth.isNonceStale() ) {
                authenticateHeader = authenticateHeader
                    + ", stale=\"true\"";
            }
        }

        return authenticateHeader;
    }


    private DigestResponse toDigestResponse( Auth auth, Method m ) {
        DigestResponse dr = new DigestResponse(
            m,
            auth.getUser(),
            auth.getRealm(),
            auth.getNonce(),
            auth.getUri(),
            auth.getResponseDigest(),
            auth.getQop(),
            auth.getNc(),
            auth.getCnonce() );
        return dr;

    }
}
