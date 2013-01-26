package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.LockableResource;
import com.bradmcevoy.http.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class WebDavResourceTypeHelper implements ResourceTypeHelper {

    private static final Logger log = LoggerFactory.getLogger( WebDavResourceTypeHelper.class );

    public List<QName> getResourceTypes( Resource r ) {
        if( r instanceof CollectionResource ) {
            ArrayList<QName> list = new ArrayList<QName>();
            QName qn = new QName( WebDavProtocol.NS_DAV.getName(), "collection" );
            list.add( qn );
            return list;
        } else {
            return null;
        }
    }

    //Need to create a ArrayList as Arrays.asList returns a fixed length list which
    //cannot be extended.
    public List<String> getSupportedLevels( Resource r ) {
        if( r instanceof LockableResource ) {
            return new ArrayList<String> (Arrays.asList( "1", "2" ));
        } else {
            return new ArrayList<String> (Arrays.asList( "1" ));
        }
    }
}
