package com.bradmcevoy.property;

import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Response.Status;
import java.util.Set;
import javax.xml.namespace.QName;

/**
 *
 * @author brad
 */
public interface PropertyAuthoriser {

    public enum PropertyPermission {
        READ,
        WRITE
    }

    /*
     * Check if the current user has permission to write to the given fields
     *
     * Returns null or an empty set to indicate that the request should be allowed,
     * or a set of fields which the current user does not have access to if
     * there has been a violation
     * 
     */
    Set<CheckResult> checkPermissions(Request request, Method method, PropertyPermission perm, Set<QName> fields, Resource resource);



    /**
     * Describes a permission violation.
     */
    public class CheckResult {
        private final QName field;
        private final Status status;
        private final String description;
        private final Resource resource;

        public CheckResult( QName field, Status status, String description, Resource resource ) {
            this.field = field;
            this.status = status;
            this.description = description;
            this.resource = resource;
        }

        public String getDescription() {
            return description;
        }

        public QName getField() {
            return field;
        }

        public Status getStatus() {
            return status;
        }

        public Resource getResource() {
            return resource;
        }
    }
}
