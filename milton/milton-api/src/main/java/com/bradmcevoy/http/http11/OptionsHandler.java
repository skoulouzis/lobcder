package com.bradmcevoy.http.http11;

import com.bradmcevoy.http.*;
import java.util.ArrayList;
import java.util.List;

import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support the OPTIONS http method.
 *
 * Note that windows 7 appears to require un-authenticated access to OPTIONS
 * requests, so this handler now supports configurable selection of allowing
 * un-authenticated access.
 *
 * @author brad
 */
public class OptionsHandler implements ResourceHandler {

    private static final Logger log = LoggerFactory.getLogger( OptionsHandler.class );
    private final Http11ResponseHandler responseHandler;
    private final HandlerHelper handlerHelper;
    private final ResourceHandlerHelper resourceHandlerHelper;
    private boolean enableAuthorisation;

    /**
     * Creates an OptionHandler with no authorisation
     * 
     * @param responseHandler
     */
    public OptionsHandler( Http11ResponseHandler responseHandler ) {
        this.responseHandler = responseHandler;
        this.handlerHelper = null;
        this.resourceHandlerHelper = new ResourceHandlerHelper( handlerHelper, responseHandler );
        this.enableAuthorisation = false;
    }

    /**
     * Creates an OptionHandler with no authorisation
     *
     * Note that the handlerHelper is redundant, but this constructor is kept
     * for backwards compatibility
     *
     * @param responseHandler
     */
    public OptionsHandler( Http11ResponseHandler responseHandler, HandlerHelper handlerHelper ) {
        this.responseHandler = responseHandler;
        this.handlerHelper = handlerHelper;
        this.resourceHandlerHelper = new ResourceHandlerHelper( handlerHelper, responseHandler );
        this.enableAuthorisation = false;
    }

    /**
     * Allows the choice of enabling authorisation. Some webdav clients (such as windows 7) require 
     * un-authenticated OPTIONS requests, because they use the information returned to determine
     * how to authenticate.
     * 
     * However, this might be considered a security risk as it allows mailicious
     * users to determine the existence of resources, although not their content.
     * 
     * @param responseHandler
     * @param handlerHelper - redundant if enableAuthorisation is false
     * @param enableAuthorisation - if false OPTIONS requests will never request authentication
     */
    public OptionsHandler( Http11ResponseHandler responseHandler, HandlerHelper handlerHelper, boolean enableAuthorisation ) {
        this.responseHandler = responseHandler;
        this.handlerHelper = handlerHelper;
        this.resourceHandlerHelper = new ResourceHandlerHelper( handlerHelper, responseHandler );
        this.enableAuthorisation = enableAuthorisation;
    }

    @Override
    public void process( HttpManager manager, Request request, Response response ) throws NotAuthorizedException, ConflictException, BadRequestException {
        resourceHandlerHelper.process( manager, request, response, this );
    }

    public void processResource( HttpManager manager, Request request, Response response, Resource resource ) throws NotAuthorizedException, ConflictException, BadRequestException {
        long t = System.currentTimeMillis();
        try {
            if( enableAuthorisation) {
                if( !handlerHelper.checkAuthorisation( manager, resource, request ) ) {
                    responseHandler.respondUnauthorised( resource, response, request );
                    return;
                }
            }

            manager.onProcessResourceStart( request, response, resource );

            List<String> methodsAllowed = determineMethodsAllowed( manager, resource );
            responseHandler.respondWithOptions( resource, response, request, methodsAllowed );

        } finally {
            t = System.currentTimeMillis() - t;
            manager.onProcessResourceFinish( request, response, resource, t );
        }
    }

    public String[] getMethods() {
        return new String[]{Method.OPTIONS.code};
    }

    @Override
    public boolean isCompatible( Resource handler ) {
        return true;
    }

    private List<String> determineMethodsAllowed( HttpManager manager, Resource res ) {
        List<String> list = new ArrayList<String>();
        for( Handler f : manager.getAllHandlers() ) {
            if( f.isCompatible( res ) ) {
                for( String m : f.getMethods() ) {
                    Method httpMethod = Method.valueOf( m );
                    if( !handlerHelper.isNotCompatible( res, httpMethod) ) {
                        list.add( m );
                    }
                }
            }
        }
        return list;
    }

    public boolean isEnableAuthorisation() {
        return enableAuthorisation;
    }

    public void setEnableAuthorisation( boolean enableAuthorisation ) {
        this.enableAuthorisation = enableAuthorisation;
        if( enableAuthorisation && (handlerHelper == null)) {
            throw new RuntimeException( "enableAuthorisation set to true, but no handlerHelper has been provided. You WILL get NullPointerException's");
        }
    }
}
