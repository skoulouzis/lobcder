package com.bradmcevoy.http.webdav;

/**
 *
 * @author brad
 */
public class DefaultUserAgentHelper implements UserAgentHelper {

    public boolean isMacFinder( String userAgent ) {
        return userAgent != null && userAgent.contains( "WebDAVFS" ) && userAgent.contains( "Darwin" );
    }
}
