package com.bradmcevoy.property;

import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Response.Status;
import java.util.HashSet;
import java.util.Set;
import javax.xml.namespace.QName;

/**
 * This default implementation is to allow property access based on the request.
 * 
 * Ie if a user has PROPPATCH access they will be permitted to patch all properties.
 * If they have PROPFIND access they will be permitted to read all properties
 *
 * @author brad
 */
public class DefaultPropertyAuthoriser implements PropertyAuthoriser {

	@Override
    public Set<CheckResult> checkPermissions( Request request, Method method, PropertyPermission perm, Set<QName> fields, Resource resource ) {
        if( resource.authorise( request, request.getMethod(), request.getAuthorization() ) ) {
            return null;
        } else {
            // return all properties
            Set<CheckResult> set = new HashSet<CheckResult>();
            for( QName name : fields ) {
                set.add( new CheckResult( name, Status.SC_UNAUTHORIZED, "Not authorised", resource));
            }
            return set;
        }
    }
}
