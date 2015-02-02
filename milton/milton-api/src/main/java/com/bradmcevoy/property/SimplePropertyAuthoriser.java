package com.bradmcevoy.property;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Response.Status;
import java.util.HashSet;
import java.util.Set;
import javax.xml.namespace.QName;

/**
 * Very basic implementation for development and prototyping
 *
 * Allows all logged in access
 *
 * @author brad
 */
public class SimplePropertyAuthoriser implements PropertyAuthoriser {

	@Override
    public Set<CheckResult> checkPermissions( Request request, Method method, PropertyPermission perm, Set<QName> fields, Resource resource ) {
        Auth auth = request.getAuthorization();
        if( auth != null && auth.getTag() != null ) {
            return null;
        } else {
            Set<CheckResult> s = new HashSet<CheckResult>();
            for( QName qn : fields ) {
                s.add(new CheckResult( qn, Status.SC_UNAUTHORIZED, "Not logged in", resource));
            }
            return s;
        }
    }

}
