package com.ettrema.http.acl;

import com.bradmcevoy.http.Auth;

/**
 * Transforms various sources of user and group information into ACL compatible
 * Principal objects
 *
 * @author brad
 */
public interface PrincipalFactory {
	Principal fromAuth(Auth auth);
}
