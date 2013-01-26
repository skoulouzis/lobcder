package com.ettrema.http.acl;

import com.bradmcevoy.http.Resource;

/**
 * Indicates a principle which is identifiable by a URL, like a user or
 * an application defined group
 *
 * @author brad
 */
public interface DiscretePrincipal extends Principal, Resource{

        
    /**
     * A URL to identify this principle. Note the relationship between this and
	 * the AccessControlledResource.getPrincipalURL method which returns the principal
	 * that owns the resource.
	 * 
	 * It is assumed that where a AccessControlledResource instance is also a DiscretePrincipal
	 * that the getPrincipalURL method will return the url of the resource/principal
	 * 
	 * In other words, we make the semantic decision that a principle owns itself.
     *
     * @return
     */
    public String getPrincipalURL();


}
