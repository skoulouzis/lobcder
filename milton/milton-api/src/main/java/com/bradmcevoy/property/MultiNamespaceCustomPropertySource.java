package com.bradmcevoy.property;

import com.bradmcevoy.http.*;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

/**
 *
 * @author brad
 */
public class MultiNamespaceCustomPropertySource implements PropertySource{

    public Object getProperty( QName name, Resource r ) {
        MultiNamespaceCustomPropertyResource cpr = (MultiNamespaceCustomPropertyResource) r;
        return cpr.getProperty( name );
    }

    public void setProperty( QName name, Object value, Resource r ) throws PropertySetException, NotAuthorizedException {
        MultiNamespaceCustomPropertyResource cpr = (MultiNamespaceCustomPropertyResource) r;
        cpr.setProperty( name, value );
    }

    public PropertyMetaData getPropertyMetaData( QName name, Resource r ) {
        if( r instanceof MultiNamespaceCustomPropertyResource ) {
            MultiNamespaceCustomPropertyResource cpr = (MultiNamespaceCustomPropertyResource) r;
            return cpr.getPropertyMetaData( name );
        } else {
            return null;
        }
    }

    /**
     * Just calls setProperty(.. null ..);
     *
     * @param name
     * @param r
     */
    public void clearProperty( QName name, Resource r ) throws PropertySetException, NotAuthorizedException {
        setProperty( name, null, r);
    }

    public List<QName> getAllPropertyNames( Resource r ) {
        if( r instanceof MultiNamespaceCustomPropertyResource ) {
            MultiNamespaceCustomPropertyResource cpr = (MultiNamespaceCustomPropertyResource) r;
            return cpr.getAllPropertyNames();
        } else {
            return new ArrayList<QName>();
        }

    }
}
