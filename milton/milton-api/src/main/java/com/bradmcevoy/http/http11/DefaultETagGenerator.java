package com.bradmcevoy.http.http11;

import com.bradmcevoy.http.Resource;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates the ETag as follows:
 *
 * - if the resource has a null unique id, returns null
 * - if the resource has a modified date it's hashcode is appended to the unique id
 * - the result is returned
 *
 * @author brad
 */
public class DefaultETagGenerator implements ETagGenerator {

    private static final Logger log = LoggerFactory.getLogger( DefaultETagGenerator.class );

    public String generateEtag( Resource r ) {
        log.trace( "generateEtag" );
        String s = r.getUniqueId();
        if( s == null ) {
            log.trace("no uniqueId, so no etag");
            return null;
        } else {
            Date dt = r.getModifiedDate();
            if( dt != null ) {
                log.trace("combine uniqueId with modDate to make etag");
                s = s + "_" + dt.hashCode();
            } else {
                log.trace("no modDate, so etag is just unique id");
            }
            return s;
        }
    }
}
