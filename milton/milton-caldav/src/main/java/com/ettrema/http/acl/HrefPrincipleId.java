package com.ettrema.http.acl;

import com.bradmcevoy.http.webdav.WebDavProtocol;
import javax.xml.namespace.QName;

/**
 *
 * @author brad
 */
public class HrefPrincipleId implements Principal.PrincipleId {

    private final QName type;
    private final String url;

    public HrefPrincipleId(String url) {
        this.url = url;
        this.type = new QName(WebDavProtocol.DAV_URI, "href");
    }

    @Override
    public QName getIdType() {
        return type;
    }

    @Override
    public String getValue() {
        return url;
    }

    @Override
    public String toString() {
        return url;
    }
}
