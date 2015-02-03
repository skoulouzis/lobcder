package com.bradmcevoy.property;

import com.bradmcevoy.http.Handler;

/**
 * A type of method handler which  does property permission checking.
 *
 * This interface is just so that the HTTP manager can inject the permission
 * property service
 *
 * @author brad
 */
public interface PropertyHandler extends Handler{
    public PropertyAuthoriser getPermissionService();

    public void setPermissionService( PropertyAuthoriser permissionService );
}
