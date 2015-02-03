package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.PropFindableResource;

/**
 * An implementation of DisplayNameFormatter which just uses the resource
 * getName() as the display name.
 *
 * May be used in conjunction with CdataDisplayNameFormatter to support extended
 * character sets.
 *
 * @author brad
 */
public class DefaultDisplayNameFormatter implements DisplayNameFormatter {

    public String formatDisplayName( PropFindableResource res ) {
        return res.getName();
    }

}
