package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.HrefStatus;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Response;
import com.bradmcevoy.http.http11.Http11ResponseHandler;
import com.bradmcevoy.http.quota.StorageChecker.StorageErrorReason;
import java.util.List;

/**
 *
 * @author brad
 */
public interface WebDavResponseHandler extends Http11ResponseHandler{
    void responseMultiStatus(Resource resource, Response response, Request request, List<HrefStatus> statii);

    /**
     * Generate the response for a PROPFIND or a PROPPATCH
     *
     * @param propFindResponses
     * @param response
     * @param request
     * @param r - the resource
     */
    void respondPropFind( List<PropFindResponse> propFindResponses, Response response, Request request, Resource r );

    void respondInsufficientStorage( Request request, Response response, StorageErrorReason storageErrorReason );

    void respondLocked( Request request, Response response, Resource existingResource );

    /**
     * Generate a 412 response, 
     * 
     * @param request
     * @param response
     * @param resource
     */
    void respondPreconditionFailed( Request request, Response response, Resource resource );
}
