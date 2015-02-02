package com.ettrema.http.fck;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.DigestResource;
import com.bradmcevoy.http.PostableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.http11.auth.DigestResponse;
import java.util.Date;

public abstract class FckCommon implements PostableResource, DigestResource {

    protected Path url;
    protected final CollectionResource wrappedResource;

    FckCommon( CollectionResource wrappedResource, Path url ) {
        this.wrappedResource = wrappedResource;
        this.url = url;
    }

    @Override
    public Long getMaxAgeSeconds( Auth auth ) {
        return null;
    }

    @Override
    public String getName() {
        return url.getName();
    }

    @Override
    public Object authenticate( String user, String password ) {
        return wrappedResource.authenticate( user, password );
    }

    @Override
    public Object authenticate( DigestResponse dr ) {
        if( wrappedResource instanceof DigestResource) {
            return ((DigestResource)wrappedResource).authenticate( dr );
        } else {
            return null;
        }
    }

    public boolean isDigestAllowed() {
        return wrappedResource instanceof DigestResource;
    }



    @Override
    public boolean authorise( Request request, Request.Method method, Auth auth ) {
        return auth != null;
    }

    @Override
    public String getRealm() {
        return wrappedResource.getRealm();
    }

    @Override
    public Date getModifiedDate() {
        return null;
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    @Override
    public String checkRedirect( Request request ) {
        return null;
    }
}
