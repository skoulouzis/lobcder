package com.bradmcevoy.http.http11;

import com.bradmcevoy.http.*;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.exceptions.NotFoundException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetHandler implements ExistingEntityHandler {

    private static final Logger log = LoggerFactory.getLogger( GetHandler.class );
    private final Http11ResponseHandler responseHandler;
    private final HandlerHelper handlerHelper;
    private final ResourceHandlerHelper resourceHandlerHelper;
	private final PartialGetHelper partialGetHelper;

    public GetHandler( Http11ResponseHandler responseHandler, HandlerHelper handlerHelper ) {
        this.responseHandler = responseHandler;
        this.handlerHelper = handlerHelper;
        this.resourceHandlerHelper = new ResourceHandlerHelper( handlerHelper, responseHandler );
		partialGetHelper = new PartialGetHelper(responseHandler);
    }

    @Override
    public void process( HttpManager manager, Request request, Response response ) throws NotAuthorizedException, ConflictException, BadRequestException {
        log.debug( "process" );
        this.resourceHandlerHelper.process( manager, request, response, this );
    }

    @Override
    public void processResource( HttpManager manager, Request request, Response response, Resource r ) throws NotAuthorizedException, ConflictException, BadRequestException {
        manager.onGet( request, response, r, request.getParams() );
        resourceHandlerHelper.processResource( manager, request, response, r, this, true, request.getParams(), null );
    }

	@Override
    public void processExistingResource( HttpManager manager, Request request, Response response, Resource resource ) throws NotAuthorizedException, BadRequestException, ConflictException, NotFoundException {
        if( log.isTraceEnabled() ) {
            log.trace( "process: " + request.getAbsolutePath() );
        }
        GetableResource r = (GetableResource) resource;
        if( checkConditional( r, request ) ) {
            if( log.isTraceEnabled() ) {
                log.trace( "respond not modified with: " + responseHandler.getClass().getCanonicalName() );
            }
            responseHandler.respondNotModified( r, response, request );
            return;
        }

        sendContent( manager, request, response, r, request.getParams() );
    }


    /** Return true if the resource has not been modified
     */
    private boolean checkConditional( GetableResource resource, Request request ) {
        // If maxAgeSeconds is null then we do not cache
        if( resource.getMaxAgeSeconds( request.getAuthorization() ) == null ) {
            log.trace( "resource has null max age, so not modified response is disabled" );
            return false;
        }
        if( checkIfMatch( resource, request ) ) {
            return true;
        }
        if( checkIfModifiedSince( resource, request ) ) {
            log.trace( "is not modified since" );
            return true;
        }
        if( checkIfNoneMatch( resource, request ) ) {
            return true;
        }
        return false;
    }

    private boolean checkIfMatch( GetableResource handler, Request requestInfo ) {
        return false;   // TODO: not implemented
    }

    /**
     *
     * @param resource
     * @param requestInfo
     * @return - true if the resource has NOT been modified since that date in the request
     */
    private boolean checkIfModifiedSince( GetableResource resource, Request requestInfo ) {
        log.trace( "checkIfModifiedSince" );
        Long maxAgeSecs = resource.getMaxAgeSeconds( requestInfo.getAuthorization() );

		// Null maxAge indicates that the resource implementor does not want
		// this resource to be cached
        if( maxAgeSecs == null ) {
            log.trace( "checkIfModifiedSince: null max age" );
            return false; // if null, always generate a fresh response
        } else {
            log.trace( "checkIfModifiedSince with maxAge" );
            Date dtRequest = requestInfo.getIfModifiedHeader();
            if( dtRequest == null ) {
                log.trace( " no modified date header" );
                return false;
            }
            long timeNowMs = System.currentTimeMillis();
            long timeRequestMs = dtRequest.getTime() + 1000; // allow for rounding to nearest second
            long timeElapsedMs = timeNowMs - timeRequestMs;
			long timeElapsed = timeElapsedMs / 1000;
			// If the max-age period has elapsed then we don't bother to check if
			// it has actually been modified. This is useful for dyamically generated
			// resources (ie JSP's) which we want cached for a fixed period, but the modified
			// date doesnt reflect that the content will change
            if( timeElapsed > maxAgeSecs ) {
                log.trace( "its been longer then the max age period, so generate fresh response" );
                return false;
            } else {
				// If max-age hasnt elapsed we check to see if the resource has
				// actually been modified since the date in the request header
                Date dtResourceModified = resource.getModifiedDate();
                if( dtResourceModified == null ) {
                    if( log.isTraceEnabled() ) {
                        log.trace( "no modified date on resource: " + resource.getClass().getCanonicalName() );
                    }
                    return true;
                }

                long resModifiedMs = dtResourceModified.getTime();
                boolean unchangedSince = ( timeRequestMs >= resModifiedMs );
                if( log.isTraceEnabled() ) {
                    log.trace( "times as long: resource modified " + dtResourceModified.getTime() + " - modified since header: " + dtRequest.getTime() );
                    log.trace( "checkModifiedSince: actual: " + dtResourceModified + " - request:" + dtRequest + " = " + unchangedSince + " (true indicates no change)" );
                }

                // If the modified time requested is greater or equal then the actual modified time, do not generate response
                return unchangedSince;
            }
        }
    }

    private boolean checkIfNoneMatch( GetableResource handler, Request requestInfo ) {
        return false;   // TODO: not implemented
    }

    @Override
    public String[] getMethods() {
        return new String[]{Request.Method.GET.code, Request.Method.HEAD.code};
    }

    @Override
    public boolean isCompatible( Resource handler ) {
        return ( handler instanceof GetableResource );
    }

    private void sendContent( HttpManager manager, Request request, Response response, GetableResource resource, Map<String, String> params ) throws NotAuthorizedException, BadRequestException, NotFoundException {
        try {
            if( request.getMethod().equals( Method.HEAD ) ) {
                responseHandler.respondHead( resource, response, request );
            } else {
                List<Range> ranges = partialGetHelper.getRanges( request.getRangeHeader() );
                if( ranges != null && ranges.size() > 0 ) {
					partialGetHelper.sendPartialContent(resource, request, response, ranges, params);
                } else {
                    if( log.isTraceEnabled() ) {
                        log.trace( "normal content: " + responseHandler.getClass().getCanonicalName() );
                    }
                    responseHandler.respondContent( resource, response, request, params );
                }
            }
		} catch(NotFoundException e) {
			throw e;
        } catch( NotAuthorizedException notAuthorizedException ) {
            throw notAuthorizedException;
        } catch( BadRequestException badRequestException ) {
            throw badRequestException;
        } catch( Throwable e ) {
            log.error( "Exception sending content for:" + request.getAbsolutePath() + " of resource type: " + resource.getClass().getCanonicalName() );
            throw new RuntimeException( e );
        }
    }
}
