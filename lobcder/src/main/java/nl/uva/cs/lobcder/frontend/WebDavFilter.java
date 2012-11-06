/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.frontend;

import com.bradmcevoy.http.*;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.exceptions.NotFoundException;
import com.bradmcevoy.http.http11.Http11ResponseHandler;
import nl.uva.cs.lobcder.webDav.exceptions.ForbiddenException;
import nl.uva.cs.lobcder.webDav.exceptions.UnauthorizedException;
import nl.uva.cs.lobcder.webDav.exceptions.WebDavException;
import nl.uva.cs.lobcder.webDav.resources.WebDataResource;

/**
 *
 * @author alogo
 */
class WebDavFilter implements Filter {
   
    @Override
    public void process(FilterChain chain, Request request, Response response) {
        HttpManager manager = chain.getHttpManager();
        Http11ResponseHandler responseHandler = manager.getResponseHandler();
        try {
            Request.Method method = request.getMethod();
            Handler handler = manager.getMethodHandler(method);
            if (handler == null) {
                responseHandler.respondMethodNotImplemented(new WebDataResource(null, null), response, request);
                return;
            }
            
            try {
                try {
                    handler.process(manager, request, response);
                } catch (NotFoundException ex) {
                    throw new RuntimeException(ex);
                }
            } catch (RuntimeException e) {
                /*
                 * Milton wraps our WebDavException in a RuntimeException.
                 */
                if (e.getCause() instanceof WebDavException) {
                    throw (WebDavException) e.getCause();
                }
                /*
                 * Milton also wraps critical errors that should not be caught.
                 */
                if (e.getCause() instanceof Error) {
                    throw (Error) e.getCause();
                }
                throw e;
            }
        } catch (BadRequestException e) {
            responseHandler.respondBadRequest(e.getResource(), response, request);
        } catch (ConflictException e) {
            responseHandler.respondConflict(e.getResource(), response, request, e.getMessage());
        } catch (NotAuthorizedException e) {
            responseHandler.respondUnauthorised(e.getResource(), response, request);
        } catch (UnauthorizedException e) {
            responseHandler.respondUnauthorised(e.getResource(), response, request);
        } catch (ForbiddenException e) {
            responseHandler.respondForbidden(e.getResource(), response, request);
        } catch (WebDavException e) {
            responseHandler.respondServerError(request, response, e.getMessage());
        } catch (RuntimeException e) {
            responseHandler.respondServerError(request, response, e.getMessage());
        } finally {
            response.close();
        }
    }
}
