package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.PropFindableResource;

/**
 * Decorator which wraps the underlying display name within a CDATA tag
 *
 * Provide the underlying DisplayNameFormatter as a constructor argument
 *
 * @author brad
 */
public class CdataDisplayNameFormatter implements DisplayNameFormatter{

    private final DisplayNameFormatter wrapped;

    public CdataDisplayNameFormatter( DisplayNameFormatter wrapped ) {
        this.wrapped = wrapped;
    }

    public String formatDisplayName( PropFindableResource res ) {
        return "<![CDATA[" + wrapped.formatDisplayName( res ) + "]]>";
    }

}
