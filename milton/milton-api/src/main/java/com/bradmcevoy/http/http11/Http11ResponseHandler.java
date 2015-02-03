package com.bradmcevoy.http.http11;

import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Response;
import com.bradmcevoy.http.Response.Status;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.exceptions.NotFoundException;
import java.util.List;
import java.util.Map;

/**
 *  The ResponseHandler should handle all responses back to the client.
 *
 *  Methods are provided for each significant response circumstance with respect
 *  to Milton.
 *
 *  The intention is that implementations may be provided or customised to support
 *  per implementation requirements for client compatibility.
 *
 *  In other words, hacks to support particular client programs should be implemented
 *  here
 *
 *  Extends ETagGenerator to facillitate wrapping, although generatlly it will
 *  contain an instance and delegate to it.
 */
public interface Http11ResponseHandler extends ETagGenerator {
    /**
     * Invoked when an operation is successful, but there is no content, and
     * there is nothing more specific to return (E.g. created)
     *
     * For example, as a result of a PUT when a resouce has been updated)
     *
     * @param resource
     * @param response
     * @param request
     */
    void respondNoContent(Resource resource, Response response,Request request);
    void respondContent(Resource resource, Response response, Request request, Map<String,String> params) throws NotAuthorizedException, BadRequestException, NotFoundException;
    void respondPartialContent(GetableResource resource, Response response, Request request, Map<String,String> params, Range range) throws NotAuthorizedException, BadRequestException, NotFoundException;
    void respondCreated(Resource resource, Response response, Request request);
    void respondUnauthorised(Resource resource, Response response, Request request);
    void respondMethodNotImplemented(Resource resource, Response response, Request request);
    void respondMethodNotAllowed(Resource res, Response response, Request request);
    void respondConflict(Resource resource, Response response, Request request, String message);
    void respondRedirect(Response response, Request request, String redirectUrl);
    void respondNotModified(GetableResource resource, Response response, Request request);
    void respondNotFound(Response response, Request request);
    void respondWithOptions(Resource resource, Response response,Request request, List<String> methodsAllowed);

    /**
     * Generate a HEAD response
     *
     * @param resource
     * @param response
     * @param request
     */
    void respondHead( Resource resource, Response response, Request request );

    /**
     * Response with a 417
     */
    void respondExpectationFailed(Response response, Request request);

    /**
     * Respond with a 400 status
     *
     * @param resource
     * @param response
     * @param request
     * @param params
     */
    void respondBadRequest( Resource resource, Response response, Request request);


    /**
     * Respond with a 403 status - forbidden
     *
     * @param resource
     * @param response
     * @param request
     * @param params
     */
    void respondForbidden( Resource resource, Response response, Request request);


    /**
     * Called when a delete has failed, including the failure status.
     *
     * Note that webdav implementations will respond with a multisttus, while
     * http 1.1 implementations will simply set the response status
     *
     * @param request
     * @param response
     * @param resource - the resource which could not be deleted
     * @param status - the status which has caused the delete to fail.
     */
    void respondDeleteFailed( Request request, Response response, Resource resource, Status status );

    /**
     * Usually a 500 error. Some error occured processing the request. Note
     * that you might not be able to assume that this will generate all 500
     * errors since a runtime exception might result in code outside of milton's
     * control generating the 500 response.
     * 
     * @param request
     * @param response
     * @param reason
     */
    void respondServerError( Request request, Response response, String reason);


    
}
