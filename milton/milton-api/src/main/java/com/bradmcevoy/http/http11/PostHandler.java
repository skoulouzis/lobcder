package com.bradmcevoy.http.http11;

import com.bradmcevoy.http.*;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

import com.bradmcevoy.http.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostHandler implements ExistingEntityHandler {

    private Logger log = LoggerFactory.getLogger( PostHandler.class );
    private final Http11ResponseHandler responseHandler;
    private final HandlerHelper handlerHelper;
    private final ResourceHandlerHelper resourceHandlerHelper;
    

    public PostHandler( Http11ResponseHandler responseHandler, HandlerHelper handlerHelper ) {
        this.responseHandler = responseHandler;
        this.handlerHelper = handlerHelper;
        this.resourceHandlerHelper = new ResourceHandlerHelper( handlerHelper, responseHandler );
    }

    @Override
    public String[] getMethods() {
        return new String[]{Request.Method.POST.code};
    }

    @Override
    public boolean isCompatible( Resource handler ) {
        return ( handler instanceof PostableResource );
    }

    @Override
    public void process( HttpManager manager, Request request, Response response ) throws NotAuthorizedException, ConflictException, BadRequestException {
        this.resourceHandlerHelper.process( manager, request, response, this );
    }

    @Override
    public void processResource( HttpManager manager, Request request, Response response, Resource r ) throws NotAuthorizedException, ConflictException, BadRequestException {
        manager.onPost( request, response, r, request.getParams(), request.getFiles() );
        resourceHandlerHelper.processResource( manager, request, response, r, this, true, request.getParams(), request.getFiles() );
    }

    @Override
    public void processExistingResource( HttpManager manager, Request request, Response response, Resource resource ) throws NotAuthorizedException, BadRequestException, ConflictException, NotFoundException {
        PostableResource r = (PostableResource) resource;
        for(CustomPostHandler h : manager.getCustomPostHandlers()) {
            if(h.supports(resource, request)) {
                log.trace("Found CustomPostHandler supporting this resource and request");
                h.process(resource, request, response);
                return ;
            }
        }
        String url = r.processForm( request.getParams(), request.getFiles() );
        if( url != null ) {
            log.debug("redirect: " + url );
            responseHandler.respondRedirect( response, request, url );
        } else {
            log.debug("respond with content");
            responseHandler.respondContent( resource, response, request, request.getParams() );
        }
    }    
}
