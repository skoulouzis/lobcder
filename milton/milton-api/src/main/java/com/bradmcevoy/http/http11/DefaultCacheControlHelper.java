package com.bradmcevoy.http.http11;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.Response;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class DefaultCacheControlHelper implements CacheControlHelper {

    private static final Logger log = LoggerFactory.getLogger( DefaultCacheControlHelper.class );
    private boolean usePrivateCache = false;

	@Override
    public void setCacheControl( final GetableResource resource, final Response response, Auth auth ) {
        Long delta = resource.getMaxAgeSeconds( auth );
        if( log.isTraceEnabled() ) {
            log.trace( "setCacheControl: " + delta + " - " + resource.getClass() );
        }
        if( delta != null && delta > 0 ) {
            if( usePrivateCache && auth != null ) {
                response.setCacheControlPrivateMaxAgeHeader( delta );
                //response.setCacheControlMaxAgeHeader(delta);
            } else {
                response.setCacheControlMaxAgeHeader( delta );
            }
            // Disable, might be interfering with IE.. ?
//            Date expiresAt = calcExpiresAt( new Date(), delta.longValue() );
//            if( log.isTraceEnabled() ) {
//                log.trace( "set expires: " + expiresAt );
//            }
//            response.setExpiresHeader( expiresAt );
        } else {
            response.setCacheControlNoCacheHeader();
        }
    }

    public static Date calcExpiresAt( Date modifiedDate, long deltaSeconds ) {
        long deltaMs = deltaSeconds * 1000;
        long expiresAt = System.currentTimeMillis() + deltaMs;
        return new Date( expiresAt );
    }
}
