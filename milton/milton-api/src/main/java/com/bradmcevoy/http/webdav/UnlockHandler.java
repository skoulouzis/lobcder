
package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.*;
import com.bradmcevoy.http.exceptions.PreConditionFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Response.Status;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

public class UnlockHandler implements ExistingEntityHandler {

    private Logger log = LoggerFactory.getLogger(UnlockHandler.class);

    private final ResourceHandlerHelper resourceHandlerHelper;

    private final WebDavResponseHandler responseHandler;

    public UnlockHandler( ResourceHandlerHelper resourceHandlerHelper, WebDavResponseHandler responseHandler ) {
        this.resourceHandlerHelper = resourceHandlerHelper;
        this.responseHandler = responseHandler;
    }


	@Override
    public void process( HttpManager httpManager, Request request, Response response ) throws ConflictException, NotAuthorizedException, BadRequestException {
        resourceHandlerHelper.process( httpManager, request, response, this );
		
        String host = request.getHostHeader();
        String url = HttpManager.decodeUrl( request.getAbsolutePath() );

        // Find a resource if it exists
        Resource r = httpManager.getResourceFactory().getResource( host, url );
        if( r != null ) {
            log.debug( "locking existing resource: " + r.getName() );
            processExistingResource( httpManager, request, response, r );
        } else {
//            log.debug( "lock target doesnt exist, attempting lock null.." );
//            processNonExistingResource( httpManager, request, response, host, url );
        }
    }

	@Override
    public void processResource( HttpManager manager, Request request, Response response, Resource r ) throws NotAuthorizedException, ConflictException, BadRequestException {
        resourceHandlerHelper.processResource( manager, request, response, r, this );
    }

	@Override
    public void processExistingResource( HttpManager manager, Request request, Response response, Resource resource ) throws NotAuthorizedException, BadRequestException, ConflictException {
        LockableResource r = (LockableResource) resource;
        String sToken = request.getLockTokenHeader();        
        sToken = LockHandler.parseToken(sToken);
        
        // this should be checked in processResource now
//       	if( r.getCurrentLock() != null &&
//       			!sToken.equals( r.getCurrentLock().tokenId))
//    	{
       		//Should this be unlocked easily? With other tokens?
//    		response.setStatus(Status.SC_LOCKED);
//    	    log.info("cant unlock with token: " + sToken);
//    		return;
//    	}

        
        log.debug("unlocking token: " + sToken);
        try {
            r.unlock( sToken );
            responseHandler.respondNoContent( resource, response, request );
        } catch( PreConditionFailedException ex ) {
            responseHandler.respondPreconditionFailed( request, response, resource );
        }
    }
    
	@Override
    public String[] getMethods() {
        return new String[]{Method.UNLOCK.code};
    }
    
    @Override
    public boolean isCompatible( Resource handler ) {
        return handler instanceof LockableResource;
    }

    
}
