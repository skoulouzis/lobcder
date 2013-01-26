package com.ettrema.http.acl;

import com.bradmcevoy.http.Handler;
import com.bradmcevoy.http.HandlerHelper;
import com.bradmcevoy.http.HttpManager;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Response;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.webdav.WebDavResponseHandler;
import com.ettrema.http.AccessControlledResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class ACLHandler implements Handler{

    private Logger log = LoggerFactory.getLogger(ACLHandler.class);

    private final WebDavResponseHandler responseHandler;
    private final HandlerHelper handlerHelper;

    public ACLHandler( WebDavResponseHandler responseHandler, HandlerHelper handlerHelper ) {
        this.responseHandler = responseHandler;
        this.handlerHelper = handlerHelper;
    }



    public String[] getMethods() {
        return new String[]{Method.ACL.code};
    }

    public void process( HttpManager httpManager, Request request, Response response ) throws ConflictException, NotAuthorizedException, BadRequestException {
        response.setStatus( Response.Status.SC_OK );
    }

    public boolean isCompatible( Resource res ) {
        return (res instanceof AccessControlledResource);
    }

}
