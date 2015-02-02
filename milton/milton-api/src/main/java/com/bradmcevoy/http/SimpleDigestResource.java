package com.bradmcevoy.http;

import com.bradmcevoy.http.http11.auth.DigestResponse;
import java.util.Date;

/**
 *
 * @author brad
 */
public class SimpleDigestResource extends SimpleResource implements DigestResource{

    private final DigestResource digestResource;

    public SimpleDigestResource( String name, Date modDate, byte[] content, String contentType, String uniqueId, String realm) {
        super(name, modDate, content, contentType, uniqueId, realm );
        this.digestResource = null;
    }

    public SimpleDigestResource( String name, Date modDate, byte[] content, String contentType, String uniqueId, DigestResource secureResource ) {
        super(name, modDate, content, contentType, uniqueId, secureResource );
        this.digestResource = secureResource;
    }

    public Object authenticate( DigestResponse digestRequest ) {
        return digestResource.authenticate( digestRequest );
    }

    public boolean isDigestAllowed() {
        return true;
    }


}
