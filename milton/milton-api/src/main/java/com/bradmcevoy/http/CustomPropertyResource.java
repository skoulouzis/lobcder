package com.bradmcevoy.http;

import java.util.Set;

/**
 * Extension to PropFindableResource which allows custom
 * properties to be returned.
 *
 * See MultiNamespaceCustomPropertySource to support multiple namespaces
 *
 * @author brad
 */
public interface CustomPropertyResource extends PropFindableResource {

    /**
     * 
     * @return - a list of all the properties of this namespace which exist
     * on this resource
     */
    public Set<String> getAllPropertyNames();

    /**
     * Return an accessor for the given property if it is supported or known. Note
     * that this includes cases where the value of the property is null
     *
     * @param name
     * @return - null if the property is unknown or not supported. Otherwise an
     * accessor to the property
     */
    public CustomProperty getProperty(String name);

    /**
     * Returns a URI used as a namespace for these properties.
     * 
     * @return
     */
    public String getNameSpaceURI();



}
