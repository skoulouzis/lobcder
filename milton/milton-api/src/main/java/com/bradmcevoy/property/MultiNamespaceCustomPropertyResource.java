package com.bradmcevoy.property;

import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.property.PropertySource.PropertyMetaData;
import java.util.List;
import javax.xml.namespace.QName;

/**
 * A resource interface similar to CustomPropertyResource, except that it doesnt
 * use accessor objects, and it supports multiple namespaces.
 *
 * Properties are requested with qualified names, QNames, which include both
 * a namespace and a local name.
 * 
 * To implement this you should decide on a namespace for your custom properties
 * and then look for that component of the QName when implementing
 *
 * @author brad
 */
public interface MultiNamespaceCustomPropertyResource {
    Object getProperty( QName name );

	/**
	 * Update the property with the given typed value.
	 * 
	 * @param name - the qualified name of the property
	 * @param value - the new typed value
	 * @throws com.bradmcevoy.property.PropertySource.PropertySetException - if the input is invalid
	 * @throws NotAuthorizedException - if the current user is not allowed to set this value
	 */
    void setProperty( QName name, Object value ) throws PropertySource.PropertySetException, NotAuthorizedException;

	/**
	 * Get the metadata for the requested property, or return null if this
	 * implementation does not provide that property
	 * 
	 * It is also legitimate to return PropertyMetaData.UNKNOWN for unsupported properties
	 */
    PropertyMetaData getPropertyMetaData( QName name );

    List<QName> getAllPropertyNames();
}
